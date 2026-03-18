const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = 7890;
const OS_HOST = 'api.opensubtitles.com';
const OS_API_KEY = 's7Y2RwG2TR4xPTmSShqVSM4gRYOTzToN';
const SUBDL_API_KEY = 'flS-uvm6eADL2bkt0YBONxB5glM2X4Xr';

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
  // OpenSubtitles API proxy
  if (req.url.startsWith('/osapi/')) {
    const apiPath = '/api/v1/' + req.url.slice(7);
    proxyHttps(OS_HOST, apiPath, req.method, req, res, { 'Api-Key': OS_API_KEY });
    return;
  }

  // SubDL API proxy
  if (req.url.startsWith('/sdapi/')) {
    const apiPath = '/api/v1/' + req.url.slice(7);
    proxyHttps('api.subdl.com', apiPath, req.method, req, res, {});
    return;
  }

  // SubDL download proxy (ZIP files)
  if (req.url.startsWith('/sddl/')) {
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
    res.writeHead(200, { 'Content-Type': types[ext] || 'text/plain' });
    res.end(data);
  });
});

server.listen(PORT, () => console.log(`Preview proxy on http://localhost:${PORT}`));
