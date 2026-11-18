/**
 * Admin Dashboard controller.
 */

document.addEventListener('DOMContentLoaded', async () => {
  await Auth.checkAuth();
  if (!Auth.requireAdmin()) return;

  loadAdminMetrics();
  loadAdminReports();
  loadAdminClaims();
});

function switchAdminTab(tab) {
  document.getElementById('admin-tab-reports').style.display = (tab === 'reports') ? 'block' : 'none';
  document.getElementById('admin-tab-claims').style.display = (tab === 'claims') ? 'block' : 'none';

  document.getElementById('tab-btn-reports').classList.toggle('active', tab === 'reports');
  document.getElementById('tab-btn-claims').classList.toggle('active', tab === 'claims');
}

async function loadAdminMetrics() {
  try {
    const stats = await API.get('/api/admin/dashboard');
    if (stats) {
      document.getElementById('admin-stat-users').textContent = stats.totalUsers;
      document.getElementById('admin-stat-reports').textContent = stats.totalReports;
      document.getElementById('admin-stat-resolved').textContent = stats.resolvedReports;
      document.getElementById('admin-stat-claims').textContent = stats.pendingClaims;
    }
  } catch (err) {
    console.warn('Failed to load admin stats:', err);
  }
}

async function loadAdminReports() {
  const tbody = document.getElementById('admin-reports-table-body');
  try {
    const data = await API.get('/api/admin/reports', { size: 50 });
    const reports = data.content || [];

    if (reports.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">No reports found.</td></tr>';
      return;
    }

    tbody.innerHTML = reports.map(r => `
      <tr style="border-bottom: 1px solid var(--border);">
        <td style="padding: 1rem; font-weight: 600;">#${r.id}</td>
        <td style="padding: 1rem;">
          <span class="badge ${r.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}">${r.reportType}</span>
        </td>
        <td style="padding: 1rem;">
          <a href="/item-details.html?id=${r.id}" style="font-weight: 600;">${escapeHtml(r.title)}</a>
          <span style="display: block; font-size: 0.8rem; color: var(--text-muted);">by ${escapeHtml(r.userName)}</span>
        </td>
        <td style="padding: 1rem;">${escapeHtml(r.categoryName || '-')}</td>
        <td style="padding: 1rem;">${escapeHtml(r.locationName)}</td>
        <td style="padding: 1rem;">
          <span class="badge ${r.status === 'OPEN' ? 'badge-open' : (r.status === 'CLAIM_PENDING' ? 'badge-pending' : 'badge-resolved')}">${r.status.replace('_', ' ')}</span>
        </td>
        <td style="padding: 1rem;">
          <div style="display: flex; gap: 0.4rem;">
            ${r.status !== 'RESOLVED' ? `<button class="btn btn-secondary btn-sm" onclick="moderateReportAction(${r.id}, 'RESOLVED')">Resolve</button>` : ''}
            ${r.status !== 'CANCELLED' ? `<button class="btn btn-danger btn-sm" onclick="moderateReportAction(${r.id}, 'CANCELLED')">Cancel</button>` : ''}
            ${r.status !== 'OPEN' ? `<button class="btn btn-secondary btn-sm" onclick="moderateReportAction(${r.id}, 'OPEN')">Reopen</button>` : ''}
          </div>
        </td>
      </tr>
    `).join('');

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

async function loadAdminClaims() {
  const tbody = document.getElementById('admin-claims-table-body');
  try {
    const data = await API.get('/api/admin/claims', { size: 50 });
    const claims = data.content || [];

    if (claims.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">No recovery claims registered.</td></tr>';
      return;
    }

    tbody.innerHTML = claims.map(c => `
      <tr style="border-bottom: 1px solid var(--border);">
        <td style="padding: 1rem; font-weight: 600;">#${c.id}</td>
        <td style="padding: 1rem;">
          <a href="/item-details.html?id=${c.reportId}" style="font-weight: 600;">${escapeHtml(c.reportTitle)}</a>
          <span style="display: block; font-size: 0.8rem; color: var(--text-muted);">${c.reportType}</span>
        </td>
        <td style="padding: 1rem;">${escapeHtml(c.claimantName)}</td>
        <td style="padding: 1rem;">
          <span class="badge ${c.status === 'PENDING' ? 'badge-pending' : (c.status === 'APPROVED' ? 'badge-open' : 'badge-cancelled')}">${c.status}</span>
        </td>
        <td style="padding: 1rem; max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
          ${escapeHtml(c.claimReason)}
        </td>
        <td style="padding: 1rem; max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-family: monospace; font-size: 0.8rem;">
          ${escapeHtml(c.proofDetails)}
        </td>
        <td style="padding: 1rem;">
          ${c.status === 'PENDING' ? `
            <div style="display: flex; gap: 0.4rem;">
              <button class="btn btn-success btn-sm" onclick="adminReviewClaim(${c.id}, 'APPROVED')">Approve</button>
              <button class="btn btn-danger btn-sm" onclick="adminReviewClaim(${c.id}, 'REJECTED')">Reject</button>
            </div>
          ` : `<span style="font-size: 0.8rem; color: var(--text-muted);">${c.reviewNotes || 'Decided'}</span>`}
        </td>
      </tr>
    `).join('');

  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

function moderateReportAction(reportId, newStatus) {
  ConfirmModal.show(
    `Change Report #${reportId} Status to ${newStatus}`,
    `Are you sure you want to change the status of this report to ${newStatus}?`,
    async () => {
      try {
        await API.patch(`/api/admin/reports/${reportId}/moderation`, {
          status: newStatus,
          moderationNote: `Status changed to ${newStatus} by admin`
        });
        Toast.show(`Report status updated to ${newStatus}`, 'success');
        loadAdminReports();
        loadAdminMetrics();
      } catch (err) {
        Toast.show(err.message, 'error');
      }
    },
    `Set to ${newStatus}`,
    newStatus === 'CANCELLED' ? 'btn-danger' : 'btn-primary'
  );
}

function adminReviewClaim(claimId, newStatus) {
  const isApprove = newStatus === 'APPROVED';
  ConfirmModal.show(
    isApprove ? 'Admin: Approve Claim' : 'Admin: Reject Claim',
    isApprove
      ? 'Approving this claim will mark the item report as RESOLVED and reject other claims.'
      : 'Reject this claim as administrator?',
    async () => {
      try {
        await API.patch(`/api/claims/${claimId}/status`, {
          status: newStatus,
          reviewNotes: isApprove ? 'Approved by administrator' : 'Rejected by administrator'
        });
        Toast.show(`Claim ${newStatus.toLowerCase()} by admin`, 'success');
        loadAdminClaims();
        loadAdminMetrics();
      } catch (err) {
        Toast.show(err.message, 'error');
      }
    },
    `Confirm ${newStatus}`,
    isApprove ? 'btn-success' : 'btn-danger'
  );
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
