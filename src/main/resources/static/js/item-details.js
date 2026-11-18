/**
 * Item Details controller: Report visualization, automated match engine suggestions,
 * claim submission, owner management, and claim review.
 */

let currentReport = null;
let reportId = null;

document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);
  reportId = params.get('id');

  if (!reportId) {
    window.location.href = '/browse.html';
    return;
  }

  await Auth.checkAuth();
  await loadReportDetails();
  await loadMatches();
});

async function loadReportDetails() {
  const loadingEl = document.getElementById('details-loading');
  const contentEl = document.getElementById('details-content');

  try {
    currentReport = await API.get(`/api/reports/${reportId}`);
    if (!currentReport) {
      throw new Error('Report not found');
    }

    renderReport(currentReport);
    setupActionPanels(currentReport);

    loadingEl.style.display = 'none';
    contentEl.style.display = 'block';

  } catch (err) {
    loadingEl.innerHTML = `
      <div class="empty-state">
        <h3 class="empty-title">Report Not Found</h3>
        <p class="empty-subtitle">${escapeHtml(err.message)}</p>
        <a href="/browse.html" class="btn btn-primary btn-sm">Browse Other Reports</a>
      </div>
    `;
  }
}

function renderReport(report) {
  document.getElementById('detail-title').textContent = report.title;
  document.getElementById('detail-description').textContent = report.description;
  document.getElementById('detail-category').textContent = report.categoryName || 'General';
  document.getElementById('detail-location').textContent = report.locationName;
  document.getElementById('detail-color').textContent = report.color || 'Not specified';
  document.getElementById('detail-brand').textContent = report.brand || 'Not specified';
  document.getElementById('detail-incident-date').textContent = report.incidentDate || 'Unknown';
  document.getElementById('detail-zone').textContent = (report.locationZone || 'CAMPUS_GENERAL').replace('_', ' ');
  document.getElementById('detail-reporter').textContent = report.userName || 'Anonymous User';
  document.getElementById('detail-created-at').textContent = `Reported on ${formatDateTime(report.createdAt)}`;

  const contactCard = document.getElementById('contact-card');
  const contactLoginNote = document.getElementById('contact-login-note');
  if (report.contactAvailable) {
    const values = [report.contactEmail, report.contactPhone].filter(Boolean);
    document.getElementById('detail-contact').textContent = values.length ? values.join('  •  ') : 'The reporter has not added a contact method.';
    contactCard.style.display = 'block';
  } else {
    contactLoginNote.style.display = 'block';
  }

  if (report.handedToOffice) {
    const handover = [report.officeName, report.officeContact, report.officeNotes].filter(Boolean).join('\n');
    document.getElementById('detail-office-handover').textContent = handover || 'Please contact the campus office for collection.';
    document.getElementById('office-handover-card').style.display = 'block';
  }

  // Badges
  const typeBadge = document.getElementById('detail-type-badge');
  typeBadge.className = `badge ${report.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}`;
  typeBadge.textContent = report.reportType;

  const statusBadge = document.getElementById('detail-status-badge');
  const statusClass = report.status === 'OPEN' ? 'badge-open' : (report.status === 'CLAIM_PENDING' ? 'badge-pending' : 'badge-resolved');
  statusBadge.className = `badge ${statusClass}`;
  statusBadge.textContent = report.status.replace('_', ' ');

  // Handle attached photo
  const photoContainer = document.getElementById('detail-photo-container');
  const photoImg = document.getElementById('detail-photo-img');
  if (photoContainer && photoImg) {
    if (report.hasImage && report.imageUrl) {
      photoImg.src = report.imageUrl;
      photoContainer.style.display = 'block';
    } else {
      photoContainer.style.display = 'none';
      photoImg.src = '';
    }
  }
}

function openPhotoLightbox() {
  const photoImg = document.getElementById('detail-photo-img');
  const lightbox = document.getElementById('photo-lightbox');
  const lightboxImg = document.getElementById('lightbox-img');
  if (photoImg && lightbox && lightboxImg) {
    lightboxImg.src = photoImg.src;
    lightbox.classList.add('active');
  }
}

function closePhotoLightbox(e) {
  if (e && e.target && e.target.id === 'lightbox-img') return;
  const lightbox = document.getElementById('photo-lightbox');
  if (lightbox) {
    lightbox.classList.remove('active');
  }
}

function setupActionPanels(report) {
  const ownerBox = document.getElementById('owner-actions-box');
  const claimBox = document.getElementById('claim-submission-box');
  const claimsReviewCard = document.getElementById('claims-review-card');

  const isOwner = report.owner;
  const isAuth = Auth.isAuthenticated;
  const isAdmin = Auth.currentUser?.role === 'ROLE_ADMIN';

  // Owner Actions
  if (isOwner || isAdmin) {
    ownerBox.style.display = 'block';

    const editBtn = document.getElementById('btn-edit-report');
    if (report.status === 'OPEN') {
      editBtn.style.display = 'block';
      editBtn.onclick = () => { window.location.href = `/report.html?edit=${report.id}`; };
    } else {
      editBtn.style.display = 'none';
    }

    const resolveBtn = document.getElementById('btn-resolve-report');
    if (report.status === 'OPEN' || report.status === 'CLAIM_PENDING') {
      resolveBtn.style.display = 'block';
      resolveBtn.onclick = () => {
        ConfirmModal.show(
          'Mark Item as Recovered / Resolved',
          'Are you sure this item has been returned/recovered? This will mark the report as resolved.',
          async () => {
            try {
              await API.patch(`/api/reports/${report.id}/status`, { status: 'RESOLVED', comments: 'Marked resolved by user' });
              Toast.show('Report marked as RESOLVED', 'success');
              setTimeout(() => location.reload(), 500);
            } catch (e) {
              Toast.show(e.message, 'error');
            }
          },
          'Mark as Resolved',
          'btn-success'
        );
      };
    } else {
      resolveBtn.style.display = 'none';
    }

    const cancelBtn = document.getElementById('btn-cancel-report');
    if (report.status === 'OPEN' || report.status === 'CLAIM_PENDING') {
      cancelBtn.style.display = 'block';
      cancelBtn.onclick = () => {
        ConfirmModal.show(
          'Cancel Report',
          'Are you sure you want to cancel this report? It will no longer appear in open listings or match suggestions.',
          async () => {
            try {
              await API.patch(`/api/reports/${report.id}/status`, { status: 'CANCELLED', comments: 'Cancelled by user' });
              Toast.show('Report cancelled', 'info');
              setTimeout(() => location.reload(), 500);
            } catch (e) {
              Toast.show(e.message, 'error');
            }
          },
          'Cancel Report',
          'btn-danger'
        );
      };
    } else {
      cancelBtn.style.display = 'none';
    }

    const handoverBtn = document.getElementById('btn-handover-office');
    if (report.reportType === 'FOUND' && (report.status === 'OPEN' || report.status === 'CLAIM_PENDING')) {
      handoverBtn.style.display = 'block';
      handoverBtn.textContent = report.handedToOffice ? 'Update Campus Office Handover' : 'Hand Over to Campus Office';
      handoverBtn.onclick = () => updateOfficeHandover(report);
    }

    // If report is FOUND, load incoming claims for review
    if (report.reportType === 'FOUND') {
      loadIncomingClaims();
    }
  } else {
    ownerBox.style.display = 'none';
  }

  // Claim Submission (for FOUND reports by non-owners)
  if (!isOwner && report.reportType === 'FOUND' && (report.status === 'OPEN' || report.status === 'CLAIM_PENDING')) {
    claimBox.style.display = 'block';
  } else {
    claimBox.style.display = 'none';
  }
}

async function updateOfficeHandover(report) {
  const officeName = window.prompt('Office name (for example, Student Affairs Office):', report.officeName || '');
  if (officeName === null) return;
  const officeContact = window.prompt('Office contact details (phone, room, or email):', report.officeContact || '');
  if (officeContact === null) return;
  const officeNotes = window.prompt('Optional collection instructions:', report.officeNotes || '');
  if (officeNotes === null) return;
  try {
    await API.patch(`/api/reports/${report.id}/custody`, { handedToOffice: true, officeName, officeContact, officeNotes });
    Toast.show('Office handover details saved.', 'success');
    setTimeout(() => location.reload(), 400);
  } catch (err) {
    Toast.show(err.message, 'error');
  }
}

async function handleClaimSubmit(event) {
  event.preventDefault();
  if (!Auth.requireAuth()) return false;

  const reason = document.getElementById('claim-reason').value.trim();
  const proof = document.getElementById('proof-details').value.trim();
  const btn = document.getElementById('btn-submit-claim');

  btn.disabled = true;
  btn.textContent = 'Submitting claim...';

  try {
    await API.post(`/api/reports/${reportId}/claims`, {
      claimReason: reason,
      proofDetails: proof
    });
    Toast.show('Claim submitted successfully! The finder has been notified to review your proof.', 'success');
    document.getElementById('claim-form').reset();
    setTimeout(() => location.reload(), 1200);
  } catch (err) {
    Toast.show(err.message, 'error');
    btn.disabled = false;
    btn.textContent = 'Submit Claim for Verification';
  }

  return false;
}

async function loadIncomingClaims() {
  const container = document.getElementById('claims-review-card');
  const listEl = document.getElementById('claims-review-list');

  try {
    const claims = await API.get(`/api/reports/${reportId}/claims`);
    if (claims && claims.length > 0) {
      container.style.display = 'block';
      listEl.innerHTML = claims.map(claim => `
        <div style="background: var(--bg-alt); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
            <div>
              <strong>Claimant: ${escapeHtml(claim.claimantName)}</strong>
              <span style="font-size: 0.8rem; color: var(--text-muted); display: block;">Submitted on ${formatDateTime(claim.createdAt)}</span>
            </div>
            <span class="badge ${claim.status === 'PENDING' ? 'badge-pending' : (claim.status === 'APPROVED' ? 'badge-open' : 'badge-cancelled')}">${claim.status}</span>
          </div>

          <div style="margin-bottom: 0.75rem; font-size: 0.9rem;">
            <div style="color: var(--text-muted); font-size: 0.8rem; font-weight: 600;">CLAIM REASON:</div>
            <p style="color: var(--text-main); margin-top: 0.2rem;">${escapeHtml(claim.claimReason)}</p>
          </div>

          <div style="margin-bottom: 1rem; font-size: 0.9rem; background: var(--bg-surface); padding: 0.75rem; border-radius: var(--radius-sm); border: 1px dashed var(--border);">
            <div style="color: var(--primary); font-size: 0.8rem; font-weight: 700; display: flex; align-items: center; gap: 0.35rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              PRIVATE IDENTIFYING PROOF:
            </div>
            <p style="color: var(--text-main); margin-top: 0.25rem; white-space: pre-wrap;">${escapeHtml(claim.proofDetails)}</p>
          </div>

          ${claim.canManage ? `
            <div style="display: flex; gap: 0.75rem; justify-content: flex-end;">
              <button class="btn btn-danger btn-sm" onclick="reviewClaimAction(${claim.id}, 'REJECTED')">Reject Claim</button>
              <button class="btn btn-success btn-sm" onclick="reviewClaimAction(${claim.id}, 'APPROVED')">Approve &amp; Return Item</button>
            </div>
          ` : (claim.reviewNotes ? `<div style="font-size: 0.825rem; color: var(--text-muted);">Note: ${escapeHtml(claim.reviewNotes)}</div>` : '')}
        </div>
      `).join('');
    } else {
      container.style.display = 'none';
    }
  } catch (e) {
    console.warn('Failed to fetch claims for report', e);
  }
}

function reviewClaimAction(claimId, newStatus) {
  const isApprove = newStatus === 'APPROVED';
  ConfirmModal.show(
    isApprove ? 'Approve Claim & Return Item' : 'Reject Claim',
    isApprove 
      ? 'Are you satisfied with the proof provided? Approving this claim will mark the item report as RESOLVED and reject other pending claims.'
      : 'Are you sure you want to reject this claim? The claimant will be notified.',
    async () => {
      try {
        await API.patch(`/api/claims/${claimId}/status`, {
          status: newStatus,
          reviewNotes: isApprove ? 'Proof verified by finder' : 'Insufficient identifying details'
        });
        Toast.show(`Claim successfully ${newStatus.toLowerCase()}`, 'success');
        setTimeout(() => location.reload(), 600);
      } catch (err) {
        Toast.show(err.message, 'error');
      }
    },
    isApprove ? 'Approve Claim' : 'Reject Claim',
    isApprove ? 'btn-success' : 'btn-danger'
  );
}

async function loadMatches() {
  const listEl = document.getElementById('matches-list');
  const countBadge = document.getElementById('matches-count-badge');

  try {
    const matches = await API.get(`/api/reports/${reportId}/matches`);
    if (!matches || matches.length === 0) {
      countBadge.textContent = '0 Matches';
      listEl.innerHTML = `
        <div style="text-align: center; padding: 2rem; color: var(--text-muted); background: var(--bg-alt); border-radius: var(--radius-md);">
          No matching counter-reports found meeting the scoring threshold yet. We continuously evaluate new submissions!
        </div>
      `;
      return;
    }

    countBadge.textContent = `${matches.length} Potential ${matches.length === 1 ? 'Match' : 'Matches'}`;
    listEl.innerHTML = matches.map(match => renderMatchItem(match)).join('');

  } catch (err) {
    listEl.innerHTML = `
      <div style="padding: 1.5rem; color: var(--text-muted); font-size: 0.9rem;">
        Matching engine available for unresolved reports.
      </div>
    `;
  }
}

function renderMatchItem(match) {
  const report = match.report;
  const score = match.matchScore;
  let scoreClass = 'match-high';
  if (score < 50) scoreClass = 'match-low';
  else if (score < 75) scoreClass = 'match-medium';

  const breakdownHtml = (match.criteriaBreakdown || []).map(crit => `
    <li class="criteria-item">
      <span><strong>${escapeHtml(crit.criteriaName)}:</strong> ${escapeHtml(crit.explanation)}</span>
      <span class="points">+${crit.pointsEarned} / ${crit.maxPoints} pts</span>
    </li>
  `).join('');

  return `
    <div style="border: 1px solid #e0e7ff; background: #fafbff; border-radius: var(--radius-lg); padding: 1.25rem; margin-bottom: 1.25rem; box-shadow: var(--shadow-sm);">
      <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
        <div>
          <span class="badge ${report.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}">${report.reportType}</span>
          <h4 style="margin-top: 0.35rem; font-size: 1.1rem;">${escapeHtml(report.title)}</h4>
        </div>
        <div class="match-score-badge ${scoreClass}">
          <span>Match Score:</span>
          <strong style="font-size: 1.1rem;">${score} / 100</strong>
        </div>
      </div>

      <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 0.85rem;">
        ${escapeHtml(report.description)}
      </p>

      <div style="font-size: 0.825rem; color: var(--text-muted); margin-bottom: 0.85rem; display: flex; gap: 1rem; flex-wrap: wrap;">
        <span><strong>Location:</strong> ${escapeHtml(report.locationName)}</span>
        <span><strong>Date:</strong> ${report.incidentDate || '-'}</span>
        ${report.color ? `<span><strong>Color:</strong> ${escapeHtml(report.color)}</span>` : ''}
        ${report.brand ? `<span><strong>Brand:</strong> ${escapeHtml(report.brand)}</span>` : ''}
      </div>

      <!-- Criteria Score Breakdown Accordion -->
      <details style="margin-top: 0.5rem; font-size: 0.85rem;">
        <summary style="cursor: pointer; font-weight: 600; color: var(--primary);">View Criteria Score Breakdown</summary>
        <ul class="criteria-list">
          ${breakdownHtml}
        </ul>
      </details>

      <div style="margin-top: 1rem; display: flex; justify-content: flex-end;">
        <a href="/item-details.html?id=${report.id}" class="btn btn-outline-primary btn-sm">Inspect Matched Report &rarr;</a>
      </div>
    </div>
  `;
}

function formatDateTime(dtStr) {
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
