const state = {
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0,
  loading: false,
  chips: { stacks: [], seniorities: [], areas: [], workModes: [] }
};

function formIds() {
  return ['keyword', 'language', 'framework', 'location', 'sort'];
}

function saveFilters() {
  const payload = { chips: state.chips };
  formIds().forEach(id => payload[id] = document.getElementById(id).value || '');
  localStorage.setItem('hermes-search-filters', JSON.stringify(payload));
}

function restoreFilters() {
  const params = new URLSearchParams(window.location.search);
  const raw = params.get('filters') || localStorage.getItem('hermes-search-filters');
  if (!raw) return;

  try {
    const payload = typeof raw === 'string' ? JSON.parse(raw) : raw;
    formIds().forEach(id => {
      if (payload[id] !== undefined) document.getElementById(id).value = payload[id];
    });
    state.chips = payload.chips || state.chips;
    renderAllChips();
  } catch (_) {}
}

function addChip(type, value) {
  const normalized = (value || '').trim().toLowerCase();
  if (!normalized || state.chips[type].includes(normalized)) return;
  state.chips[type].push(normalized);
  renderChips(type);
}

function removeChip(type, value) {
  state.chips[type] = state.chips[type].filter(item => item !== value);
  renderChips(type);
}

function renderChips(type) {
  const idMap = { stacks: 'stackChips', seniorities: 'seniorityChips', areas: 'areaChips', workModes: 'workModeChips' };
  const target = document.getElementById(idMap[type]);
  target.innerHTML = '';

  state.chips[type].forEach(value => {
    const chip = document.createElement('span');
    chip.className = 'chip';
    chip.innerHTML = `${value}<button type="button">×</button>`;
    chip.querySelector('button').addEventListener('click', () => removeChip(type, value));
    target.appendChild(chip);
  });
}

function renderAllChips() {
  renderChips('stacks');
  renderChips('seniorities');
  renderChips('areas');
  renderChips('workModes');
}

function updateLoading(loading) {
  state.loading = loading;
  document.getElementById('searchBtn').disabled = loading;
  document.getElementById('prevBtn').disabled = loading || state.page === 0;
  document.getElementById('nextBtn').disabled = loading || state.page >= Math.max(state.totalPages - 1, 0);
  if (loading) document.getElementById('status').textContent = 'Buscando vagas...';
}

function buildPayload() {
  return {
    keyword: document.getElementById('keyword').value,
    language: document.getElementById('language').value,
    framework: document.getElementById('framework').value,
    location: document.getElementById('location').value,
    stacks: state.chips.stacks,
    seniorities: state.chips.seniorities,
    areas: state.chips.areas,
    workModes: state.chips.workModes
  };
}

function fillSelect(id, options) {
  const select = document.getElementById(id);
  options.forEach(option => {
    const el = document.createElement('option');
    el.value = option;
    el.textContent = option;
    select.appendChild(el);
  });
}

async function loadOptions() {
  try {
    const r = await fetch('/api/v1/search/options');
    if (!r.ok) throw new Error('Falha ao carregar filtros.');

    const data = await r.json();
    fillSelect('seniority', data.seniorities || []);
    fillSelect('area', data.areas || []);
    fillSelect('language', data.languages || []);
    fillSelect('framework', data.frameworks || []);
  } catch (e) {
    document.getElementById('status').innerHTML = '<span class="error">' + e.message + '</span>';
  }
}

function renderPageInfo() {
  const current = state.page + 1;
  const total = Math.max(state.totalPages, 1);
  document.getElementById('pageInfo').textContent = `Página ${current} de ${total} • ${state.totalElements} vagas`;
}

function openDetails(job) {
  document.getElementById('detailTitle').textContent = job.title || 'Sem título';
  document.getElementById('detailMeta').textContent = `${job.empresa || '-'} • ${job.location || '-'} • ${job.seniority || '-'} • ${job.sourceType || '-'} • conf: ${job.confidence ?? '-'}`;
  document.getElementById('detailDescription').textContent = job.description || 'Sem descrição';
  document.getElementById('detailUrl').href = job.url;
  document.getElementById('jobDetails').showModal();
}

function renderResults(data) {
  const results = document.getElementById('results');
  results.innerHTML = '';

  (data.content || []).forEach(job => {
    const div = document.createElement('div');
    div.className = 'job';
    div.innerHTML = `
      <strong>${job.title || 'Sem título'}</strong>
      <div class="meta">${job.empresa || '-'} • ${job.location || '-'} • ${job.seniority || '-'}</div>
      <div class="meta">Stack: ${job.stacks || '-'}</div>
      <div class="actions">
        <a href="${job.url}" target="_blank" rel="noopener noreferrer">Candidatar</a>
        <button type="button" class="secondary details-btn">Detalhes</button>
      </div>
    `;
    div.querySelector('.details-btn').addEventListener('click', () => openDetails(job));
    results.appendChild(div);
  });

  if (!data.content || data.content.length === 0) {
    results.innerHTML = '<p>Nenhuma vaga encontrada com esses filtros.</p>';
  }
}

async function search(page = 0) {
  saveFilters();
  state.page = page;
  updateLoading(true);

  try {
    const payload = buildPayload();
    const sort = document.getElementById('sort').value;
    const r = await fetch(`/api/v1/search/filters?page=${state.page}&size=${state.size}&sort=${encodeURIComponent(sort)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!r.ok) throw new Error('Não foi possível buscar vagas agora.');

    const data = await r.json();
    state.totalPages = data.totalPages || 0;
    state.totalElements = data.totalElements || 0;
    renderResults(data);
    document.getElementById('status').textContent = state.totalElements > 0 ? 'Busca concluída.' : 'Nenhuma vaga encontrada.';
    renderPageInfo();
  } catch (e) {
    document.getElementById('status').innerHTML = '<span class="error">' + e.message + '</span>';
  } finally {
    updateLoading(false);
  }
}

function clearFilters() {
  formIds().forEach(id => document.getElementById(id).value = '');
  state.chips = { stacks: [], seniorities: [], areas: [], workModes: [] };
  renderAllChips();
  localStorage.removeItem('hermes-search-filters');
  state.page = 0;
  state.totalPages = 0;
  state.totalElements = 0;
  document.getElementById('results').innerHTML = '';
  document.getElementById('status').textContent = 'Filtros limpos. Faça uma nova busca.';
  renderPageInfo();
  updateLoading(false);
}

function shareFilters() {
  const payload = { chips: state.chips };
  formIds().forEach(id => payload[id] = document.getElementById(id).value || '');
  const url = new URL(window.location.href);
  url.searchParams.set('filters', JSON.stringify(payload));
  navigator.clipboard.writeText(url.toString());
  document.getElementById('status').textContent = 'Link de busca copiado para a área de transferência.';
}

document.getElementById('searchBtn').addEventListener('click', () => search(0));
document.getElementById('clearBtn').addEventListener('click', clearFilters);
document.getElementById('shareBtn').addEventListener('click', shareFilters);
document.getElementById('prevBtn').addEventListener('click', () => { if (state.page > 0) search(state.page - 1); });
document.getElementById('nextBtn').addEventListener('click', () => { if (state.page < state.totalPages - 1) search(state.page + 1); });

document.getElementById('stack').addEventListener('keydown', event => {
  if (event.key === 'Enter') {
    event.preventDefault();
    addChip('stacks', event.target.value);
    event.target.value = '';
  }
});

document.getElementById('seniority').addEventListener('change', event => { addChip('seniorities', event.target.value); event.target.value = ''; });
document.getElementById('area').addEventListener('change', event => { addChip('areas', event.target.value); event.target.value = ''; });
document.getElementById('workMode').addEventListener('change', event => { addChip('workModes', event.target.value); event.target.value = ''; });

restoreFilters();
loadOptions();
renderPageInfo();
renderAllChips();
updateLoading(false);
