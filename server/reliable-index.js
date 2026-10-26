const express = require('express');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createApp } = require('./app');

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const root = path.join(__dirname, '..');
const modern = path.join(root, 'modern');
const modules = path.join(root, 'node_modules', '@fontsource');
const databasePath = process.env.DATABASE_PATH ? path.resolve(process.env.DATABASE_PATH) : path.join(root, 'data', 'machly.db');
const api = createApp({ databasePath });
const app = express();

const html = fs.readFileSync(path.join(modern, 'index.html'), 'utf8')
  .replace(/\s*<link[^>]+fonts\.googleapis\.com[^>]*>/gi, '')
  .replace(/\s*<link[^>]+fonts\.gstatic\.com[^>]*>/gi, '');
const css = [
  fs.readFileSync(path.join(modern, 'local-fonts.css'), 'utf8'),
  fs.readFileSync(path.join(modern, 'styles.css'), 'utf8'),
  fs.readFileSync(path.join(modern, 'polish.css'), 'utf8')
].join('\n');

app.get('/api/ui-version', (req, res) => res.json({ ui: 'machly-agro', version: '3.0.0', assets: 'local' }));
app.use((req, res, next) => req.path.startsWith('/api/') ? api(req, res, next) : next());
app.get(['/','/index.html'], (req, res) => res.type('html').send(html));
app.get('/styles.css', (req, res) => res.type('css').set('Cache-Control','no-cache').send(css));
app.get('/app.js', (req, res) => res.sendFile(path.join(root, 'public', 'app.js')));
app.use('/fonts/dm-sans', express.static(path.join(modules, 'dm-sans', 'files'), { maxAge: '30d', immutable: true }));
app.use('/fonts/manrope', express.static(path.join(modules, 'manrope', 'files'), { maxAge: '30d', immutable: true }));
app.use('/assets', express.static(path.join(root, 'public', 'assets'), { maxAge: '1d' }));
app.use(express.static(modern, { index: false, extensions: ['html'], maxAge: '1h' }));
app.use((req, res) => req.method === 'GET' ? res.type('html').send(html) : res.status(404).json({ error: 'Ruta no encontrada.' }));

const server = app.listen(port, host, () => {
  const addresses = Object.values(os.networkInterfaces()).flat().filter(info => info && info.family === 'IPv4' && !info.internal);
  console.log('\nMachly está listo. Todos los recursos visuales son locales.');
  console.log(`  Abrir aquí: http://localhost:${port}`);
  for (const info of addresses) console.log(`  Teléfono: http://${info.address}:${port}`);
  console.log(`  Base de datos: ${databasePath}\n`);
});

function shutdown(){server.close(()=>{api.locals.db.close();process.exit(0);});}
process.on('SIGINT',shutdown);
process.on('SIGTERM',shutdown);
