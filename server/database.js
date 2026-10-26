const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');
const { hashPassword } = require('./auth');

function openDatabase(filename) {
  if (filename !== ':memory:') fs.mkdirSync(path.dirname(filename), { recursive: true });
  const db = new DatabaseSync(filename);
  db.exec('PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 5000;');
  migrate(db);
  seed(db);
  return db;
}

function migrate(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      full_name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE COLLATE NOCASE,
      phone TEXT DEFAULT '',
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'renter' CHECK(role IN ('renter','owner','admin')),
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS equipment (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      owner_id INTEGER NOT NULL REFERENCES users(id),
      type TEXT NOT NULL CHECK(type IN ('machinery','vehicle')),
      category TEXT NOT NULL,
      name TEXT NOT NULL,
      brand TEXT NOT NULL,
      model TEXT NOT NULL,
      year INTEGER NOT NULL,
      price_day REAL NOT NULL CHECK(price_day > 0),
      deposit REAL NOT NULL DEFAULT 0,
      city TEXT NOT NULL,
      province TEXT NOT NULL,
      description TEXT NOT NULL,
      features TEXT NOT NULL DEFAULT '[]',
      image TEXT NOT NULL DEFAULT '/assets/equipment/default.svg',
      status TEXT NOT NULL DEFAULT 'available' CHECK(status IN ('available','maintenance','inactive')),
      featured INTEGER NOT NULL DEFAULT 0,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS bookings (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      equipment_id INTEGER NOT NULL REFERENCES equipment(id),
      renter_id INTEGER NOT NULL REFERENCES users(id),
      start_date TEXT NOT NULL,
      end_date TEXT NOT NULL,
      days INTEGER NOT NULL CHECK(days > 0),
      subtotal REAL NOT NULL,
      service_fee REAL NOT NULL,
      total REAL NOT NULL,
      status TEXT NOT NULL DEFAULT 'requested' CHECK(status IN ('requested','confirmed','active','completed','cancelled','rejected')),
      message TEXT DEFAULT '',
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS favorites (
      user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      equipment_id INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY(user_id, equipment_id)
    );

    CREATE TABLE IF NOT EXISTS reviews (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      booking_id INTEGER NOT NULL UNIQUE REFERENCES bookings(id),
      equipment_id INTEGER NOT NULL REFERENCES equipment(id),
      user_id INTEGER NOT NULL REFERENCES users(id),
      rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),
      comment TEXT NOT NULL DEFAULT '',
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_equipment_filters ON equipment(type, category, city, status);
    CREATE INDEX IF NOT EXISTS idx_bookings_dates ON bookings(equipment_id, start_date, end_date, status);
    CREATE INDEX IF NOT EXISTS idx_bookings_renter ON bookings(renter_id, created_at);
  `);
}

function seed(db) {
  const count = db.prepare('SELECT COUNT(*) AS count FROM users').get().count;
  if (count > 0) return;

  const addUser = db.prepare('INSERT INTO users (full_name,email,phone,password_hash,role) VALUES (?,?,?,?,?)');
  addUser.run('Administrador Machly', 'admin@machly.local', '70000001', hashPassword('admin123'), 'admin');
  const owner = addUser.run('Agro Rental Santa Cruz', 'propietario@machly.local', '70000002', hashPassword('machly123'), 'owner').lastInsertRowid;
  addUser.run('Cliente de prueba', 'cliente@machly.local', '70000003', hashPassword('machly123'), 'renter');

  const addEquipment = db.prepare(`
    INSERT INTO equipment (owner_id,type,category,name,brand,model,year,price_day,deposit,city,province,description,features,image,status,featured)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
  `);
  const rows = [
    ['machinery','Tractores','Tractor John Deere 6110J','John Deere','6110J',2022,1450,3000,'Santa Cruz de la Sierra','Santa Cruz','Tractor robusto de 110 HP, ideal para preparación de suelo, siembra y labores generales.',['110 HP','Doble tracción','Cabina con aire','GPS agrícola'],'/assets/equipment/tractor.svg','available',1],
    ['machinery','Cosechadoras','Cosechadora Case IH 6130','Case IH','6130',2021,3200,6000,'Montero','Santa Cruz','Cosechadora de alto rendimiento con cabezal para soya y sistema de monitoreo de pérdidas.',['Cabezal 30 pies','Tolva 10.570 L','Piloto automático','Monitor AFS'],'/assets/equipment/harvester.svg','available',1],
    ['machinery','Excavadoras','Excavadora CAT 320','Caterpillar','320 GC',2020,2850,5000,'Cochabamba','Cochabamba','Excavadora hidráulica confiable para movimiento de tierra, drenajes y obras rurales.',['22 toneladas','Cucharón 1 m³','Operador opcional','Entrega disponible'],'/assets/equipment/excavator.svg','available',0],
    ['machinery','Implementos','Sembradora de precisión 12 líneas','Baldan','SPDE 4500',2023,980,1800,'Warnes','Santa Cruz','Sembradora neumática de precisión preparada para maíz, soya y girasol.',['12 líneas','Dosificador neumático','Monitor de semillas','Fertilización'],'/assets/equipment/seeder.svg','available',1],
    ['vehicle','Camionetas','Toyota Hilux 4x4','Toyota','Hilux SRV',2022,650,1500,'Santa Cruz de la Sierra','Santa Cruz','Camioneta doble cabina lista para campo, inspecciones y transporte de personal.',['4x4','5 pasajeros','Diésel','Seguro incluido'],'/assets/equipment/pickup.svg','available',1],
    ['vehicle','Camiones','Camión Volvo FMX','Volvo','FMX 6x4',2019,1800,4000,'La Paz','La Paz','Camión de alto desempeño para transporte pesado en caminos exigentes.',['Carga 25 t','Tolva','GPS','Asistencia en ruta'],'/assets/equipment/truck.svg','available',0],
    ['vehicle','Utilitarios','Suzuki Jimny 4x4','Suzuki','Jimny',2023,420,1000,'Tarija','Tarija','Vehículo compacto 4x4 para visitas técnicas y recorridos por terrenos difíciles.',['4x4 reductora','4 pasajeros','Gasolina','Kilometraje flexible'],'/assets/equipment/pickup.svg','available',0],
    ['machinery','Pulverizadoras','Pulverizadora autopropulsada','Jacto','Uniport 3030',2021,2400,4500,'San Julián','Santa Cruz','Pulverización de gran cobertura con control automático de secciones.',['Barra 36 m','Tanque 3.000 L','Corte automático','Piloto hidráulico'],'/assets/equipment/sprayer.svg','maintenance',0]
  ];
  for (const row of rows) addEquipment.run(owner, ...row.slice(0, 11), JSON.stringify(row[11]), ...row.slice(12));
}

module.exports = { openDatabase };
