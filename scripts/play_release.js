#!/usr/bin/env node
// Inspect, prepare, validate, and commit a release without logging credentials.
const fs = require('node:fs');
const crypto = require('node:crypto');
const path = require('node:path');
const key = JSON.parse(fs.readFileSync(process.env.GOOGLE_PLAY_JSON_KEY_FILE || 'fastlane/google-play-key.json', 'utf8'));
const root = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.subtranslate/edits';
const action = process.argv[2] || 'inspect';
const statePath = process.argv[3] || 'artifacts/release-20261019/play-edit.json';

async function main() {
  const now = Math.floor(Date.now() / 1000);
  const encode = obj => Buffer.from(JSON.stringify(obj)).toString('base64url');
  const data = `${encode({alg:'RS256',typ:'JWT'})}.${encode({iss:key.client_email,scope:'https://www.googleapis.com/auth/androidpublisher',aud:key.token_uri,iat:now,exp:now+3600})}`;
  const assertion = `${data}.${crypto.sign('RSA-SHA256', Buffer.from(data), key.private_key).toString('base64url')}`;
  const auth = await (await fetch(key.token_uri, {method:'POST',body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion}),signal:AbortSignal.timeout(60000)})).json();
  if (!auth.access_token) throw Error('Play authentication failed');
  const headers = {authorization:`Bearer ${auth.access_token}`};
  async function request(url, options={}) {
    if (action === 'prepare') console.error(`${options.method || 'GET'} ${url.includes('/upload/') ? 'upload bundle' : url.split('/').pop()}`);
    const response = await fetch(url,{...options,headers:{...headers,...options.headers},signal:AbortSignal.timeout(600000)});
    const body = await response.text();
    if (!response.ok) throw Error(`${response.status}: ${body}`);
    return body ? JSON.parse(body) : {};
  }
  const json = body => ({headers:{'content-type':'application/json'},body:JSON.stringify(body)});
  if (action === 'data-safety') {
    const safetyLabels = fs.readFileSync(statePath,'utf8');
    await request('https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.subtranslate/dataSafety',{method:'POST',...json({safetyLabels})});
    console.log(JSON.stringify({dataSafetySubmitted:true,file:statePath}));
    return;
  }
  if (action === 'commit') {
    const state = JSON.parse(fs.readFileSync(statePath));
    await request(`${root}/${state.editId}:validate`,{method:'POST'});
    const result = await request(`${root}/${state.editId}:commit`,{method:'POST'});
    state.committed = true;
    state.commitResult = result;
    fs.writeFileSync(statePath,JSON.stringify(state,null,2));
    console.log(JSON.stringify({committed:true,versionCode:state.versionCode,track:state.track,result}));
    return;
  }
  const edit = await request(root,{method:'POST'});
  const base = `${root}/${edit.id}`;
  let keep = false;
  try {
    if (action === 'inspect') {
      const tracks = await request(`${base}/tracks`);
      const listings = await request(`${base}/listings`);
      const details = await request(`${base}/details`);
      const bundles = await request(`${base}/bundles`);
      const images = {};
      for (const language of ['en-US','iw-IL']) {
        images[language] = {};
        for (const type of ['icon','featureGraphic','phoneScreenshots']) images[language][type] = await request(`${base}/listings/${language}/${type}`);
      }
      console.log(JSON.stringify({tracks,listings,details,bundles,images},null,2));
      return;
    }
    if (action !== 'prepare') throw Error(`Unknown action ${action}`);
    for (const language of ['en-US','he-IL']) {
      const dir = `fastlane/metadata/android/${language}/images`;
      const screenshots = fs.readdirSync(`${dir}/phoneScreenshots`).filter(name=>name.endsWith('.png'));
      if (screenshots.length < 2 || screenshots.length > 8) throw Error(`Expected 2–8 screenshots for ${language}`);
      for (const name of ['icon.png','featureGraphic.png']) if (!fs.statSync(`${dir}/${name}`).size) throw Error(`Empty ${name}`);
    }
    const build = fs.readFileSync('app/build.gradle.kts','utf8');
    const versionCode = Number(build.match(/versionCode = (\d+)/)[1]);
    const versionName = build.match(/versionName = "([^"]+)"/)[1];
    const track = process.env.SUBTY_RELEASE_TRACK || 'internal';
    if (!['internal','production'].includes(track)) throw Error('Unsupported track');
    const aab = 'app/build/outputs/bundle/stableRelease/app-stable-release.aab';
    const uploadBase = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/com.subtranslate/edits/${edit.id}`;
    const bundle = await request(`${uploadBase}/bundles?uploadType=media`,{method:'POST',headers:{'content-type':'application/octet-stream'},body:fs.readFileSync(aab)});
    if (bundle.versionCode !== versionCode) throw Error('Bundle version mismatch');
    const releaseNotes = [];
    for (const language of ['en-US','he-IL']) {
      const playLanguage = language === 'he-IL' ? 'iw-IL' : language;
      const dir = `fastlane/metadata/android/${language}`;
      const read = name => fs.readFileSync(path.join(dir,name),'utf8').trim();
      if (process.env.SUBTY_BINARY_ONLY !== '1') await request(`${base}/listings/${playLanguage}`,{method:'PUT',...json({language:playLanguage,title:read('title.txt'),shortDescription:read('short_description.txt'),fullDescription:read('full_description.txt')})});
      releaseNotes.push({language:playLanguage,text:read(`changelogs/${versionCode}.txt`)});
      if (process.env.SUBTY_BINARY_ONLY !== '1') for (const [type, files] of [
        ['icon',[path.join(dir,'images/icon.png')]],
        ['featureGraphic',[path.join(dir,'images/featureGraphic.png')]],
        ['phoneScreenshots',fs.readdirSync(path.join(dir,'images/phoneScreenshots')).filter(n=>n.endsWith('.png')).sort().map(n=>path.join(dir,'images/phoneScreenshots',n))],
      ]) {
        await request(`${base}/listings/${playLanguage}/${type}`,{method:'DELETE'});
        for (const file of files) await request(`${uploadBase}/listings/${playLanguage}/${type}?uploadType=media`,{method:'POST',headers:{'content-type':'image/png'},body:fs.readFileSync(file)});
      }
    }
    await request(`${base}/tracks/${track}`,{method:'PUT',...json({track,releases:[{name:versionName,versionCodes:[String(versionCode)],status:'completed',releaseNotes}]})});
    await request(`${base}:validate`,{method:'POST'});
    const state = {editId:edit.id,versionCode,versionName,track,bundle,validated:true,committed:false,preparedAt:new Date().toISOString()};
    fs.mkdirSync(path.dirname(statePath),{recursive:true});
    fs.writeFileSync(statePath,JSON.stringify(state,null,2));
    keep = true;
    console.log(JSON.stringify(state,null,2));
  } finally {
    if (!keep) await request(base,{method:'DELETE'});
  }
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
