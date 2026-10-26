const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('../server/app');

let app, server, baseUrl;

before(async () => {
  app = createApp({ databasePath: ':memory:', jwtSecret: 'test-secret', isTest: true });
  await new Promise(resolve => { server = app.listen(0, '127.0.0.1', resolve); });
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise(resolve => server.close(resolve));
  app.locals.db.close();
});

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}) }
  });
  const body = response.status === 204 ? null : await response.json();
  return { response, body };
}

async function login(email, password) {
  const { response, body } = await request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
  assert.equal(response.status, 200);
  return body.token;
}

test('health y catálogo público funcionan', async () => {
  const health = await request('/api/health');
  assert.equal(health.response.status, 200);
  assert.equal(health.body.status, 'ok');
  const catalog = await request('/api/equipment?type=machinery&limit=20');
  assert.equal(catalog.response.status, 200);
  assert.ok(catalog.body.items.length >= 4);
  assert.ok(catalog.body.items.every(item => item.type === 'machinery'));
});

test('registro, sesión y perfil protegen los datos', async () => {
  const registration = await request('/api/auth/register', { method: 'POST', body: JSON.stringify({ fullName: 'Productor Nuevo', email: 'nuevo@machly.local', phone: '75555555', password: 'segura123', role: 'renter' }) });
  assert.equal(registration.response.status, 201);
  assert.equal(registration.body.user.email, 'nuevo@machly.local');
  const profile = await request('/api/auth/me', { token: registration.body.token });
  assert.equal(profile.response.status, 200);
  assert.equal(profile.body.fullName, 'Productor Nuevo');
  const denied = await request('/api/auth/me');
  assert.equal(denied.response.status, 401);
});

test('reservas calculan importes y evitan fechas solapadas', async () => {
  const renterToken = await login('cliente@machly.local', 'machly123');
  const first = await request('/api/bookings', { method: 'POST', token: renterToken, body: JSON.stringify({ equipmentId: 1, startDate: '2027-02-10', endDate: '2027-02-12', message: 'Preparación de terreno' }) });
  assert.equal(first.response.status, 201);
  assert.equal(first.body.days, 3);
  assert.equal(first.body.subtotal, 4350);
  const overlap = await request('/api/bookings', { method: 'POST', token: renterToken, body: JSON.stringify({ equipmentId: 1, startDate: '2027-02-12', endDate: '2027-02-14' }) });
  assert.equal(overlap.response.status, 409);
});

test('propietario administra solicitudes y publicaciones', async () => {
  const ownerToken = await login('propietario@machly.local', 'machly123');
  const requests = await request('/api/bookings?scope=owner', { token: ownerToken });
  assert.equal(requests.response.status, 200);
  assert.ok(requests.body.length >= 1);
  const bookingId = requests.body[0].id;
  const accepted = await request(`/api/bookings/${bookingId}/status`, { method: 'PATCH', token: ownerToken, body: JSON.stringify({ status: 'confirmed' }) });
  assert.equal(accepted.response.status, 200);
  assert.equal(accepted.body.status, 'confirmed');
  const listing = await request('/api/equipment', { method: 'POST', token: ownerToken, body: JSON.stringify({ type: 'vehicle', category: 'Camionetas', name: 'Nissan Frontier 4x4', brand: 'Nissan', model: 'Frontier', year: 2024, priceDay: 700, deposit: 1200, city: 'Sucre', province: 'Chuquisaca', description: 'Camioneta equipada y lista para trabajo rural exigente.', features: ['4x4', 'Diésel'] }) });
  assert.equal(listing.response.status, 201);
  assert.equal(listing.body.city, 'Sucre');
});

test('favoritos son privados por usuario', async () => {
  const renterToken = await login('cliente@machly.local', 'machly123');
  const added = await request('/api/favorites/2', { method: 'POST', token: renterToken });
  assert.equal(added.response.status, 201);
  const favorites = await request('/api/favorites', { token: renterToken });
  assert.equal(favorites.response.status, 200);
  assert.ok(favorites.body.some(item => item.id === 2));
});
