const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = 7890;
const OS_HOST = 'api.opensubtitles.com';
const OS_API_KEY = 's7Y2RwG2TR4xPTmSShqVSM4gRYOTzToN';
const SUBDL_API_KEY = 'flS-uvm6eADL2bkt0YBONxB5glM2X4Xr';
const LIVE_RELOAD_ENABLED = process.env.LIVE_RELOAD !== '0';

const liveClients = new Set();
let liveWatchStarted = false;

function sendLiveEvent(type, payload) {
  const msg = `event: ${type}\ndata: ${JSON.stringify(payload)}\n\n`;
  for (const res of liveClients) {
    try {
      res.write(msg);
    } catch {
      liveClients.delete(res);
    }
  }
}

function startLiveWatchOnce() {
  if (liveWatchStarted || !LIVE_RELOAD_ENABLED) return;
  liveWatchStarted = true;
  fs.watch(__dirname, { recursive: false }, (eventType, filename) => {
    if (!filename) return;
    if (!/\.(html|css|js)$/i.test(filename)) return;
    sendLiveEvent('reload', { eventType, filename, at: Date.now() });
  });
}

function injectLiveReload(htmlBuffer) {
  if (!LIVE_RELOAD_ENABLED) return htmlBuffer;
  const snippet = `
<script>
(() => {
  const src = '/__live';
  let es;
  function connect() {
    es = new EventSource(src);
    es.addEventListener('reload', () => {
      window.location.reload();
    });
    es.onerror = () => {
      try { es.close(); } catch (_) {}
      setTimeout(connect, 800);
    };
  }
  connect();
})();
</script>`;
  const html = String(htmlBuffer);
  if (html.includes('</body>')) return html.replace('</body>', `${snippet}\n</body>`);
  return html + snippet;
}

function fetchText(urlString, timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const req = https.request(
      {
        hostname: url.hostname,
        path: url.pathname + url.search,
        method: 'GET',
        headers: {
          'User-Agent': 'Mozilla/5.0 (Subty WebHunt)',
          Accept: 'text/html,application/xhtml+xml',
        },
      },
      (res) => {
        let body = '';
        res.setEncoding('utf8');
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => resolve({ status: res.statusCode || 0, body }));
      }
    );

    req.setTimeout(timeoutMs, () => {
      req.destroy(new Error('Timeout'));
    });
    req.on('error', reject);
    req.end();
  });
}

function stripTags(html) {
  return String(html || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
}

function decodeEntities(input) {
  return String(input || '')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function toAbs(base, href) {
  try {
    return new URL(href, base).toString();
  } catch {
    return '';
  }
}

function extractAnchors(html, base, isWanted, limit = 8) {
  const out = [];
  const seen = new Set();
  const re = /<a\b[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/gi;
  let m;

  while ((m = re.exec(html)) && out.length < limit) {
    const href = decodeEntities(m[1] || '').trim();
    const title = decodeEntities(stripTags(m[2] || ''));
    if (!href || !title) continue;
    if (!isWanted(href, title)) continue;
    const abs = toAbs(base, href);
    if (!abs || seen.has(abs)) continue;
    seen.add(abs);
    out.push({ title, url: abs });
  }
  return out;
}

async function scrapeProvider(name, searchUrl, matcher, limit = 6) {
  try {
    const { status, body } = await fetchText(searchUrl);
    if (status < 200 || status >= 300) return [];
    return extractAnchors(body, searchUrl, matcher, limit).map((x) => ({
      provider: name,
      title: x.title,
      url: x.url,
    }));
  } catch {
    return [];
  }
}

async function runWebHunt(query) {
  const q = encodeURIComponent(query.trim());
  const providers = [
    {
      name: 'YIFY',
      url: `https://yifysubtitles.org/search?q=${q}`,
      matcher: (href, title) =>
        (/\/movie-imdb\//.test(href) || /\/subtitle\//.test(href)) && title.length > 3,
    },
    {
      name: 'Addic7ed',
      url: `https://www.addic7ed.com/search.php?search=${q}&Submit=Search`,
      matcher: (href, title) =>
        (/\/show\//.test(href) || /\/serie\//.test(href) || /\/movie\//.test(href)) && title.length > 3,
    },
    {
      name: 'TVSubtitles',
      url: `http://www.tvsubtitles.net/search.php?q=${q}`,
      matcher: (href, title) => (/tvshow-|episode-/.test(href) || /search\.php/.test(href)) && title.length > 3,
    },
    {
      name: 'Podnapisi',
      url: `https://www.podnapisi.net/subtitles/search?keywords=${q}`,
      matcher: (href, title) => /\/subtitles\//.test(href) && title.length > 3,
    },
  ];

  const all = (await Promise.all(
    providers.map((p) => scrapeProvider(p.name, p.url, p.matcher))
  )).flat();

  const dedup = [];
  const seen = new Set();
  for (const item of all) {
    if (seen.has(item.url)) continue;
    seen.add(item.url);
    dedup.push(item);
  }
  return dedup.slice(0, 24);
}

function proxyHttps(hostname, apiPath, method, req, res, extraHeaders) {
  const opts = {
    hostname,
    path: apiPath,
    method,
    headers: {
      'Content-Type': 'application/json',
      'User-Agent': 'Subty v1.0',
      ...extraHeaders,
    },
  };
  const proxy = https.request(opts, (upstream) => {
    res.writeHead(upstream.statusCode, {
      'Content-Type': upstream.headers['content-type'] || 'application/json',
      'Access-Control-Allow-Origin': '*',
    });
    upstream.pipe(res);
  });
  proxy.on('error', (e) => {
    res.writeHead(502, { 'Content-Type': 'text/plain' });
    res.end('Proxy error: ' + e.message);
  });
  if (method === 'POST') req.pipe(proxy);
  else proxy.end();
}

const server = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, 'http://localhost');
  const pathname = reqUrl.pathname;

  if (pathname === '/__health') {
    res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
    res.end(JSON.stringify({ ok: true, port: PORT, liveReload: LIVE_RELOAD_ENABLED }));
    return;
  }

  if (pathname === '/__live') {
    if (!LIVE_RELOAD_ENABLED) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Live reload disabled');
      return;
    }
    startLiveWatchOnce();
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
      'Access-Control-Allow-Origin': '*',
    });
    res.write(': connected\n\n');
    liveClients.add(res);
    req.on('close', () => {
      liveClients.delete(res);
    });
    return;
  }

  // Hidden web-hunt scraping route (no official API providers)
  if (pathname === '/webhunt/search') {
    const query = (reqUrl.searchParams.get('query') || '').trim();
    if (!query) {
      res.writeHead(400, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
      res.end(JSON.stringify({ error: 'Missing query' }));
      return;
    }
    runWebHunt(query)
      .then((results) => {
        res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ query, count: results.length, results }));
      })
      .catch((e) => {
        res.writeHead(500, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ error: e.message }));
      });
    return;
  }

  // OpenSubtitles API proxy
  if (pathname.startsWith('/osapi/')) {
    const apiPath = '/api/v1/' + req.url.slice(7);
    proxyHttps(OS_HOST, apiPath, req.method, req, res, { 'Api-Key': OS_API_KEY });
    return;
  }

  // SubDL API proxy
  if (pathname.startsWith('/sdapi/')) {
    const apiPath = '/api/v1/' + req.url.slice(7);
    proxyHttps('api.subdl.com', apiPath, req.method, req, res, {});
    return;
  }

  // SubDL download proxy (ZIP files)
  if (pathname.startsWith('/sddl/')) {
    const dlPath = req.url.slice(5); // keep leading /
    proxyHttps('dl.subdl.com', dlPath, 'GET', req, res, {});
    return;
  }

  // Serve static files
  let filePath = req.url.split('?')[0];
  filePath = filePath === '/' ? '/index.html' : filePath;
  filePath = path.join(__dirname, filePath);
  const ext = path.extname(filePath);
  const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css' };
  fs.readFile(filePath, (err, data) => {
    if (err) { res.writeHead(404); res.end('Not found'); return; }
    if (ext === '.html') {
      const body = injectLiveReload(data);
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(body);
      return;
    }
    res.writeHead(200, { 'Content-Type': types[ext] || 'text/plain' });
    res.end(data);
  });
});

server.listen(PORT, () => console.log(`Preview proxy on http://localhost:${PORT}`));
