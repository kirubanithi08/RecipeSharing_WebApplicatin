const API_URL = 'http://localhost:8080/api';
let currentPage = 0;
const pageSize = 8;
let lastPage = 0;

const LS = {
  title: 'rs_searchTitle',
  category: 'rs_searchCategory',
  page: 'rs_currentPage'
};

// Elements
const searchTitle = document.getElementById('search-title');
const searchCategory = document.getElementById('search-category');
const recipeList = document.getElementById('recipe-list');
const favoritesList = document.getElementById('favorites-list');
const pageInfo = document.getElementById('page-info');
const spinner = document.getElementById('loading-spinner');
const modal = document.getElementById('modal');
const modalBody = document.getElementById('modal-body');
const modalClose = document.getElementById('modal-close');
const toastRoot = document.getElementById('toast-root');
const btnCreate = document.getElementById('btn-create');
const btnLogin = document.getElementById('btn-login');
const btnRegister = document.getElementById('btn-register');
const btnLogout = document.getElementById('btn-logout');
const currentUserSpan = document.getElementById('current-user');

// ====== TOKEN HELPERS ======
function getToken() { return localStorage.getItem('accessToken'); }
function setToken(t) { localStorage.setItem('accessToken', t); }
function clearToken() { localStorage.removeItem('accessToken'); }
function authHeaders() {
  const t = getToken();
  return t ? { 'Authorization': 'Bearer ' + t } : {};
}

// ====== UI UTILITIES ======
function showSpinner() { spinner.classList.remove('hidden'); }
function hideSpinner() { spinner.classList.add('hidden'); }

function showToast(msg, type = 'success') {
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.textContent = msg;
  toastRoot.appendChild(t);
  requestAnimationFrame(() => t.classList.add('show'));
  setTimeout(() => {
    t.classList.remove('show');
    setTimeout(() => t.remove(), 300);
  }, 2800);
}

function openModal(html) {
  modalBody.innerHTML = html;
  modal.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}
function closeModal() {
  modal.classList.add('hidden');
  modalBody.innerHTML = '';
  document.body.style.overflow = '';
}
modalClose.addEventListener('click', closeModal);
modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });

function escapeHtml(s) {
  return s ? String(s).replace(/[&<>"]/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]
  )) : '';
}

// ======== API FETCH WITH REFRESH ========
async function apiFetch(url, options = {}) {
  options.headers = { ...(options.headers || {}), ...authHeaders() };
  let res = await fetch(url, options);
  if (res.status === 401) {
    const refresh = await fetch(`${API_URL}/auth/refresh`, { method: 'POST', credentials: 'include' });
    if (refresh.ok) {
      const data = await refresh.json();
      if (data.accessToken) setToken(data.accessToken);
      options.headers = { ...(options.headers || {}), ...authHeaders() };
      res = await fetch(url, options);
    } else {
      clearToken();
      refreshAuthUI();
    }
  }
  return res;
}

// ======== AUTH ========
function refreshAuthUI() {
  const token = getToken();
  if (!token) {
    currentUserSpan.textContent = '';
    btnLogin.style.display = '';
    btnRegister.style.display = '';
    btnLogout.style.display = 'none';
    btnCreate.style.display = 'none';
    favoritesList.innerHTML = '(login to view)';
  } else {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      currentUserSpan.textContent = 'Hi, ' + (payload.sub || payload.username || 'user');
    } catch { currentUserSpan.textContent = 'Hi, user'; }
    btnLogin.style.display = 'none';
    btnRegister.style.display = 'none';
    btnLogout.style.display = '';
    btnCreate.style.display = '';
    loadFavorites();
  }
}

btnLogout.addEventListener('click', async () => {
  try { await fetch(`${API_URL}/auth/logout`, { method: 'POST', credentials: 'include' }); } catch { }
  clearToken(); refreshAuthUI(); showToast('Logged out');
});

btnLogin.addEventListener('click', () => showAuthModal('login'));
btnRegister.addEventListener('click', () => showAuthModal('register'));

function showAuthModal(type) {
  openModal(`
    <h3>${type === 'login' ? 'Login' : 'Register'}</h3>
    <div class="form-row"><label>Username</label><input id="auth-username"/></div>
    <div class="form-row"><label>Password</label><input id="auth-password" type="password"/></div>
    <div style="text-align:right;margin-top:12px;">
      <button id="auth-submit" class="btn primary">${type === 'login' ? 'Login' : 'Register'}</button>
      <button id="auth-cancel" class="btn ghost">Cancel</button>
    </div>
  `);
  document.getElementById('auth-cancel').onclick = closeModal;
  document.getElementById('auth-submit').onclick = () => submitAuth(type);
}

async function submitAuth(type) {
  const username = document.getElementById('auth-username').value.trim();
  const password = document.getElementById('auth-password').value;
  if (!username || !password) { showToast('All fields required', 'warning'); return; }
  try {
    const res = await fetch(`${API_URL}/auth/${type}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
      credentials: 'include'
    });
    if (!res.ok) throw new Error(`${type} failed`);
    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = { message: text }; }
    if (data.accessToken) setToken(data.accessToken);
    closeModal(); refreshAuthUI(); loadRecipes();
    showToast(data.message || (type === 'login' ? 'Logged in' : 'Registered successfully'), 'success');
  } catch (e) { showToast(e.message, 'error'); }
}

// ======== SEARCH + PAGINATION ========
let searchTimeout;
searchTitle.addEventListener('input', () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => { currentPage = 0; loadRecipes(); }, 500);
});
searchCategory.addEventListener('change', () => { currentPage = 0; loadRecipes(); });
document.getElementById('prev-page').addEventListener('click', () => { if (currentPage > 0) { currentPage--; loadRecipes(); } });
document.getElementById('next-page').addEventListener('click', () => { if (currentPage < lastPage) { currentPage++; loadRecipes(); } });

// ======== RECIPES ========
async function loadRecipes() {
  const title = encodeURIComponent(searchTitle.value || '');
  const cat = encodeURIComponent(searchCategory.value || '');
  const url = `${API_URL}/recipes?page=${currentPage}&size=${pageSize}${title ? `&title=${title}` : ''}${cat ? `&category=${cat}` : ''}`;
  showSpinner();
  try {
    const res = await apiFetch(url);
    if (!res.ok) { recipeList.innerHTML = '<div class="card">Load failed</div>'; return; }
    const page = await res.json();
    const content = page.content || page;
    recipeList.innerHTML = content.map(r => `
      <div class="recipe-card" data-id="${r.id}">
        <h4>${escapeHtml(r.title)}</h4>
        <div class="recipe-meta">${escapeHtml(prettyCategory(r.category))} • by ${escapeHtml(r.authorUsername)}</div>
        <div class="recipe-desc">${escapeHtml((r.description || '').slice(0, 100))}</div>
      </div>
    `).join('') || '<div class="card">No recipes</div>';
    lastPage = (page.totalPages || 1) - 1;
    pageInfo.textContent = `Page ${page.number + 1} of ${page.totalPages}`;
    document.querySelectorAll('.recipe-card').forEach(el => {
      el.addEventListener('click', () => loadRecipeDetail(el.dataset.id));
    });
  } catch { recipeList.innerHTML = '<div class="card">Error loading</div>'; }
  finally { hideSpinner(); }
}

// ======== RECIPE DETAIL ========
async function loadRecipeDetail(id) {
  openModal('<p>Loading...</p>');
  try {
    const res = await apiFetch(`${API_URL}/recipes/${id}`);
    if (!res.ok) { modalBody.innerHTML = '<p>Failed to load</p>'; return; }
    const r = await res.json();
    modalBody.innerHTML = `
      <h2>${escapeHtml(r.title)}</h2>
      <p><strong>Category:</strong> ${escapeHtml(prettyCategory(r.category))}</p>
      <p><strong>By:</strong> ${escapeHtml(r.authorUsername)}</p>
      <p><strong>Description:</strong><br>${escapeHtml(r.description || '')}</p>
      <p><strong>Instructions:</strong><br>${escapeHtml(r.instructions || '')}</p>
      <div style="margin-top:12px;text-align:right;">
        ${getToken() ? `<button class="btn" id="fav-add" data-id="${r.id}">❤ Favorite</button>` : ''}
      </div>
    `;
    const favBtn = document.getElementById('fav-add');
    if (favBtn) {
      const favs = await getFavorites();
      if (favs.some(f => f.id === r.id)) favBtn.classList.add('fav-active');
      favBtn.onclick = () => toggleFavorite(r.id, favBtn);
    }
  } catch { modalBody.innerHTML = '<p>Error loading recipe</p>'; }
}

// ======== FAVORITES ========
async function getFavorites() {
  try {
    const res = await apiFetch(`${API_URL}/favorites`);
    if (!res.ok) return [];
    return await res.json();
  } catch { return []; }
}

async function loadFavorites() {
  try {
    const res = await apiFetch(`${API_URL}/favorites`);
    if (!res.ok) { favoritesList.innerHTML = '(error)'; return; }
    const favs = await res.json();
    favoritesList.innerHTML = favs.length ? favs.map(f => `
      <li>
        <span class="fav-title" data-id="${f.id}" style="cursor:pointer">${escapeHtml(f.title)}</span>
        <button class="fav-remove" data-id="${f.id}">×</button>
      </li>`).join('') : '(none)';
    favoritesList.querySelectorAll('.fav-remove').forEach(btn => {
      btn.onclick = () => removeFavorite(btn.dataset.id);
    });
    favoritesList.querySelectorAll('.fav-title').forEach(span => {
      span.onclick = () => loadRecipeDetail(span.dataset.id);
    });
  } catch { favoritesList.innerHTML = '(error)'; }
}

async function toggleFavorite(id, btn) {
  try {
    const res = await apiFetch(`${API_URL}/favorites/${id}`, { method: 'POST' });
    if (res.ok) {
      btn.classList.toggle('fav-active');
      showToast(btn.classList.contains('fav-active') ? 'Added to favorites' : 'Removed from favorites');
      loadFavorites();
    } else showToast('Failed', 'error');
  } catch { showToast('Failed', 'error'); }
}

async function removeFavorite(id) {
  try {
    const res = await apiFetch(`${API_URL}/favorites/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed');
    showToast('Removed'); loadFavorites();
  } catch { showToast('Remove failed', 'error'); }
}

// ======== CATEGORY HANDLING ========
function prettyCategory(c) {
  return c ? c.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, ch => ch.toUpperCase()) : '';
}

async function loadCategories() {
  try {
    const res = await fetch(`${API_URL}/recipes/categories`);
    if (res.ok) {
      const cats = await res.json();
      searchCategory.innerHTML =
        '<option value="">All categories</option>' +
        cats.map(c => `<option value="${c}">${prettyCategory(c)}</option>`).join('');
      window.allCategories = cats;
    }
  } catch { }
}

// ======== RECIPE FORM ========
btnCreate.addEventListener('click', () => showRecipeForm());
function showRecipeForm(recipe) {
  const catOptions = (window.allCategories || []).map(c => {
    const label = prettyCategory(c);
    return `<option value="${c}" ${recipe?.category === c ? 'selected' : ''}>${label}</option>`;
  }).join('');
  openModal(`
    <h3>${recipe ? 'Edit' : 'Create'} Recipe</h3>
    <div class="form-row"><label>Title</label><input id="r-title" value="${escapeHtml(recipe?.title || '')}"></div>
    <div class="form-row"><label>Category</label>
      <select id="r-category">${catOptions}</select>
    </div>
    <div class="form-row"><label>Description</label><textarea id="r-description">${escapeHtml(recipe?.description || '')}</textarea></div>
    <div class="form-row"><label>Instructions</label><textarea id="r-instructions">${escapeHtml(recipe?.instructions || '')}</textarea></div>
    <div style="text-align:right;margin-top:12px;">
      <button id="r-submit" class="btn primary">${recipe ? 'Update' : 'Create'}</button>
      <button id="r-cancel" class="btn ghost">Cancel</button>
    </div>
  `);
  document.getElementById('r-cancel').onclick = closeModal;
  document.getElementById('r-submit').onclick = () => submitRecipeForm(recipe?.id);
}

async function submitRecipeForm(id) {
  const title = document.getElementById('r-title').value.trim();
  const category = document.getElementById('r-category').value.trim();
  const description = document.getElementById('r-description').value.trim();
  const instructions = document.getElementById('r-instructions').value.trim();
  if (!title || !category) { showToast('Title & Category required', 'warning'); return; }
  const body = JSON.stringify({ title, category, description, instructions });
  const method = id ? 'PUT' : 'POST';
  const url = id ? `${API_URL}/recipes/${id}` : `${API_URL}/recipes`;
  try {
    const res = await apiFetch(url, { method, headers: { 'Content-Type': 'application/json' }, body });
    if (!res.ok) throw new Error('Save failed');
    closeModal(); showToast('Recipe saved'); loadRecipes();
  } catch (e) { showToast(e.message, 'error'); }
}

// ======== INIT ========
(async () => {
  searchTitle.value = localStorage.getItem(LS.title) || '';
  searchCategory.value = localStorage.getItem(LS.category) || '';
  currentPage = parseInt(localStorage.getItem(LS.page) || 0);
  await loadCategories();
  refreshAuthUI();
  await loadRecipes();
})();
window.addEventListener('beforeunload', () => {
  localStorage.setItem(LS.title, searchTitle.value);
  localStorage.setItem(LS.category, searchCategory.value);
  localStorage.setItem(LS.page, currentPage);
});
