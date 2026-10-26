const express = require('express');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createApp } = require('./app');

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const root = path.join(__dirname, '..');
const modern = path.join(root, 'modern');
const databasePath = process.env.DATABASE_PATH ? path.resolve(process.env.DATABASE_PATH) : path.join(root, 'data', 'machly.db');
const api = createApp({ databasePath });
const app = express();

app.use((req, res, next) => req.path.startsWith('/api/') ? api(req, res, next) : next());
app.get('/styles.css', (req, res) => {
  res.type('text/css').send(`${fs.readFileSync(path.join(modern, 'styles.css'), 'utf8')}\n${fs.readFileSync(path.join(modern, 'polish.css'), 'utf8')}`);
});
app.get('/app.js', (req, res) => res.sendFile(path.join(root, 'public', 'app.js')));
app.use('/assets', express.static(path.join(root, 'public', 'assets'), { maxAge: '1d' }));
app.use(express.static(modern, { extensions: ['html'], maxAge: '1h' }));
app.use((req, res) => req.method === 'GET' ? res.sendFile(path.join(modern, 'index.html')) : res.status(404).json({ error: 'Ruta no encontrada.' }));

const server = app.listen(port, host, () => {
  const addresses = Object.values(os.networkInterfaces()).flat().filter(info => info && info.family === 'IPv4' && !info.internal);
  console.log('\nMachly está listo con el diseño agronómico.');
  console.log(`  Esta computadora: http://localhost:${port}`);
  for (const info of addresses) console.log(`  Teléfono (misma Wi-Fi): http://${info.address}:${port}`);
  console.log(`  Base de datos: ${databasePath}\n`);
});

function shutdown() { server.close(() => { api.locals.db.close(); process.exit(0); }); }
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
