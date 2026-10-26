const state = {
  token: localStorage.getItem('machly_token') || '',
  user: readJson(localStorage.getItem('machly_user')),
  type: '', page: 1, pages: 1, total: 0, items: [], currentItem: null,
  dashboardPanel: 'summary'
};

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const money = value => new Intl.NumberFormat('es-BO', { style: 'currency', currency: 'BOB', maximumFractionDigits: 0 }).format(Number(value || 0));
const dateFmt = value => value ? new Intl.DateTimeFormat('es-BO', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`)) : '';
const statusLabel = { requested: 'Solicitada', confirmed: 'Confirmada', active: 'En curso', completed: 'Completada', cancelled: 'Cancelada', rejected: 'Rechazada', available: 'Disponible', maintenance: 'Mantenimiento', inactive: 'Inactivo' };
const roleLabel = { renter: 'Arrendatario', owner: 'Propietario', admin: 'Administrador' };

document.addEventListener('DOMContentLoaded', boot);

async function boot() {
  bindEvents();
  setDateMinimums();
  updateSessionUI();
  renderSkeletons();
  await loadEquipment(true);
  if (state.token) refreshUser();
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js').catch(() => {});
}

function bindEvents() {
  $('#heroSearchForm').addEventListener('submit', event => {
    event.preventDefault();
    $('#filterForm [name="search"]').value = $('#heroSearch').value;
    $('#filterForm [name="city"]').value = $('#heroCity').value;
    state.page = 1; loadEquipment(true); location.hash = 'catalogo';
  });
  $('#filterForm').addEventListener('submit', event => { event.preventDefault(); state.page = 1; loadEquipment(true); });
  $('#clearFilters').addEventListener('click', () => { $('#filterForm').reset(); setType(''); state.page = 1; loadEquipment(true); });
  $('#loadMoreButton').addEventListener('click', () => { state.page += 1; loadEquipment(false); });
  $$('.segmented button').forEach(button => button.addEventListener('click', () => { setType(button.dataset.type); state.page = 1; loadEquipment(true); }));
  $$('#categoryGrid button').forEach(button => button.addEventListener('click', () => {
    setType(button.dataset.type); $('#filterForm [name="search"]').value = button.dataset.category; state.page = 1; loadEquipment(true); location.hash = 'catalogo';
  }));
  $('#equipmentGrid').addEventListener('click', cardClick);
  $('#loginButton').addEventListener('click', () => openAuth('login'));
  $('#registerButton').addEventListener('click', () => openAuth('register'));
  $('#favoritesButton').addEventListener('click', () => openAccount('favorites'));
  $('#mobileFavorites').addEventListener('click', () => openAccount('favorites'));
  $('#mobileAccount').addEventListener('click', () => openAccount('summary'));
  $('#userMenuButton').addEventListener('click', () => showDashboard('summary'));
  $('#ownerCtaButton').addEventListener('click', publishEquipment);
  $('#backHomeButton').addEventListener('click', showHome);
  $('#logoutButton').addEventListener('click', logout);
  $('#dashboardNav').addEventListener('click', event => { const button = event.target.closest('[data-panel]'); if (button) showDashboard(button.dataset.panel); });
  $$('.modal-close').forEach(button => button.addEventListener('click', () => document.getElementById(button.dataset.close).close()));
  $$('.modal').forEach(dialog => dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); }));
  $$('.auth-tabs button').forEach(button => button.addEventListener('click', () => renderAuth(button.dataset.authTab)));
  window.addEventListener('online', () => $('#offlineBanner').hidden = true);
  window.addEventListener('offline', () => $('#offlineBanner').hidden = false);
}

async function api(path, options = {}) {
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...options.headers };
  try {
    const response = await fetch(path, { ...options, headers });
    $('#offlineBanner').hidden = true;
    const payload = response.status === 204 ? null : await response.json().catch(() => ({}));
    if (response.status === 401 && state.token) { clearSession(); updateSessionUI(); }
    if (!response.ok) throw new Error(payload?.error || `Error ${response.status}`);
    return payload;
  } catch (error) {
    if (error instanceof TypeError) $('#offlineBanner').hidden = false;
    throw error;
  }
}

async function loadEquipment(reset) {
  if (reset) { state.items = []; renderSkeletons(); }
  const form = new FormData($('#filterForm'));
  const params = new URLSearchParams({ page: state.page, limit: 12 });
  if (state.type) params.set('type', state.type);
  for (const [key, value] of form) if (value) params.set(key, value);
  try {
    const data = await api(`/api/equipment?${params}`);
    state.pages = data.pages; state.total = data.total;
    state.items = reset ? data.items : [...state.items, ...data.items];
    renderEquipment();
  } catch (error) {
    $('#equipmentGrid').innerHTML = emptyState('⚠', 'No pudimos cargar los equipos', error.message);
  }
}

function renderSkeletons() {
  $('#equipmentGrid').innerHTML = Array.from({ length: 8 }, () => $('#cardSkeleton').innerHTML).join('');
}

function renderEquipment() {
  $('#resultsCount').textContent = `${state.total} ${state.total === 1 ? 'equipo encontrado' : 'equipos encontrados'}`;
  $('#loadMoreButton').hidden = state.page >= state.pages;
  if (!state.items.length) { $('#equipmentGrid').innerHTML = emptyState('⌕', 'No encontramos equipos', 'Prueba con otra ubicación, fecha o palabra de búsqueda.'); return; }
  $('#equipmentGrid').innerHTML = state.items.map(cardHtml).join('');
}

function cardHtml(item) {
  return `<article class="equipment-card" data-id="${item.id}">
    <button class="card-main-button" data-action="detail" aria-label="Ver ${esc(item.name)}"></button>
    <div class="card-image"><img src="${asset(item.image)}" alt="${esc(item.name)}" loading="lazy"><span class="status-badge ${item.status}">${statusLabel[item.status] || item.status}</span><button class="favorite-toggle ${item.is_favorite ? 'active' : ''}" data-action="favorite" aria-label="Favorito">${item.is_favorite ? '♥' : '♡'}</button></div>
    <div class="card-body"><div class="card-kicker"><span>${esc(item.category)}</span><span class="rating">★ ${item.rating ? item.rating.toFixed(1) : 'Nuevo'}</span></div><h3>${esc(item.name)}</h3><div class="card-location">⌖ ${esc(item.city)}, ${esc(item.province)}</div><div class="card-footer"><span class="price"><strong>${money(item.price_day)}</strong><small> / día</small></span><span class="card-arrow">→</span></div></div>
  </article>`;
}

async function cardClick(event) {
  const card = event.target.closest('.equipment-card'); if (!card) return;
  const id = Number(card.dataset.id);
  if (event.target.closest('[data-action="favorite"]')) return toggleFavorite(id, event.target.closest('button'));
  if (event.target.closest('[data-action="detail"]')) openDetail(id);
}

async function openDetail(id) {
  const dialog = $('#detailDialog');
  $('#detailContent').innerHTML = '<div class="panel-empty">Cargando detalles…</div>'; dialog.showModal();
  try {
    const item = await api(`/api/equipment/${id}`); state.currentItem = item;
    const available = item.status === 'available';
    $('#detailContent').innerHTML = `<div class="detail-layout"><div class="detail-image"><img src="${asset(item.image)}" alt="${esc(item.name)}"></div><div class="detail-info"><span class="eyebrow">${esc(item.type === 'vehicle' ? 'Vehículo' : 'Maquinaria')} · ${esc(item.category)}</span><h2>${esc(item.name)}</h2><div class="detail-location">⌖ ${esc(item.city)}, ${esc(item.province)} · ${item.year}</div><p class="detail-description">${esc(item.description)}</p><div class="feature-list">${item.features.map(feature => `<span>✓ ${esc(feature)}</span>`).join('')}</div><div class="detail-owner"><span><small>Publicado por</small><br><strong>${esc(item.owner_name)}</strong></span><span class="rating">★ ${item.rating ? item.rating.toFixed(1) : 'Nuevo'} <small>(${item.reviews_count})</small></span></div><div class="detail-book"><span class="price"><small>Precio por día</small><br><strong>${money(item.price_day)}</strong>${item.deposit ? `<br><small>Garantía: ${money(item.deposit)}</small>` : ''}</span><button class="button button-primary" id="reserveNow" ${available ? '' : 'disabled'}>${available ? 'Solicitar reserva' : 'No disponible'}</button></div></div></div>`;
    $('#reserveNow')?.addEventListener('click', () => { dialog.close(); openBooking(item); });
  } catch (error) { $('#detailContent').innerHTML = emptyState('⚠', 'No pudimos abrir el equipo', error.message); }
}

async function toggleFavorite(id, button) {
  if (!state.user) return openAuth('login', 'Inicia sesión para guardar favoritos.');
  const active = button?.classList.contains('active') || state.items.find(item => item.id === id)?.is_favorite;
  try {
    await api(`/api/favorites/${id}`, { method: active ? 'DELETE' : 'POST' });
    state.items.forEach(item => { if (item.id === id) item.is_favorite = !active; });
    if (button) { button.classList.toggle('active', !active); button.textContent = active ? '♡' : '♥'; }
    toast(active ? 'Se quitó de favoritos.' : 'Guardado en tus favoritos.');
  } catch (error) { toast(error.message, true); }
}

function openAuth(tab = 'login', message = '') { renderAuth(tab, message); $('#authDialog').showModal(); }
function renderAuth(tab, message = '') {
  $$('.auth-tabs button').forEach(button => button.classList.toggle('active', button.dataset.authTab === tab));
  $('#authContent').innerHTML = tab === 'login' ? `<form class="auth-form" id="authForm"><h2>Bienvenido de nuevo</h2><p>${esc(message || 'Ingresa para administrar tus reservas y equipos.')}</p><div class="form-grid"><div class="form-field"><label>Correo electrónico</label><input type="email" name="email" autocomplete="email" required placeholder="tu@correo.com"></div><div class="form-field"><label>Contraseña</label><input type="password" name="password" autocomplete="current-password" required placeholder="••••••••"></div><div id="authError"></div><button class="button button-primary">Ingresar a Machly</button></div><div class="demo-box"><strong>Cuenta de demostración</strong><br>cliente@machly.local · machly123</div></form>` : `<form class="auth-form" id="authForm"><h2>Crea tu cuenta</h2><p>Reserva equipos o publica los tuyos en pocos minutos.</p><div class="form-grid"><div class="form-field"><label>Nombre completo</label><input name="fullName" required minlength="3"></div><div class="form-field"><label>Correo electrónico</label><input type="email" name="email" required></div><div class="form-field"><label>Teléfono</label><input name="phone" inputmode="tel" placeholder="70000000"></div><div class="form-field"><label>Quiero usar Machly para</label><select name="role"><option value="renter">Alquilar equipos</option><option value="owner">Publicar mis equipos</option></select></div><div class="form-field"><label>Contraseña</label><input type="password" name="password" required minlength="8" autocomplete="new-password"></div><div id="authError"></div><button class="button button-primary">Crear mi cuenta</button></div></form>`;
  $('#authForm').addEventListener('submit', event => submitAuth(event, tab));
}

async function submitAuth(event, tab) {
  event.preventDefault(); const form = event.currentTarget; const button = form.querySelector('button[type="submit"],button.button');
  button.disabled = true; button.textContent = 'Un momento…';
  const body = Object.fromEntries(new FormData(form));
  try {
    const data = await api(`/api/auth/${tab === 'login' ? 'login' : 'register'}`, { method: 'POST', body: JSON.stringify(body) });
    setSession(data); $('#authDialog').close(); toast(`Hola, ${data.user.fullName.split(' ')[0]}.`); loadEquipment(true);
  } catch (error) { $('#authError').innerHTML = `<div class="form-error">${esc(error.message)}</div>`; button.disabled = false; button.textContent = tab === 'login' ? 'Ingresar a Machly' : 'Crear mi cuenta'; }
}

function setSession(data) { state.token = data.token; state.user = data.user; localStorage.setItem('machly_token', data.token); localStorage.setItem('machly_user', JSON.stringify(data.user)); updateSessionUI(); }
function clearSession() { state.token = ''; state.user = null; localStorage.removeItem('machly_token'); localStorage.removeItem('machly_user'); }
function logout() { clearSession(); updateSessionUI(); showHome(); loadEquipment(true); toast('Sesión cerrada correctamente.'); }
async function refreshUser() { try { state.user = await api('/api/auth/me'); localStorage.setItem('machly_user', JSON.stringify(state.user)); updateSessionUI(); } catch {} }
function updateSessionUI() {
  $('#loginButton').hidden = Boolean(state.user); $('#registerButton').hidden = Boolean(state.user); $('#userMenuButton').hidden = !state.user;
  if (state.user) { $('#userName').textContent = state.user.fullName.split(' ')[0]; $('#userInitial').textContent = state.user.fullName[0].toUpperCase(); }
  $$('.owner-only').forEach(el => el.hidden = !state.user || !['owner', 'admin'].includes(state.user.role));
}
function openAccount(panel) { state.user ? showDashboard(panel) : openAuth('login'); }

function openBooking(item) {
  if (!state.user) return openAuth('login', 'Inicia sesión para solicitar una reserva.');
  if (item.owner_id === state.user.id) return toast('Este equipo te pertenece; no puedes reservarlo.', true);
  const today = todayIso();
  $('#bookingContent').innerHTML = `<form id="bookingForm"><div class="booking-head"><img src="${asset(item.image)}" alt=""><div><span class="eyebrow">Solicitar reserva</span><h2>${esc(item.name)}</h2><small>${esc(item.city)}</small></div></div><div class="form-grid"><div class="form-field"><label>Fecha de inicio</label><input type="date" name="startDate" min="${today}" required></div><div class="form-field"><label>Fecha de devolución</label><input type="date" name="endDate" min="${today}" required></div><div class="form-field"><label>Mensaje al propietario (opcional)</label><textarea name="message" placeholder="Cuéntale dónde y para qué necesitas el equipo"></textarea></div></div><div class="booking-summary" id="bookingSummary"><div><span>Selecciona tus fechas</span><strong>${money(item.price_day)} / día</strong></div></div><div id="bookingError"></div><button class="button button-primary" style="width:100%">Enviar solicitud</button></form>`;
  const form = $('#bookingForm'); form.addEventListener('input', () => renderBookingPrice(form, item)); form.addEventListener('submit', event => submitBooking(event, item)); $('#bookingDialog').showModal();
}

function renderBookingPrice(form, item) {
  const start = form.startDate.value, end = form.endDate.value;
  if (!start || !end || end < start) return;
  form.endDate.min = start; const days = dateDays(start, end); const subtotal = days * item.price_day; const fee = subtotal * .08; const total = subtotal + fee + item.deposit;
  $('#bookingSummary').innerHTML = `<div><span>${days} ${days === 1 ? 'día' : 'días'} × ${money(item.price_day)}</span><strong>${money(subtotal)}</strong></div><div><span>Servicio Machly (8%)</span><strong>${money(fee)}</strong></div><div><span>Garantía reembolsable</span><strong>${money(item.deposit)}</strong></div><div class="total"><span>Total inicial</span><strong>${money(total)}</strong></div>`;
}

async function submitBooking(event, item) {
  event.preventDefault(); const form = event.currentTarget; const button = form.querySelector('button'); button.disabled = true; button.textContent = 'Enviando…';
  const body = Object.fromEntries(new FormData(form)); body.equipmentId = item.id;
  try { await api('/api/bookings', { method: 'POST', body: JSON.stringify(body) }); $('#bookingDialog').close(); toast('Solicitud enviada al propietario.'); }
  catch (error) { $('#bookingError').innerHTML = `<div class="form-error">${esc(error.message)}</div>`; button.disabled = false; button.textContent = 'Enviar solicitud'; }
}

function showHome() { $('#dashboardView').hidden = true; $('#homeView').hidden = false; window.scrollTo({ top: 0, behavior: 'smooth' }); }
async function showDashboard(panel = 'summary') {
  if (!state.user) return openAuth('login');
  state.dashboardPanel = panel; $('#homeView').hidden = true; $('#dashboardView').hidden = false;
  $('#dashboardGreeting').textContent = `Hola, ${state.user.fullName.split(' ')[0]}`; $('#dashboardRole').textContent = roleLabel[state.user.role];
  $$('#dashboardNav [data-panel]').forEach(button => button.classList.toggle('active', button.dataset.panel === panel));
  window.scrollTo(0, 0); await loadDashboardPanel(panel);
}

async function loadDashboardPanel(panel) {
  const target = $('#dashboardPanel'); target.innerHTML = '<div class="panel-empty">Cargando…</div>';
  try {
    if (panel === 'summary') return renderSummary(target, await Promise.all([api('/api/dashboard'), api('/api/bookings')]));
    if (panel === 'bookings') return renderBookings(target, await api('/api/bookings'), false);
    if (panel === 'favorites') return renderFavoritePanel(target, await api('/api/favorites'));
    if (panel === 'listings') return renderListings(target, await api('/api/my/equipment'));
    if (panel === 'requests') return renderBookings(target, await api('/api/bookings?scope=owner'), true);
  } catch (error) { target.innerHTML = emptyState('⚠', 'No pudimos cargar esta sección', error.message); }
}

function renderSummary(target, [stats, bookings]) {
  const owner = ['owner', 'admin'].includes(state.user.role);
  const cards = owner ? [['Equipos publicados', stats.listings || 0], ['Solicitudes pendientes', stats.pending || 0], ['Ingresos registrados', money(stats.revenue || 0)]] : [['Reservas realizadas', stats.bookings || 0], ['Equipos favoritos', stats.favorites || 0], ['Próximas reservas', bookings.filter(b => ['requested','confirmed'].includes(b.status)).length]];
  target.innerHTML = `<div class="dashboard-header"><div><span class="eyebrow">Tu actividad</span><h1>Resumen</h1></div>${owner ? '<button class="button button-primary" id="newListing">+ Publicar equipo</button>' : '<button class="button button-primary" id="exploreFromDash">Explorar equipos</button>'}</div><div class="stat-grid">${cards.map(([label,value]) => `<article class="stat-card"><small>${label}</small><strong>${value}</strong></article>`).join('')}</div><div class="dashboard-header" style="margin-top:42px"><h2>Actividad reciente</h2></div>${bookings.length ? `<div class="dashboard-list">${bookings.slice(0,4).map(booking => bookingRow(booking, false)).join('')}</div>` : emptyState('⌁','Todavía no hay actividad','Tus reservas aparecerán aquí.')}`;
  $('#newListing')?.addEventListener('click', () => openListingForm()); $('#exploreFromDash')?.addEventListener('click', () => { showHome(); location.hash='catalogo'; });
}

function renderBookings(target, bookings, ownerView) {
  target.innerHTML = `<div class="dashboard-header"><div><span class="eyebrow">${ownerView ? 'Gestión' : 'Alquileres'}</span><h1>${ownerView ? 'Solicitudes recibidas' : 'Mis reservas'}</h1></div></div>${bookings.length ? `<div class="dashboard-list">${bookings.map(item => bookingRow(item, ownerView)).join('')}</div>` : emptyState('▤', ownerView ? 'Aún no recibiste solicitudes' : 'Aún no tienes reservas', ownerView ? 'Cuando alguien solicite uno de tus equipos, aparecerá aquí.' : 'Explora el catálogo y encuentra el equipo ideal.')}`;
  target.querySelectorAll('[data-booking-action]').forEach(button => button.addEventListener('click', () => changeBookingStatus(button.dataset.id, button.dataset.bookingAction)));
}

function bookingRow(item, ownerView) {
  let actions = '';
  if (ownerView && item.status === 'requested') actions = `<button class="button button-primary" data-booking-action="confirmed" data-id="${item.id}">Aceptar</button><button class="button button-outline" data-booking-action="rejected" data-id="${item.id}">Rechazar</button>`;
  if (ownerView && item.status === 'confirmed') actions = `<button class="button button-primary" data-booking-action="active" data-id="${item.id}">Marcar entrega</button>`;
  if (ownerView && item.status === 'active') actions = `<button class="button button-primary" data-booking-action="completed" data-id="${item.id}">Completar</button>`;
  if (!ownerView && ['requested','confirmed'].includes(item.status)) actions = `<button class="button button-outline" data-booking-action="cancelled" data-id="${item.id}">Cancelar</button>`;
  return `<article class="booking-row"><img src="${asset(item.image)}" alt=""><div class="row-copy"><span class="status-pill ${item.status}">${statusLabel[item.status]}</span><h3>${esc(item.equipment_name)}</h3><p>${dateFmt(item.start_date)} — ${dateFmt(item.end_date)} · ${item.days} días</p>${ownerView ? `<p>Solicitante: ${esc(item.renter_name)} · ${esc(item.renter_phone || 'Sin teléfono')}</p>` : ''}<strong>${money(item.total)}</strong></div><div class="row-actions">${actions}</div></article>`;
}

async function changeBookingStatus(id, status) {
  try { await api(`/api/bookings/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }); toast('Reserva actualizada.'); loadDashboardPanel(state.dashboardPanel); }
  catch (error) { toast(error.message, true); }
}

function renderFavoritePanel(target, items) {
  target.innerHTML = `<div class="dashboard-header"><div><span class="eyebrow">Tu selección</span><h1>Favoritos</h1></div></div>${items.length ? `<div class="equipment-grid">${items.map(cardHtml).join('')}</div>` : emptyState('♡','No guardaste favoritos','Toca el corazón de un equipo para encontrarlo rápidamente.')}`;
  target.querySelector('.equipment-grid')?.addEventListener('click', cardClick);
}

function renderListings(target, items) {
  target.innerHTML = `<div class="dashboard-header"><div><span class="eyebrow">Inventario</span><h1>Mis equipos</h1></div><button class="button button-primary" id="newListing">+ Publicar equipo</button></div>${items.length ? `<div class="dashboard-list">${items.map(item => `<article class="listing-row"><img src="${asset(item.image)}" alt=""><div class="row-copy"><span class="status-pill ${item.status}">${statusLabel[item.status]}</span><h3>${esc(item.name)}</h3><p>${esc(item.city)} · ${money(item.price_day)} / día</p></div><div class="row-actions"><button class="button button-outline" data-edit-listing="${item.id}">Editar</button></div></article>`).join('')}</div>` : emptyState('♜','Publica tu primer equipo','Empieza a recibir solicitudes de productores cercanos.')}`;
  $('#newListing').addEventListener('click', () => openListingForm()); target.querySelectorAll('[data-edit-listing]').forEach(button => button.addEventListener('click', () => openListingForm(items.find(item => item.id === Number(button.dataset.editListing)))));
}

function publishEquipment() {
  if (!state.user) return openAuth('register', 'Crea una cuenta de propietario para publicar equipos.');
  if (!['owner','admin'].includes(state.user.role)) return toast('Tu cuenta es de arrendatario. Registra una cuenta de propietario para publicar.', true);
  openListingForm();
}

function openListingForm(item = null) {
  const edit = Boolean(item); const features = item?.features?.join(', ') || '';
  $('#listingContent').innerHTML = `<form id="listingForm"><span class="eyebrow">${edit ? 'Actualizar publicación' : 'Nueva publicación'}</span><h2>${edit ? 'Editar equipo' : 'Publicar un equipo'}</h2><p>Completa la información que ayudará a los arrendatarios a decidir.</p><div class="form-grid listing-form"><div class="form-field"><label>Tipo</label><select name="type"><option value="machinery" ${item?.type==='machinery'?'selected':''}>Maquinaria</option><option value="vehicle" ${item?.type==='vehicle'?'selected':''}>Vehículo</option></select></div><div class="form-field"><label>Categoría</label><input name="category" required value="${attr(item?.category || '')}" placeholder="Tractores"></div><div class="form-field full"><label>Título del equipo</label><input name="name" required value="${attr(item?.name || '')}" placeholder="Tractor John Deere 6110J"></div><div class="form-field"><label>Marca</label><input name="brand" required value="${attr(item?.brand || '')}"></div><div class="form-field"><label>Modelo</label><input name="model" required value="${attr(item?.model || '')}"></div><div class="form-field"><label>Año</label><input name="year" type="number" min="1980" max="2030" required value="${attr(item?.year || '2024')}"></div><div class="form-field"><label>Precio por día (Bs)</label><input name="priceDay" type="number" min="1" step="0.01" required value="${attr(item?.price_day || '')}"></div><div class="form-field"><label>Garantía (Bs)</label><input name="deposit" type="number" min="0" step="0.01" value="${attr(item?.deposit || '0')}"></div><div class="form-field"><label>Ciudad</label><input name="city" required value="${attr(item?.city || '')}"></div><div class="form-field"><label>Departamento</label><input name="province" required value="${attr(item?.province || '')}"></div><div class="form-field full"><label>Características (separadas por coma)</label><input name="features" value="${attr(features)}" placeholder="4x4, aire acondicionado, GPS"></div><div class="form-field full"><label>Descripción</label><textarea name="description" required minlength="20">${esc(item?.description || '')}</textarea></div>${edit ? `<div class="form-field"><label>Estado</label><select name="status"><option value="available" ${item.status==='available'?'selected':''}>Disponible</option><option value="maintenance" ${item.status==='maintenance'?'selected':''}>Mantenimiento</option><option value="inactive" ${item.status==='inactive'?'selected':''}>Inactivo</option></select></div>` : ''}<div id="listingError" class="full"></div><button class="button button-primary full">${edit ? 'Guardar cambios' : 'Publicar equipo'}</button></div></form>`;
  $('#listingForm').addEventListener('submit', event => submitListing(event, item)); $('#listingDialog').showModal();
}

async function submitListing(event, item) {
  event.preventDefault(); const form = event.currentTarget; const button = form.querySelector('button'); button.disabled = true; button.textContent = 'Guardando…';
  const body = Object.fromEntries(new FormData(form)); body.features = body.features.split(',').map(value => value.trim()).filter(Boolean);
  try { await api(item ? `/api/equipment/${item.id}` : '/api/equipment', { method: item ? 'PUT' : 'POST', body: JSON.stringify(body) }); $('#listingDialog').close(); toast(item ? 'Equipo actualizado.' : 'Equipo publicado.'); if (!$('#dashboardView').hidden) showDashboard('listings'); loadEquipment(true); }
  catch (error) { $('#listingError').innerHTML = `<div class="form-error">${esc(error.message)}</div>`; button.disabled = false; button.textContent = item ? 'Guardar cambios' : 'Publicar equipo'; }
}

function setType(type) { state.type = type; $$('.segmented button').forEach(button => button.classList.toggle('active', button.dataset.type === type)); }
function setDateMinimums() { const today = todayIso(); $$('#filterForm input[type="date"]').forEach(input => input.min = today); }
function todayIso() { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`; }
function dateDays(start, end) { return Math.round((new Date(`${end}T00:00:00Z`) - new Date(`${start}T00:00:00Z`)) / 86400000) + 1; }
function emptyState(icon, title, text) { return `<div class="empty-state panel-empty"><span>${icon}</span><h3>${esc(title)}</h3><p>${esc(text)}</p></div>`; }
function toast(message, error = false) { const el = $('#toast'); el.textContent = message; el.className = `toast show${error ? ' error' : ''}`; clearTimeout(toast.timer); toast.timer = setTimeout(() => el.className = 'toast', 3200); }
function asset(value) { const path = String(value || ''); return /^\/assets\/[a-zA-Z0-9_./-]+$/.test(path) ? path : '/assets/equipment/default.svg'; }
function esc(value) { return String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char])); }
function attr(value) { return esc(value).replace(/`/g, '&#96;'); }
function readJson(value) { try { return JSON.parse(value); } catch { return null; } }
