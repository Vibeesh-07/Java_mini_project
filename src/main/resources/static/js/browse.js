/**
 * Browse page controller: Search, filters, sort, backend pagination.
 */

let currentPage = 0;
let totalPages = 1;
const pageSize = 12;

document.addEventListener('DOMContentLoaded', async () => {
  await loadCategories();
  initFromUrlParams();
  loadReports();
});

async function loadCategories() {
  const catSelect = document.getElementById('filter-category');
  try {
    const categories = await API.get('/api/categories');
    if (categories && Array.isArray(categories)) {
      categories.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat.id;
        opt.textContent = cat.name;
        catSelect.appendChild(opt);
      });
    }
  } catch (err) {
    console.warn('Failed to load categories', err);
  }
}

function initFromUrlParams() {
  const params = new URLSearchParams(window.location.search);
  const q = params.get('query') || params.get('q');
  const type = params.get('type');
  const cat = params.get('category');
  const zone = params.get('zone');

  if (q) document.getElementById('search-input').value = q;
  if (type) document.getElementById('filter-type').value = type;
  if (cat) document.getElementById('filter-category').value = cat;
  if (zone) document.getElementById('filter-zone').value = zone;
}

function handleSearchSubmit(event) {
  event.preventDefault();
  currentPage = 0;
  loadReports();
  return false;
}

function resetFilters() {
  document.getElementById('search-input').value = '';
  document.getElementById('filter-type').value = '';
  document.getElementById('filter-category').value = '';
  document.getElementById('filter-zone').value = '';
  document.getElementById('sort-select').value = 'createdAt,desc';
  currentPage = 0;
  loadReports();
}

function handleSortChange() {
  currentPage = 0;
  loadReports();
}

function changePage(delta) {
  const target = currentPage + delta;
  if (target >= 0 && target < totalPages) {
    currentPage = target;
    loadReports();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

async function loadReports() {
  const grid = document.getElementById('items-grid');
  const countEl = document.getElementById('results-count');
  const paginationContainer = document.getElementById('pagination-container');
  const pageIndicator = document.getElementById('page-indicator');
  const prevBtn = document.getElementById('btn-prev-page');
  const nextBtn = document.getElementById('btn-next-page');

  grid.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 3rem;">Loading items...</div>';

  const query = document.getElementById('search-input').value.trim();
  const reportType = document.getElementById('filter-type').value;
  const categoryId = document.getElementById('filter-category').value;
  const locationZone = document.getElementById('filter-zone').value;

  const [sortBy, direction] = document.getElementById('sort-select').value.split(',');

  const params = {
    page: currentPage,
    size: pageSize,
    sortBy: sortBy || 'createdAt',
    direction: direction || 'desc'
  };

  if (query) params.query = query;
  if (reportType) params.reportType = reportType;
  if (categoryId) params.categoryId = categoryId;
  if (locationZone) params.locationZone = locationZone;

  try {
    const data = await API.get('/api/reports', params);
    totalPages = data.totalPages || 1;
    countEl.textContent = `Showing ${data.totalElements} ${data.totalElements === 1 ? 'report' : 'reports'}`;

    if (!data.content || data.content.length === 0) {
      grid.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          <div class="empty-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </div>
          <h3 class="empty-title">No matching reports found</h3>
          <p class="empty-subtitle">Try adjusting your search terms, removing filters, or report your lost/found item directly.</p>
          <button class="btn btn-primary btn-sm" onclick="resetFilters()">Reset All Filters</button>
        </div>
      `;
      paginationContainer.style.display = 'none';
      return;
    }

    grid.innerHTML = data.content.map(item => renderBrowseCard(item)).join('');

    // Update pagination
    if (totalPages > 1) {
      paginationContainer.style.display = 'flex';
      pageIndicator.textContent = `Page ${currentPage + 1} of ${totalPages}`;
      prevBtn.disabled = data.first;
      nextBtn.disabled = data.last;
    } else {
      paginationContainer.style.display = 'none';
    }

  } catch (err) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: var(--danger);">
        Error loading reports: ${escapeHtml(err.message)}
      </div>
    `;
    countEl.textContent = 'Error loading results';
    paginationContainer.style.display = 'none';
  }
}

function renderBrowseCard(item) {
  const typeBadgeClass = item.reportType === 'LOST' ? 'badge-lost' : 'badge-found';
  const typeLabel = item.reportType === 'LOST' ? 'LOST' : 'FOUND';
  const statusBadgeClass = item.status === 'OPEN' ? 'badge-open' : (item.status === 'CLAIM_PENDING' ? 'badge-pending' : 'badge-resolved');
  const imageHeader = item.hasImage && item.imageUrl
    ? `<div class="card-thumbnail-wrap"><img src="${item.imageUrl}" alt="${escapeHtml(item.title)}" class="card-thumbnail-img" loading="lazy"></div>`
    : '';

  return `
    <div class="card-item">
      ${imageHeader}
      <div style="padding: 1.25rem; display: flex; flex-direction: column; flex-grow: 1;">
        <div class="card-header-bar">
          <span class="badge ${typeBadgeClass}">${typeLabel}</span>
          <span class="badge ${statusBadgeClass}">${item.status.replace('_', ' ')}</span>
        </div>
        
        <h3 class="card-title">${escapeHtml(item.title)}</h3>
        <p class="card-desc">${escapeHtml(item.description)}</p>

        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.85rem;">
          ${item.color ? `<span style="font-size: 0.75rem; background: var(--bg-alt); padding: 0.2rem 0.5rem; border-radius: 4px; border: 1px solid var(--border);">Color: <strong>${escapeHtml(item.color)}</strong></span>` : ''}
          ${item.brand ? `<span style="font-size: 0.75rem; background: var(--bg-alt); padding: 0.2rem 0.5rem; border-radius: 4px; border: 1px solid var(--border);">Brand: <strong>${escapeHtml(item.brand)}</strong></span>` : ''}
        </div>

        <div class="card-meta">
          <div class="meta-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            <span>${escapeHtml(item.locationName)}</span>
          </div>
          <div class="meta-row" style="justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 0.35rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              <span>${item.incidentDate || 'Recent'}</span>
            </div>
            <span style="font-weight: 600; color: var(--primary);">${escapeHtml(item.categoryName || 'General')}</span>
          </div>
        </div>

        <div style="margin-top: 1rem;">
          <a href="/item-details.html?id=${item.id}" class="btn btn-secondary btn-full btn-sm">View Full Details &rarr;</a>
        </div>
      </div>
    </div>
  `;
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
