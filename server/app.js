const express = require('express');
const path = require('node:path');
const { openDatabase } = require('./database');
const { hashPassword, verifyPassword, signToken, authMiddleware } = require('./auth');

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ACTIVE_BOOKING_STATUSES = ['requested', 'confirmed', 'active'];
const BOOKING_TRANSITIONS = {
  requested: ['confirmed', 'rejected', 'cancelled'],
  confirmed: ['active', 'cancelled'],
  active: ['completed'],
  completed: [], cancelled: [], rejected: []
};

function createApp(options = {}) {
  const databasePath = options.databasePath || process.env.DATABASE_PATH || path.join(__dirname, '..', 'data', 'machly.db');
  const jwtSecret = options.jwtSecret || process.env.JWT_SECRET || 'machly-local-development-secret-change-me';
  const publicDir = path.join(__dirname, '..', 'public');
  const db = openDatabase(databasePath);
  const app = express();
  app.locals.db = db;

  app.disable('x-powered-by');
  app.use(express.json({ limit: '1mb' }));
  app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
  });
  app.use('/api', authMiddleware(jwtSecret, false));

  const requireAuth = authMiddleware(jwtSecret, true);
  const requireRole = (...roles) => (req, res, next) => {
    if (!req.auth || !roles.includes(req.auth.role)) return res.status(403).json({ error: 'No tienes permiso para realizar esta acción.' });
    next();
  };

  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'Machly API', version: '2.0.0', time: new Date().toISOString() });
  });

  app.post('/api/auth/register', (req, res) => {
    const fullName = clean(req.body.fullName, 100);
    const email = clean(req.body.email, 160).toLowerCase();
    const phone = clean(req.body.phone, 30);
    const password = String(req.body.password || '');
    const role = req.body.role === 'owner' ? 'owner' : 'renter';
    if (fullName.length < 3) return bad(res, 'Ingresa tu nombre completo.');
    if (!EMAIL_RE.test(email)) return bad(res, 'Ingresa un correo electrónico válido.');
    if (password.length < 8) return bad(res, 'La contraseña debe tener al menos 8 caracteres.');
    try {
      const result = db.prepare('INSERT INTO users (full_name,email,phone,password_hash,role) VALUES (?,?,?,?,?)')
        .run(fullName, email, phone, hashPassword(password), role);
      const user = publicUser(db.prepare('SELECT * FROM users WHERE id = ?').get(result.lastInsertRowid));
      res.status(201).json({ ...signToken(user, jwtSecret), user });
    } catch (error) {
      if (String(error.message).includes('UNIQUE')) return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });
      throw error;
    }
  });

  app.post('/api/auth/login', (req, res) => {
    const email = clean(req.body.email, 160).toLowerCase();
    const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
    if (!user || !verifyPassword(String(req.body.password || ''), user.password_hash)) {
      return res.status(401).json({ error: 'Correo o contraseña incorrectos.' });
    }
    const safe = publicUser(user);
    res.json({ ...signToken(safe, jwtSecret), user: safe });
  });

  app.get('/api/auth/me', requireAuth, (req, res) => {
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.auth.sub);
    if (!user) return res.status(404).json({ error: 'Usuario no encontrado.' });
    res.json(publicUser(user));
  });

  app.get('/api/categories', (req, res) => {
    const rows = db.prepare(`SELECT type, category, COUNT(*) AS count FROM equipment WHERE status != 'inactive' GROUP BY type, category ORDER BY category`).all();
    res.json(rows);
  });

  app.get('/api/equipment', (req, res) => {
    const page = clampInt(req.query.page, 1, 10000, 1);
    const limit = clampInt(req.query.limit, 1, 100, 24);
    const where = ["e.status != 'inactive'"];
    const params = [];
    const filters = [
      ['type', 'e.type = ?'], ['category', 'e.category = ?'], ['city', 'e.city = ?']
    ];
    for (const [name, sql] of filters) if (req.query[name]) { where.push(sql); params.push(clean(req.query[name], 80)); }
    if (req.query.search) {
      const term = `%${clean(req.query.search, 100)}%`;
      where.push('(e.name LIKE ? OR e.brand LIKE ? OR e.model LIKE ? OR e.category LIKE ? OR e.city LIKE ?)');
      params.push(term, term, term, term, term);
    }
    if (req.query.minPrice) { where.push('e.price_day >= ?'); params.push(Number(req.query.minPrice) || 0); }
    if (req.query.maxPrice) { where.push('e.price_day <= ?'); params.push(Number(req.query.maxPrice) || 999999999); }
    if (req.query.featured === 'true') where.push('e.featured = 1');
    if (validDate(req.query.startDate) && validDate(req.query.endDate)) {
      where.push(`NOT EXISTS (SELECT 1 FROM bookings b WHERE b.equipment_id = e.id AND b.status IN ('requested','confirmed','active') AND b.start_date <= ? AND b.end_date >= ?)`);
      params.push(req.query.endDate, req.query.startDate);
    }
    const sortMap = { price_asc: 'e.price_day ASC', price_desc: 'e.price_day DESC', newest: 'e.created_at DESC', rating: 'rating DESC' };
    const order = sortMap[req.query.sort] || 'e.featured DESC, e.created_at DESC';
    const base = `FROM equipment e JOIN users u ON u.id=e.owner_id LEFT JOIN reviews r ON r.equipment_id=e.id WHERE ${where.join(' AND ')}`;
    const total = db.prepare(`SELECT COUNT(DISTINCT e.id) AS count ${base}`).get(...params).count;
    const favoriteJoin = req.auth ? ', EXISTS(SELECT 1 FROM favorites f WHERE f.equipment_id=e.id AND f.user_id=?) AS is_favorite' : ', 0 AS is_favorite';
    const selectParams = req.auth ? [req.auth.sub, ...params, limit, (page - 1) * limit] : [...params, limit, (page - 1) * limit];
    const rows = db.prepare(`SELECT e.*,u.full_name AS owner_name,COALESCE(AVG(r.rating),0) AS rating,COUNT(DISTINCT r.id) AS reviews_count ${favoriteJoin} ${base} GROUP BY e.id ORDER BY ${order} LIMIT ? OFFSET ?`).all(...selectParams);
    res.json({ items: rows.map(equipmentJson), page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) });
  });

  app.get('/api/equipment/:id', (req, res) => {
    const favorite = req.auth ? ', EXISTS(SELECT 1 FROM favorites f WHERE f.equipment_id=e.id AND f.user_id=?) AS is_favorite' : ', 0 AS is_favorite';
    const params = req.auth ? [req.auth.sub, req.params.id] : [req.params.id];
    const item = db.prepare(`SELECT e.*,u.full_name AS owner_name,u.phone AS owner_phone,COALESCE(AVG(r.rating),0) AS rating,COUNT(DISTINCT r.id) AS reviews_count ${favorite} FROM equipment e JOIN users u ON u.id=e.owner_id LEFT JOIN reviews r ON r.equipment_id=e.id WHERE e.id=? GROUP BY e.id`).get(...params);
    if (!item) return res.status(404).json({ error: 'Equipo no encontrado.' });
    const reviews = db.prepare('SELECT r.id,r.rating,r.comment,r.created_at,u.full_name FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.equipment_id=? ORDER BY r.created_at DESC').all(req.params.id);
    const blockedDates = db.prepare(`SELECT start_date,end_date FROM bookings WHERE equipment_id=? AND status IN ('requested','confirmed','active') ORDER BY start_date`).all(req.params.id);
    res.json({ ...equipmentJson(item), reviews, blockedDates });
  });

  app.post('/api/equipment', requireAuth, requireRole('owner', 'admin'), (req, res) => {
    const value = equipmentInput(req.body);
    if (value.error) return bad(res, value.error);
    const result = db.prepare(`INSERT INTO equipment (owner_id,type,category,name,brand,model,year,price_day,deposit,city,province,description,features,image,status,featured) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`)
      .run(req.auth.sub, ...value.values, req.auth.role === 'admin' && req.body.featured ? 1 : 0);
    res.status(201).json(equipmentJson(db.prepare('SELECT * FROM equipment WHERE id=?').get(result.lastInsertRowid)));
  });

  app.put('/api/equipment/:id', requireAuth, requireRole('owner', 'admin'), (req, res) => {
    const existing = db.prepare('SELECT * FROM equipment WHERE id=?').get(req.params.id);
    if (!existing) return res.status(404).json({ error: 'Equipo no encontrado.' });
    if (req.auth.role !== 'admin' && existing.owner_id !== req.auth.sub) return res.status(403).json({ error: 'Solo el propietario puede editar este equipo.' });
    const value = equipmentInput(req.body);
    if (value.error) return bad(res, value.error);
    const featured = req.auth.role === 'admin' ? (req.body.featured ? 1 : 0) : existing.featured;
    db.prepare(`UPDATE equipment SET type=?,category=?,name=?,brand=?,model=?,year=?,price_day=?,deposit=?,city=?,province=?,description=?,features=?,image=?,status=?,featured=?,updated_at=CURRENT_TIMESTAMP WHERE id=?`)
      .run(...value.values, featured, req.params.id);
    res.json(equipmentJson(db.prepare('SELECT * FROM equipment WHERE id=?').get(req.params.id)));
  });

  app.delete('/api/equipment/:id', requireAuth, requireRole('owner', 'admin'), (req, res) => {
    const item = db.prepare('SELECT * FROM equipment WHERE id=?').get(req.params.id);
    if (!item) return res.status(404).json({ error: 'Equipo no encontrado.' });
    if (req.auth.role !== 'admin' && item.owner_id !== req.auth.sub) return res.status(403).json({ error: 'Solo el propietario puede retirar este equipo.' });
    db.prepare("UPDATE equipment SET status='inactive',updated_at=CURRENT_TIMESTAMP WHERE id=?").run(req.params.id);
    res.sendStatus(204);
  });

  app.get('/api/my/equipment', requireAuth, requireRole('owner', 'admin'), (req, res) => {
    const rows = req.auth.role === 'admin'
      ? db.prepare('SELECT * FROM equipment ORDER BY created_at DESC').all()
      : db.prepare('SELECT * FROM equipment WHERE owner_id=? ORDER BY created_at DESC').all(req.auth.sub);
    res.json(rows.map(equipmentJson));
  });

  app.get('/api/bookings', requireAuth, (req, res) => {
    let rows;
    if ((req.query.scope === 'owner' && ['owner', 'admin'].includes(req.auth.role)) || req.auth.role === 'admin' && req.query.scope === 'all') {
      const ownerClause = req.auth.role === 'admin' && req.query.scope === 'all' ? '' : 'WHERE e.owner_id=?';
      rows = db.prepare(`${bookingSelect()} ${ownerClause} ORDER BY b.created_at DESC`).all(...(ownerClause ? [req.auth.sub] : []));
    } else {
      rows = db.prepare(`${bookingSelect()} WHERE b.renter_id=? ORDER BY b.created_at DESC`).all(req.auth.sub);
    }
    res.json(rows.map(bookingJson));
  });

  app.post('/api/bookings', requireAuth, (req, res) => {
    const equipmentId = Number(req.body.equipmentId);
    const startDate = String(req.body.startDate || '');
    const endDate = String(req.body.endDate || '');
    if (!validDate(startDate) || !validDate(endDate) || endDate < startDate) return bad(res, 'Selecciona un rango de fechas válido.');
    if (startDate < localDate()) return bad(res, 'La fecha de inicio no puede estar en el pasado.');
    const item = db.prepare("SELECT * FROM equipment WHERE id=? AND status='available'").get(equipmentId);
    if (!item) return res.status(404).json({ error: 'El equipo no está disponible.' });
    if (item.owner_id === req.auth.sub) return bad(res, 'No puedes reservar tu propio equipo.');
    const overlap = db.prepare(`SELECT id FROM bookings WHERE equipment_id=? AND status IN ('requested','confirmed','active') AND start_date<=? AND end_date>=?`).get(equipmentId, endDate, startDate);
    if (overlap) return res.status(409).json({ error: 'El equipo ya tiene una reserva en esas fechas.' });
    const days = daysInclusive(startDate, endDate);
    const subtotal = round(item.price_day * days);
    const serviceFee = round(subtotal * 0.08);
    const total = round(subtotal + serviceFee + item.deposit);
    const result = db.prepare(`INSERT INTO bookings (equipment_id,renter_id,start_date,end_date,days,subtotal,service_fee,total,message) VALUES (?,?,?,?,?,?,?,?,?)`)
      .run(equipmentId, req.auth.sub, startDate, endDate, days, subtotal, serviceFee, total, clean(req.body.message, 500));
    const booking = db.prepare(`${bookingSelect()} WHERE b.id=?`).get(result.lastInsertRowid);
    res.status(201).json(bookingJson(booking));
  });

  app.patch('/api/bookings/:id/status', requireAuth, (req, res) => {
    const booking = db.prepare('SELECT b.*,e.owner_id FROM bookings b JOIN equipment e ON e.id=b.equipment_id WHERE b.id=?').get(req.params.id);
    if (!booking) return res.status(404).json({ error: 'Reserva no encontrada.' });
    const nextStatus = String(req.body.status || '');
    const isOwner = booking.owner_id === req.auth.sub || req.auth.role === 'admin';
    const isRenter = booking.renter_id === req.auth.sub;
    if (!isOwner && !isRenter) return res.status(403).json({ error: 'No tienes acceso a esta reserva.' });
    if (!BOOKING_TRANSITIONS[booking.status]?.includes(nextStatus)) return bad(res, `No se puede cambiar de ${booking.status} a ${nextStatus}.`);
    if (isRenter && nextStatus !== 'cancelled') return res.status(403).json({ error: 'El arrendatario solo puede cancelar la solicitud.' });
    if (isOwner && ['cancelled'].includes(nextStatus) && !isRenter) return bad(res, 'El propietario debe rechazar, no cancelar, una solicitud.');
    db.prepare('UPDATE bookings SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?').run(nextStatus, req.params.id);
    res.json(bookingJson(db.prepare(`${bookingSelect()} WHERE b.id=?`).get(req.params.id)));
  });

  app.get('/api/favorites', requireAuth, (req, res) => {
    const rows = db.prepare(`SELECT e.*,u.full_name owner_name,COALESCE(AVG(r.rating),0) rating,COUNT(DISTINCT r.id) reviews_count,1 is_favorite FROM favorites f JOIN equipment e ON e.id=f.equipment_id JOIN users u ON u.id=e.owner_id LEFT JOIN reviews r ON r.equipment_id=e.id WHERE f.user_id=? GROUP BY e.id ORDER BY f.created_at DESC`).all(req.auth.sub);
    res.json(rows.map(equipmentJson));
  });

  app.post('/api/favorites/:equipmentId', requireAuth, (req, res) => {
    if (!db.prepare('SELECT id FROM equipment WHERE id=?').get(req.params.equipmentId)) return res.status(404).json({ error: 'Equipo no encontrado.' });
    db.prepare('INSERT OR IGNORE INTO favorites (user_id,equipment_id) VALUES (?,?)').run(req.auth.sub, req.params.equipmentId);
    res.status(201).json({ favorite: true });
  });

  app.delete('/api/favorites/:equipmentId', requireAuth, (req, res) => {
    db.prepare('DELETE FROM favorites WHERE user_id=? AND equipment_id=?').run(req.auth.sub, req.params.equipmentId);
    res.sendStatus(204);
  });

  app.post('/api/reviews', requireAuth, (req, res) => {
    const booking = db.prepare("SELECT * FROM bookings WHERE id=? AND renter_id=? AND status='completed'").get(req.body.bookingId, req.auth.sub);
    if (!booking) return bad(res, 'Solo puedes calificar una reserva completada.');
    const rating = Number(req.body.rating);
    if (!Number.isInteger(rating) || rating < 1 || rating > 5) return bad(res, 'La calificación debe estar entre 1 y 5.');
    try {
      const result = db.prepare('INSERT INTO reviews (booking_id,equipment_id,user_id,rating,comment) VALUES (?,?,?,?,?)')
        .run(booking.id, booking.equipment_id, req.auth.sub, rating, clean(req.body.comment, 600));
      res.status(201).json(db.prepare('SELECT * FROM reviews WHERE id=?').get(result.lastInsertRowid));
    } catch (error) {
      if (String(error.message).includes('UNIQUE')) return res.status(409).json({ error: 'Esta reserva ya fue calificada.' });
      throw error;
    }
  });

  app.get('/api/dashboard', requireAuth, (req, res) => {
    const userId = req.auth.sub;
    const stats = req.auth.role === 'renter'
      ? { bookings: db.prepare('SELECT COUNT(*) count FROM bookings WHERE renter_id=?').get(userId).count, favorites: db.prepare('SELECT COUNT(*) count FROM favorites WHERE user_id=?').get(userId).count }
      : { listings: db.prepare('SELECT COUNT(*) count FROM equipment WHERE owner_id=? AND status != \'inactive\'').get(userId).count, pending: db.prepare("SELECT COUNT(*) count FROM bookings b JOIN equipment e ON e.id=b.equipment_id WHERE e.owner_id=? AND b.status='requested'").get(userId).count, revenue: db.prepare("SELECT COALESCE(SUM(b.subtotal),0) total FROM bookings b JOIN equipment e ON e.id=b.equipment_id WHERE e.owner_id=? AND b.status IN ('completed','active')").get(userId).total };
    res.json(stats);
  });

  app.use(express.static(publicDir, { extensions: ['html'], maxAge: options.isTest ? 0 : '1h' }));
  app.use((req, res, next) => {
    if (req.method === 'GET' && !req.path.startsWith('/api/')) return res.sendFile(path.join(publicDir, 'index.html'));
    next();
  });
  app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada.' }));
  app.use((error, req, res, next) => {
    console.error(error);
    if (res.headersSent) return next(error);
    res.status(500).json({ error: 'Ocurrió un error interno en el servidor.' });
  });

  return app;
}

function clean(value, max = 255) { return String(value || '').trim().slice(0, max); }
function bad(res, error) { return res.status(400).json({ error }); }
function clampInt(value, min, max, fallback) { const n = Number.parseInt(value, 10); return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback; }
function round(value) { return Math.round((value + Number.EPSILON) * 100) / 100; }
function validDate(value) { return /^\d{4}-\d{2}-\d{2}$/.test(String(value || '')) && !Number.isNaN(Date.parse(`${value}T00:00:00Z`)); }
function localDate() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`; }
function daysInclusive(start, end) { return Math.floor((Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86400000) + 1; }
function publicUser(user) { return { id: Number(user.id), fullName: user.full_name, email: user.email, phone: user.phone, role: user.role, createdAt: user.created_at }; }
function equipmentJson(item) { return { ...item, id: Number(item.id), owner_id: Number(item.owner_id), features: safeJson(item.features, []), is_favorite: Boolean(item.is_favorite), rating: round(Number(item.rating || 0)), reviews_count: Number(item.reviews_count || 0) }; }
function bookingJson(item) { return { ...item, id: Number(item.id), equipment_id: Number(item.equipment_id), renter_id: Number(item.renter_id), features: safeJson(item.features, []) }; }
function safeJson(value, fallback) { try { return JSON.parse(value); } catch { return fallback; } }
function bookingSelect() { return `SELECT b.*,e.name equipment_name,e.image,e.city,e.price_day,e.owner_id,u.full_name renter_name,u.phone renter_phone FROM bookings b JOIN equipment e ON e.id=b.equipment_id JOIN users u ON u.id=b.renter_id`; }

function equipmentInput(body) {
  const type = body.type === 'vehicle' ? 'vehicle' : body.type === 'machinery' ? 'machinery' : '';
  const category = clean(body.category, 80), name = clean(body.name, 140), brand = clean(body.brand, 80), model = clean(body.model, 80);
  const year = Number(body.year), price = Number(body.priceDay), deposit = Number(body.deposit || 0);
  const city = clean(body.city, 100), province = clean(body.province, 100), description = clean(body.description, 1500);
  const features = Array.isArray(body.features) ? body.features.map(v => clean(v, 80)).filter(Boolean).slice(0, 12) : clean(body.features, 500).split(',').map(v => v.trim()).filter(Boolean).slice(0, 12);
  const image = clean(body.image, 500) || '/assets/equipment/default.svg';
  const status = ['available', 'maintenance', 'inactive'].includes(body.status) ? body.status : 'available';
  if (!type || !category || !name || !brand || !model || !city || !province || description.length < 20) return { error: 'Completa todos los datos del equipo y una descripción de al menos 20 caracteres.' };
  if (!Number.isInteger(year) || year < 1980 || year > new Date().getFullYear() + 1) return { error: 'El año del equipo no es válido.' };
  if (!Number.isFinite(price) || price <= 0 || !Number.isFinite(deposit) || deposit < 0) return { error: 'El precio y la garantía deben ser valores válidos.' };
  return { values: [type, category, name, brand, model, year, round(price), round(deposit), city, province, description, JSON.stringify(features), image, status] };
}

module.exports = { createApp };
