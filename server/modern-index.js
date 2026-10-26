const express = require('express');
const os = require('node:os');
const path = require('node:path');
const { createApp } = require('./app');

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const root = path.join(__dirname, '..');
const databasePath = process.env.DATABASE_PATH ? path.resolve(process.env.DATABASE_PATH) : path.join(root, 'data', 'machly.db');
const api = createApp({ databasePath });
const app = express();

// Conserva la API probada y sustituye únicamente la capa visual.
app.use((req, res, next) => req.path.startsWith('/api/') ? api(req, res, next) : next());
app.get('/app.js', (req, res) => res.sendFile(path.join(root, 'public', 'app.js')));
app.use('/assets', express.static(path.join(root, 'public', 'assets'), { maxAge: '1d' }));
app.use(express.static(path.join(root, 'modern'), { extensions: ['html'], maxAge: '1h' }));
app.use((req, res) => req.method === 'GET' ? res.sendFile(path.join(root, 'modern', 'index.html')) : res.status(404).json({ error: 'Ruta no encontrada.' }));

const server = app.listen(port, host, () => {
  const addresses = Object.values(os.networkInterfaces()).flat().filter(info => info && info.family === 'IPv4' && !info.internal);
  console.log('\nMachly Moderno está funcionando.');
  console.log(`  Esta computadora: http://localhost:${port}`);
  for (const info of addresses) console.log(`  Teléfono (misma Wi-Fi): http://${info.address}:${port}`);
  console.log(`  Base de datos: ${databasePath}\n`);
});

function shutdown() { server.close(() => { api.locals.db.close(); process.exit(0); }); }
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
