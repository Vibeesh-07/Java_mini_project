/**
 * Home page logic: Loads statistics and recent lost/found items.
 */

function handleHeroSearch(event) {
  event.preventDefault();
  const query = document.getElementById('hero-query').value.trim();
  if (query) {
    window.location.href = `/browse.html?query=${encodeURIComponent(query)}`;
  } else {
    window.location.href = '/browse.html';
  }
  return false;
}

document.addEventListener('DOMContentLoaded', async () => {
  loadStatistics();
  loadRecentItems();
});

async function loadStatistics() {
  try {
    const stats = await API.get('/api/statistics/public');
    if (stats) {
      document.getElementById('stat-lost').textContent = stats.totalLostOpen;
      document.getElementById('stat-found').textContent = stats.totalFoundOpen;
      document.getElementById('stat-resolved').textContent = stats.totalResolved;
      document.getElementById('stat-pending').textContent = stats.totalPendingClaims;
    }
  } catch (err) {
    console.warn('Failed to load stats:', err);
  }
}

async function loadRecentItems() {
  const container = document.getElementById('recent-items-grid');
  try {
    const items = await API.get('/api/reports/recent', { limit: 6 });
    if (!items || items.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: var(--text-muted);">
          No reports currently posted. Be the first to report a lost or found item!
        </div>
      `;
      return;
    }

    container.innerHTML = items.map(item => renderItemCard(item)).join('');
  } catch (err) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: var(--danger);">
        Unable to load recent items. Please refresh the page.
      </div>
    `;
  }
}

function renderItemCard(item) {
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
          <a href="/item-details.html?id=${item.id}" class="btn btn-secondary btn-full btn-sm">View Details &rarr;</a>
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
