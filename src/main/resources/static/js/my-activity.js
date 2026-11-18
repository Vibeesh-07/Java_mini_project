/**
 * My Activity page controller.
 */

document.addEventListener('DOMContentLoaded', async () => {
  await Auth.checkAuth();
  if (!Auth.requireAuth()) return;

  loadMyReports();
  loadReceivedClaims();
  loadSubmittedClaims();
});

function switchActivityTab(tab) {
  const tabs = ['reports', 'received', 'submitted'];
  tabs.forEach(t => {
    document.getElementById(`tab-content-${t}`).style.display = (t === tab) ? 'block' : 'none';
    const btn = document.getElementById(`tab-btn-${t}`);
    if (t === tab) btn.classList.add('active');
    else btn.classList.remove('active');
  });
}

async function loadMyReports() {
  const container = document.getElementById('my-reports-list');
  const countBadge = document.getElementById('count-reports');

  try {
    const data = await API.get('/api/users/me/reports');
    const reports = data.content || [];
    countBadge.textContent = reports.length;

    if (reports.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          <h3 class="empty-title">No reports submitted yet</h3>
          <p class="empty-subtitle">Report a lost or found belonging to start matching with other reports across campus.</p>
          <div style="display: flex; gap: 0.5rem; justify-content: center;">
            <a href="/report.html?type=LOST" class="btn btn-outline-primary btn-sm">Report Lost Item</a>
            <a href="/report.html?type=FOUND" class="btn btn-primary btn-sm">Report Found Item</a>
          </div>
        </div>
      `;
      return;
    }

    container.innerHTML = reports.map(r => {
      const imgHeader = r.hasImage && r.imageUrl
        ? `<div class="card-thumbnail-wrap"><img src="${r.imageUrl}" alt="${escapeHtml(r.title)}" class="card-thumbnail-img" loading="lazy"></div>`
        : '';

      return `
      <div class="card-item">
        ${imgHeader}
        <div style="padding: 1.25rem; display: flex; flex-direction: column; flex-grow: 1;">
          <div class="card-header-bar">
            <span class="badge ${r.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}">${r.reportType}</span>
            <span class="badge ${r.status === 'OPEN' ? 'badge-open' : (r.status === 'CLAIM_PENDING' ? 'badge-pending' : 'badge-resolved')}">${r.status.replace('_', ' ')}</span>
          </div>

          <h3 class="card-title">${escapeHtml(r.title)}</h3>
          <p class="card-desc">${escapeHtml(r.description)}</p>

          <div class="card-meta">
            <div class="meta-row">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <span>${escapeHtml(r.locationName)}</span>
            </div>
            <div class="meta-row" style="justify-content: space-between;">
              <span>${r.incidentDate || 'Recent'}</span>
              <span style="font-weight: 600; color: var(--primary);">${escapeHtml(r.categoryName || 'General')}</span>
            </div>
          </div>

          <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
            <a href="/item-details.html?id=${r.id}" class="btn btn-secondary btn-full btn-sm">View &amp; Matches &rarr;</a>
          </div>
        </div>
      </div>
    `;
    }).join('');

  } catch (err) {
    container.innerHTML = `<div style="grid-column: 1 / -1; color: var(--danger); text-align: center;">Error loading reports: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadReceivedClaims() {
  const container = document.getElementById('received-claims-list');
  const countBadge = document.getElementById('count-received');

  try {
    const claims = await API.get('/api/users/me/received-claims');
    countBadge.textContent = claims ? claims.length : 0;

    if (!claims || claims.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <h3 class="empty-title">No incoming claims</h3>
          <p class="empty-subtitle">When other students file a claim on items you found, their verification proof will appear here.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = claims.map(c => `
      <div class="card" style="margin-bottom: 1.25rem;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
          <div>
            <span class="badge ${c.status === 'PENDING' ? 'badge-pending' : (c.status === 'APPROVED' ? 'badge-open' : 'badge-cancelled')}">${c.status}</span>
            <h3 style="margin-top: 0.35rem; font-size: 1.15rem;">Claim on: <a href="/item-details.html?id=${c.reportId}">${escapeHtml(c.reportTitle)}</a></h3>
            <span style="font-size: 0.825rem; color: var(--text-muted);">Filed by <strong>${escapeHtml(c.claimantName)}</strong> on ${formatDate(c.createdAt)}</span>
          </div>
        </div>

        <div style="background: var(--bg-alt); padding: 1rem; border-radius: var(--radius-md); margin-bottom: 1rem; font-size: 0.9rem;">
          <div style="font-weight: 700; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.25rem;">CLAIM REASON:</div>
          <p style="color: var(--text-main); margin-bottom: 0.75rem;">${escapeHtml(c.claimReason)}</p>

          <div style="font-weight: 700; font-size: 0.8rem; color: var(--primary); margin-bottom: 0.25rem; display: flex; align-items: center; gap: 0.35rem;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            PRIVATE IDENTIFYING PROOF:
          </div>
          <p style="color: var(--text-main); white-space: pre-wrap;">${escapeHtml(c.proofDetails)}</p>
        </div>

        ${c.canManage ? `
          <div style="display: flex; gap: 0.75rem; justify-content: flex-end;">
            <button class="btn btn-danger btn-sm" onclick="reviewReceivedClaim(${c.id}, 'REJECTED')">Reject Claim</button>
            <button class="btn btn-success btn-sm" onclick="reviewReceivedClaim(${c.id}, 'APPROVED')">Approve Claim &amp; Return Item</button>
          </div>
        ` : (c.reviewNotes ? `<div style="font-size: 0.825rem; color: var(--text-muted);">Review note: ${escapeHtml(c.reviewNotes)}</div>` : '')}
      </div>
    `).join('');

  } catch (err) {
    container.innerHTML = `<div style="color: var(--danger); text-align: center;">Error loading claims: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadSubmittedClaims() {
  const container = document.getElementById('submitted-claims-list');
  const countBadge = document.getElementById('count-submitted');

  try {
    const claims = await API.get('/api/users/me/claims');
    countBadge.textContent = claims ? claims.length : 0;

    if (!claims || claims.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <h3 class="empty-title">No claims submitted</h3>
          <p class="empty-subtitle">You have not submitted recovery claims for any found belongings yet.</p>
          <a href="/browse.html?type=FOUND" class="btn btn-primary btn-sm">Browse Found Items</a>
        </div>
      `;
      return;
    }

    container.innerHTML = claims.map(c => `
      <div class="card" style="margin-bottom: 1.25rem;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
          <div>
            <span class="badge ${c.status === 'PENDING' ? 'badge-pending' : (c.status === 'APPROVED' ? 'badge-open' : 'badge-cancelled')}">${c.status}</span>
            <h3 style="margin-top: 0.35rem; font-size: 1.15rem;">Claimed Item: <a href="/item-details.html?id=${c.reportId}">${escapeHtml(c.reportTitle)}</a></h3>
            <span style="font-size: 0.825rem; color: var(--text-muted);">Reported by ${escapeHtml(c.reportOwnerName)} &bull; Submitted on ${formatDate(c.createdAt)}</span>
          </div>
        </div>

        <div style="background: var(--bg-alt); padding: 1rem; border-radius: var(--radius-md); font-size: 0.9rem;">
          <div style="font-weight: 600; font-size: 0.8rem; color: var(--text-muted);">YOUR CLAIM REASON:</div>
          <p style="color: var(--text-main); margin-bottom: 0.5rem;">${escapeHtml(c.claimReason)}</p>

          ${c.reviewNotes ? `
            <div style="margin-top: 0.75rem; padding-top: 0.75rem; border-top: 1px solid var(--border);">
              <span style="font-size: 0.8rem; font-weight: 700; color: var(--text-main);">Finder Feedback:</span>
              <p style="color: var(--text-muted); margin-top: 0.2rem;">${escapeHtml(c.reviewNotes)}</p>
            </div>
          ` : ''}
        </div>
      </div>
    `).join('');

  } catch (err) {
    container.innerHTML = `<div style="color: var(--danger); text-align: center;">Error loading submitted claims: ${escapeHtml(err.message)}</div>`;
  }
}

function reviewReceivedClaim(claimId, status) {
  const isApprove = status === 'APPROVED';
  ConfirmModal.show(
    isApprove ? 'Approve Claim & Return Item' : 'Reject Claim',
    isApprove 
      ? 'Are you sure you want to approve this claim? The item report will be marked as RESOLVED and all other pending claims will be rejected.'
      : 'Are you sure you want to reject this claim?',
    async () => {
      try {
        await API.patch(`/api/claims/${claimId}/status`, {
          status,
          reviewNotes: isApprove ? 'Proof verified by finder' : 'Insufficient identifying details'
        });
        Toast.show(`Claim successfully ${status.toLowerCase()}`, 'success');
        loadReceivedClaims();
        loadMyReports();
      } catch (err) {
        Toast.show(err.message, 'error');
      }
    },
    isApprove ? 'Approve Claim' : 'Reject Claim',
    isApprove ? 'btn-success' : 'btn-danger'
  );
}

function formatDate(dtStr) {
  if (!dtStr) return '-';
  try {
    const d = new Date(dtStr);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch (e) {
    return dtStr;
  }
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
