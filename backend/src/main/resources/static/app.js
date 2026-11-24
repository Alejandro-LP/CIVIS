const $ = (selector) => document.querySelector(selector);
const state = { token: '', actor: null, view: 'summary', rows: [], page: 0, totalPages: 0, listPage: 0, busy: false };
const ranks = {
  'Policía': ['Patrullero', 'Subintendente', 'Intendente', 'Subcomisario', 'Comisario', 'Subteniente', 'Teniente', 'Capitán', 'Mayor', 'Teniente coronel', 'Coronel', 'General'],
  'Ejército': ['Soldado', 'Cabo', 'Sargento', 'Subteniente', 'Teniente', 'Capitán', 'Mayor', 'Teniente coronel', 'Coronel', 'General'],
  'Armada': ['Marinero', 'Cabo', 'Suboficial', 'Teniente de corbeta', 'Teniente de fragata', 'Teniente de navío', 'Capitán de corbeta', 'Capitán de fragata', 'Capitán de navío', 'Almirante'],
  'Fuerza Aérea': ['Técnico', 'Suboficial', 'Subteniente', 'Teniente', 'Capitán', 'Mayor', 'Teniente coronel', 'Coronel', 'General']
};
const views = {
  summary: ['Resumen', 'Estado del registro y actividad del sistema.'],
  citizens: ['Ciudadanos', 'Administra la información general y el estado registrado de cada ciudadano.'],
  officers: ['Funcionarios', 'Controla el acceso y los permisos del personal registrado.'],
  registry: ['Autorizaciones', 'Autoriza los datos institucionales antes del registro en la app móvil.'],
  audit: ['Auditoría', 'Consulta quién verificó cada documento y cuándo lo hizo.'],
  'citizen-registrations': ['Altas ciudadanas', 'Fecha, documento y administrador que registró cada ficha.'],
  'status-history': ['Historial de estados', 'Consulta los cambios de antecedentes, su autor y fecha.'],
  reports: ['Reportes PDF', 'Genera el listado de ciudadanos según su estado.']
};
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);

async function request(path, options = {}) {
  const headers = { ...options.headers };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (options.body) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`/api${path}`, { ...options, headers });
  } catch {
    throw new Error('No se pudo conectar con CIVIS. Comprueba que el servidor esté activo.');
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 401 && state.token) {
      endSession();
      showLoginError('La sesión expiró. Vuelve a ingresar.');
    }
    throw new Error(error.message || `La solicitud falló (${response.status}).`);
  }
  if (options.pdf) return response.blob();
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function message(text, error = false) {
  $('#global-message').hidden = !text;
  $('#global-message').className = error ? 'error' : '';
  $('#global-message').textContent = text;
}

function showLoginError(text) {
  $('#login-error').hidden = false;
  $('#login-error').textContent = text;
}

function endSession() {
  state.token = '';
  state.actor = null;
  state.rows = [];
  $('#workspace').hidden = true;
  $('#login').hidden = false;
  $('#view-body').replaceChildren();
  $('#editor').close();
  $('#confirmation').close();
  $('#login-form').reset();
}

$('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.submitter;
  button.disabled = true;
  $('#login-error').hidden = true;
  try {
    const values = Object.fromEntries(new FormData(event.target));
    const result = await request('/auth/login', { method: 'POST', body: JSON.stringify(values) });
    state.token = result.token;
    if (result.officer.role !== 'ADMIN') {
      await request('/auth/logout', { method: 'POST' });
      state.token = '';
      throw new Error('El portal requiere un administrador. Los funcionarios pueden usar la app móvil.');
    }
    state.actor = result.officer;
    $('#actor-name').textContent = `${result.officer.firstName} ${result.officer.lastName}`;
    $('#environment').textContent = result.demo ? 'Laboratorio con datos ficticios' : 'Registro interno';
    $('#login').hidden = true;
    $('#workspace').hidden = false;
    event.target.reset();
    await navigate('summary');
  } catch (error) {
    showLoginError(error.message);
  } finally {
    button.disabled = false;
  }
});

$('#logout').addEventListener('click', async () => {
  try {
    await request('/auth/logout', { method: 'POST' });
    endSession();
  } catch (error) {
    message(`No se pudo cerrar la sesión en el servidor. ${error.message} Inténtalo de nuevo.`, true);
  }
});

async function navigate(view) {
  if (state.busy) return;
  state.busy = true;
  const sessionToken = state.token;
  state.view = view;
  const [title, description] = views[view];
  $('#view-title').textContent = title;
  $('#view-description').textContent = description;
  document.querySelectorAll('[data-view]').forEach((button) => button.classList.toggle('active', button.dataset.view === view));
  message('');
  $('#view-body').textContent = 'Cargando información…';
  $('#refresh').disabled = true;
  try {
    if (view === 'summary') {
      const data = await request('/admin/summary');
      if (state.token !== sessionToken) return;
      $('#view-body').innerHTML = `<div class="stats">${[
        [data.citizens, 'Ciudadanos registrados'], [data.required, 'Con estado requerido'],
        [data.officers, 'Funcionarios'], [data.lookups, 'Consultas realizadas']
      ].map(([value, label]) => `<div class="stat"><strong>${value}</strong><span>${label}</span></div>`).join('')}</div>
        <section class="panel"><h2>Registro conectado con la app móvil</h2><p class="muted">Los cambios que guardas aquí se reflejan en las consultas de los funcionarios. Cada verificación conserva el documento consultado, el usuario y la fecha.</p><div class="section-links"><button class="primary" data-go="citizens">Administrar ciudadanos</button><button class="secondary" data-go="registry">Autorizar funcionario</button></div></section>
        <section class="panel"><h2>Preparar una verificación</h2><p class="muted">Registra el ciudadano con sus datos completos. Autoriza al funcionario con su fuerza, rango e identificación institucional. El funcionario recibe su contraseña por correo al registrarse y puede consultar desde Android.</p></section>`;
    } else if (view === 'reports') {
      $('#view-body').innerHTML = `<section class="panel report-options"><h2>Reporte de ciudadanos</h2><p class="muted">El documento incluye identificación, nombres, apellidos y estado. Selecciona el filtro antes de descargar.</p><label>Estado del ciudadano<select id="report-filter"><option value="">Todos los ciudadanos</option><option value="true">Requeridos</option><option value="false">No requeridos</option></select></label><button class="primary" id="download-report">Descargar PDF</button><p id="report-status" class="small" role="status"></p></section>`;
    } else if (view === 'audit' || view === 'status-history' || view === 'citizen-registrations') {
      const result = await request(`/admin/${view}?page=${state.page}&size=20`);
      if (state.token !== sessionToken) return;
      state.rows = result.content;
      state.totalPages = result.totalPages;
      renderRows();
    } else {
      state.rows = await request(`/admin/${view}`);
      if (state.token !== sessionToken) return;
      renderRows();
    }
  } catch (error) {
    if (state.token) {
      $('#view-body').textContent = 'No se pudo cargar la información. Pulsa Actualizar para volver a intentar.';
      message(error.message, true);
    }
  } finally {
    state.busy = false;
    $('#refresh').disabled = false;
  }
}

function badge(text, kind = '') {
  return `<span class="badge ${kind}">${escapeHtml(text)}</span>`;
}

function renderRows() {
  const view = state.view;
  const columns = {
    citizens: ['Ciudadano', 'Documento', 'Estado', 'Acciones'],
    officers: ['Funcionario', 'Fuerza y rango', 'Correo', 'Permisos', 'Estado', 'Acciones'],
    registry: ['Documento', 'Fuerza y rango', 'ID institucional', 'Correo', 'Estado', 'Acciones'],
    audit: ['Fecha y hora', 'Funcionario', 'Documento consultado', 'Resultado'],
    'status-history': ['Fecha y hora', 'Administrador', 'Documento', 'Estado anterior', 'Estado nuevo'],
    'citizen-registrations': ['Fecha y hora', 'Administrador', 'Documento registrado']
  }[view];
  let toolbar = '';
  const paginated = view === 'audit' || view === 'status-history' || view === 'citizen-registrations';
  if (!paginated) {
    toolbar = `<div class="toolbar"><input id="search" aria-label="Buscar por documento o nombre" placeholder="Buscar por documento o nombre…">${view === 'citizens' ? '<select id="status-filter" aria-label="Filtrar por estado"><option value="">Todos los estados</option><option value="true">Requeridos</option><option value="false">No requeridos</option></select>' : ''}${view === 'officers' ? '' : `<button class="primary" id="create-record">${view === 'citizens' ? 'Nuevo ciudadano' : 'Nueva autorización'}</button>`}</div>`;
  }
  $('#view-body').innerHTML = `${toolbar}<div class="table-wrap"><table><thead><tr>${columns.map((name) => `<th scope="col">${name}</th>`).join('')}</tr></thead><tbody id="rows"></tbody></table></div>${!paginated ? `<div class="pagination"><button id="previous-list-page" class="secondary">Anterior</button><span id="list-page-label" role="status"></span><button id="next-list-page" class="secondary">Siguiente</button></div>` : ""}${paginated ? `<div class="pagination"><button id="previous-page" class="secondary" ${state.page === 0 ? 'disabled' : ''}>Anterior</button><span>Página ${state.page + 1} de ${Math.max(1, state.totalPages)}</span><button id="next-page" class="secondary" ${state.page + 1 >= state.totalPages ? 'disabled' : ''}>Siguiente</button></div>` : ''}`;
  filterRows();
}

function filterRows() {
  const query = ($('#search')?.value || '').trim().toLocaleLowerCase('es');
  const status = $('#status-filter')?.value || '';
  const rows = state.rows.filter((row) => {
    const text = `${row.identification} ${row.firstName || ''} ${row.lastName || ''} ${row.email || ''}`.toLocaleLowerCase('es');
    return text.includes(query) && (status === '' || String(row.required) === status);
  });
  const pageSize = 20;
  const pages = Math.max(1, Math.ceil(rows.length / pageSize));
  state.listPage = Math.min(state.listPage, pages - 1);
  const visibleRows = rows.slice(state.listPage * pageSize, (state.listPage + 1) * pageSize);
  const label = $('#list-page-label');
  if (label) {
    label.textContent = `Página ${state.listPage + 1} de ${pages} · ${rows.length} registros`;
    $('#previous-list-page').disabled = state.listPage === 0;
    $('#next-list-page').disabled = state.listPage + 1 >= pages;
  }
  $('#rows').innerHTML = visibleRows.length ? visibleRows.map(renderRow).join('') : `<tr><td class="empty" colspan="${state.view === 'status-history' ? 5 : state.view === 'citizen-registrations' ? 3 : state.view === 'citizens' || state.view === 'audit' ? 4 : 6}">No hay registros para mostrar.</td></tr>`;
}

function renderRow(row) {
  const id = escapeHtml(row.identification);
  const edit = `<button class="table-action" data-edit="${id}">Editar</button>`;
  const person = `${escapeHtml(row.firstName)} ${escapeHtml(row.lastName)}`;
  let cells;
  if (state.view === 'citizens') {
    cells = [`<strong>${person}</strong><span class="small">${escapeHtml(row.birthPlace || 'Lugar de nacimiento por completar')}</span>`, id, badge(row.required ? 'Requerido' : 'No requerido', row.required ? 'red' : ''), `${edit}<button class="table-action delete" data-delete="${id}">Eliminar</button>`];
  } else if (state.view === 'officers') {
    cells = [`<strong>${person}</strong><span class="small">${id}</span>`, `${escapeHtml(row.force)}<span class="small">${escapeHtml(row.rank)}</span>`, escapeHtml(row.email), badge(row.role === 'ADMIN' ? 'Administrador' : 'Funcionario', 'gray'), badge(row.active ? 'Activo' : 'Inactivo', row.active ? '' : 'red'), row.identification === state.actor.identification ? '<span class="small muted">Tu cuenta</span>' : edit];
  } else if (state.view === 'registry') {
    cells = [id, `${escapeHtml(row.force)}<span class="small">${escapeHtml(row.rank)}</span>`, escapeHtml(row.forceId), escapeHtml(row.email), badge(row.active ? 'Autorizado' : 'Suspendido', row.active ? '' : 'red'), edit];
  } else if (state.view === 'citizen-registrations') {
    cells = [escapeHtml(new Date(row.occurredAt).toLocaleString('es-CO')),escapeHtml(row.officerId),escapeHtml(row.citizenId)];
  } else if (state.view === 'status-history') {
    cells = [escapeHtml(new Date(row.occurredAt).toLocaleString('es-CO')), escapeHtml(row.officerId), escapeHtml(row.citizenId), badge(row.previousRequired ? 'Requerido' : 'No requerido', row.previousRequired ? 'red' : ''), badge(row.newRequired ? 'Requerido' : 'No requerido', row.newRequired ? 'red' : '')];
  } else {
    cells = [escapeHtml(new Date(row.occurredAt).toLocaleString('es-CO')), escapeHtml(row.officerId), escapeHtml(row.citizenId), badge(row.outcome.replaceAll('_', ' '), row.outcome === 'REQUERIDO' ? 'red' : '')];
  }
  return `<tr>${cells.map((cell) => `<td>${cell}</td>`).join('')}</tr>`;
}

function input(name, label, value = '', attributes = '') {
  return `<label>${label}<input name="${name}" value="${escapeHtml(value)}" ${attributes}></label>`;
}

function select(name, label, values, selected = '', required = false) {
  return `<label>${label}<select name="${name}" ${required ? 'required' : ''}>${values.map((item) => {
    const [value, text] = Array.isArray(item) ? item : [item, item];
    return `<option value="${escapeHtml(value)}" ${String(value) === String(selected) ? 'selected' : ''}>${escapeHtml(text)}</option>`;
  }).join('')}</select></label>`;
}

let editing = null;
function openEditor(id) {
  editing = id ? state.rows.find((row) => row.identification === id) : null;
  const row = editing || {};
  const view = state.view;
  let fields = '';
  $('#editor-title').textContent = view === 'officers' ? 'Permisos del funcionario' : `${editing ? 'Editar' : 'Crear'} ${view === 'citizens' ? 'ciudadano' : 'autorización'}`;
  if (view === 'officers') {
    fields = `<p class="wide">${escapeHtml(row.firstName)} ${escapeHtml(row.lastName)} · ${escapeHtml(row.identification)}</p>${select('role', 'Perfil', [['OFFICER', 'Funcionario'], ['ADMIN', 'Administrador']], row.role)}${select('active', 'Estado de acceso', [['true', 'Activo'], ['false', 'Inactivo']], row.active)}<p class="small muted wide">Guardar estos permisos revoca las sesiones del funcionario. Deberá iniciar sesión nuevamente.</p>`;
  } else {
    fields = input('identification', 'Identificación', row.identification, `required pattern="[0-9]{6,12}" maxlength="12" inputmode="numeric" placeholder="Ej. 1000000011" ${editing ? 'readonly' : ''}`);
    if (view === 'citizens') {
      const today = new Date().toLocaleDateString('en-CA');
      fields += input('firstName', 'Nombres', row.firstName, 'required maxlength="100" placeholder="Ej. Laura"');
      fields += input('lastName', 'Apellidos', row.lastName, 'required maxlength="100" placeholder="Ej. Cárdenas"');
      fields += input('birthDate', 'Fecha de nacimiento', row.birthDate, `required type="date" max="${today}"`);
      fields += input('birthPlace', 'Lugar de nacimiento', row.birthPlace, 'required maxlength="150" placeholder="Ej. Bogotá"');
      fields += input('issueDate', 'Fecha de expedición', row.issueDate, `required type="date" max="${today}"`);
      fields += input('issuePlace', 'Lugar de expedición', row.issuePlace, 'required maxlength="150" placeholder="Ej. Bogotá"');
      fields += select('rh', 'RH', [['', 'Seleccionar'], '+', '-'], row.rh, true);
      fields += select('bloodGroup', 'Grupo sanguíneo', [['', 'Seleccionar'], 'A', 'B', 'AB', 'O'], row.bloodGroup, true);
      fields += input('heightCm', 'Estatura en centímetros', row.heightCm, 'required type="number" min="40" max="250" placeholder="Ej. 164"');
      if (editing) fields += select('required', 'Estado registrado', [['false', 'No requerido'], ['true', 'Requerido']], row.required ?? false);
      else fields += '<p class="small muted wide">Estado inicial asignado por el servidor: No requerido.</p>';
    } else {
      fields += select('force', 'Fuerza pública', [['', 'Seleccionar'], ...Object.keys(ranks)], row.force, true);
      const rankOptions = ranks[row.force] || [];
      if (row.rank && !rankOptions.includes(row.rank)) rankOptions.push(row.rank);
      fields += select('rank', 'Rango', [['', 'Seleccionar'], ...rankOptions], row.rank, true);
      fields += input('forceId', 'ID institucional', row.forceId, 'required maxlength="100" placeholder="Ej. POL-101"');
      fields += input('email', 'Correo autorizado', row.email, 'required type="email" maxlength="254" placeholder="Ej. funcionario@civis.test"');
      fields += select('active', 'Autorización', [['true', 'Activa'], ['false', 'Suspendida']], row.active ?? true);
    }
  }
  $('#editor-fields').innerHTML = fields;
  $('#editor-error').hidden = true;
  $('#editor').showModal();
}

$('#editor-fields').addEventListener('change', (event) => {
  if (event.target.name === 'force') {
    const target = $('#editor-form select[name="rank"]');
    target.replaceChildren(new Option('Seleccionar', ''), ...(ranks[event.target.value] || []).map((name) => new Option(name, name)));
  }
});

$('#editor-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.submitter;
  button.disabled = true;
  const view = state.view;
  const data = Object.fromEntries(new FormData(event.target));
  Object.keys(data).forEach((key) => { data[key] = data[key].trim(); });
  try {
    if (view === 'citizens') {
      for (const key of ['birthDate', 'birthPlace', 'issueDate', 'issuePlace', 'rh', 'bloodGroup']) data[key] ||= null;
      data.heightCm = data.heightCm ? Number(data.heightCm) : null;
      data.required = data.required === 'true';
      if (!data.birthDate || !data.issueDate || data.issueDate <= data.birthDate) throw new Error('La expedición debe ser posterior al nacimiento.');
    } else {
      data.active = data.active === 'true';
    }
    const path = view === 'officers' ? `/admin/officers/${encodeURIComponent(editing.identification)}` : view === 'citizens' && editing ? `/admin/citizens/${encodeURIComponent(editing.identification)}` : `/admin/${view}`;
    const method = view === 'officers' ? 'PATCH' : view === 'citizens' && editing ? 'PUT' : 'POST';
    await request(path, { method, body: JSON.stringify(data) });
    $('#editor').close();
    await navigate(view);
    message('Cambios guardados correctamente.');
  } catch (error) {
    $('#editor-error').hidden = false;
    $('#editor-error').textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

let deleting = '';
$('#confirm-delete').addEventListener('click', async (event) => {
  event.target.disabled = true;
  try {
    await request(`/admin/citizens/${encodeURIComponent(deleting)}`, { method: 'DELETE' });
    $('#confirmation').close();
    await navigate('citizens');
    message('Ciudadano eliminado del registro.');
  } catch (error) {
    $('#delete-error').hidden = false;
    $('#delete-error').textContent = error.message;
  } finally {
    event.target.disabled = false;
  }
});

async function downloadReport(button) {
  button.disabled = true;
  try {
    const filter = $('#report-filter').value;
    const blob = await request(`/admin/report.pdf${filter ? `?required=${filter}` : ''}`, { pdf: true });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `civis-ciudadanos-${filter === '' ? 'todos' : filter === 'true' ? 'requeridos' : 'no-requeridos'}.pdf`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    $('#report-status').textContent = 'Reporte generado. Revisa las descargas de tu navegador.';
  } catch (error) {
    message(error.message, true);
  } finally {
    button.disabled = false;
  }
}

document.addEventListener('click', (event) => {
  const button = event.target.closest('button');
  if (!button || button.disabled) return;
  if (button.dataset.view || button.dataset.go) { state.page = 0; navigate(button.dataset.view || button.dataset.go); }
  if (button.id === 'refresh') navigate(state.view);
  if (button.id === 'create-record') openEditor();
  if (button.dataset.edit) openEditor(button.dataset.edit);
  if (button.dataset.delete) {
    deleting = button.dataset.delete;
    $('#confirmation-text').textContent = `¿Deseas eliminar al ciudadano con identificación ${deleting}? Esta operación elimina su ficha del registro.`;
    $('#delete-error').hidden = true;
    $('#confirmation').showModal();
  }
  if (button.id === 'close-editor' || button.id === 'cancel-editor') $('#editor').close();
  if (button.id === 'cancel-delete') $('#confirmation').close();
  if (button.id === 'previous-list-page') { state.listPage = Math.max(0, state.listPage - 1); filterRows(); }
  if (button.id === 'next-list-page') { state.listPage++; filterRows(); }
  if (button.id === 'previous-page') { state.page--; navigate(state.view); }
  if (button.id === 'next-page') { state.page++; navigate(state.view); }
  if (button.id === 'download-report') downloadReport(button);
});
document.addEventListener('input', (event) => { if (event.target.id === 'search') { state.listPage = 0; filterRows(); } });
document.addEventListener('change', (event) => { if (event.target.id === 'status-filter') { state.listPage = 0; filterRows(); } });
request('/health').then((health) => { $('#lab-login').hidden = !health.demo; }).catch(() => {});
