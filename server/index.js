const os = require('node:os');
const path = require('node:path');
const { createApp } = require('./app');

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const databasePath = process.env.DATABASE_PATH ? path.resolve(process.env.DATABASE_PATH) : path.join(__dirname, '..', 'data', 'machly.db');
const app = createApp({ databasePath });

const server = app.listen(port, host, () => {
  const addresses = Object.values(os.networkInterfaces()).flat().filter(info => info && info.family === 'IPv4' && !info.internal);
  console.log('\nMachly está funcionando.');
  console.log(`  Esta computadora: http://localhost:${port}`);
  for (const info of addresses) console.log(`  Teléfono (misma Wi-Fi): http://${info.address}:${port}`);
  console.log(`  Base de datos: ${databasePath}\n`);
});

function shutdown() {
  server.close(() => {
    app.locals.db.close();
    process.exit(0);
  });
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
