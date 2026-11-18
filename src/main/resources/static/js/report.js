/**
 * Report submission & editing controller.
 */

let selectedReportType = 'LOST';
let editReportId = null;
let currentImageData = null;
let removeImageFlag = false;

document.addEventListener('DOMContentLoaded', async () => {
  await Auth.checkAuth();
  if (!Auth.requireAuth()) return;

  await loadCategories();
  setupImageUploader();

  // Set today as maximum date
  const today = new Date().toISOString().split('T')[0];
  const dateInput = document.getElementById('report-date');
  dateInput.max = today;
  dateInput.value = today;

  const params = new URLSearchParams(window.location.search);
  const typeParam = params.get('type');
  const editParam = params.get('edit');

  if (editParam) {
    editReportId = editParam;
    await setupEditMode(editReportId);
  } else if (typeParam && (typeParam === 'LOST' || typeParam === 'FOUND')) {
    selectReportType(typeParam);
  }
});

async function loadCategories() {
  const catSelect = document.getElementById('report-category');
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
    Toast.show('Failed to load categories', 'error');
  }
}

function selectReportType(type) {
  selectedReportType = type;
  const lostTab = document.getElementById('tab-lost');
  const foundTab = document.getElementById('tab-found');
  const foundHint = document.getElementById('found-camera-hint');
  const cameraBtn = document.getElementById('btn-open-camera');

  if (type === 'LOST') {
    lostTab.style.borderColor = 'var(--danger)';
    lostTab.style.background = 'var(--danger-light)';
    lostTab.style.color = 'var(--danger-dark)';

    foundTab.style.borderColor = 'var(--border)';
    foundTab.style.background = 'var(--bg-surface)';
    foundTab.style.color = 'var(--text-muted)';

    if (foundHint) foundHint.style.display = 'none';
    if (cameraBtn) {
      cameraBtn.className = 'btn btn-outline-secondary btn-sm';
    }
  } else {
    foundTab.style.borderColor = 'var(--success)';
    foundTab.style.background = 'var(--success-light)';
    foundTab.style.color = 'var(--success-dark)';

    lostTab.style.borderColor = 'var(--border)';
    lostTab.style.background = 'var(--bg-surface)';
    lostTab.style.color = 'var(--text-muted)';

    if (foundHint) foundHint.style.display = 'inline-flex';
    if (cameraBtn) {
      cameraBtn.className = 'btn btn-outline-success btn-sm';
    }
  }
}

async function setupEditMode(id) {
  document.getElementById('page-heading').textContent = 'Edit Report';
  document.getElementById('page-subheading').textContent = 'Update information for your open report';
  document.getElementById('type-selector-container').style.display = 'none';
  document.getElementById('btn-submit-report').textContent = 'Save Changes';

  try {
    const report = await API.get(`/api/reports/${id}`);
    if (!report) throw new Error('Report not found');

    if (!report.owner && Auth.currentUser?.role !== 'ROLE_ADMIN') {
      Toast.show('You do not have permission to edit this report', 'error');
      setTimeout(() => { window.location.href = `/item-details.html?id=${id}`; }, 1000);
      return;
    }

    selectedReportType = report.reportType;
    document.getElementById('report-title').value = report.title;
    document.getElementById('report-category').value = report.categoryId;
    document.getElementById('report-date').value = report.incidentDate;
    document.getElementById('report-color').value = report.color || '';
    document.getElementById('report-brand').value = report.brand || '';
    document.getElementById('report-zone').value = report.locationZone || 'CAMPUS_GENERAL';
    document.getElementById('report-location').value = report.locationName;
    document.getElementById('report-description').value = report.description;

    if (report.hasImage) {
      const promptEl = document.getElementById('image-upload-prompt');
      const previewContainer = document.getElementById('image-preview-container');
      const previewImg = document.getElementById('image-preview');
      const previewName = document.getElementById('image-preview-name');
      const previewSize = document.getElementById('image-preview-size');

      previewImg.src = report.imageUrl;
      previewName.textContent = 'Existing attached photo';
      previewSize.textContent = 'Saved in system';

      if (promptEl) promptEl.style.display = 'none';
      if (previewContainer) previewContainer.style.display = 'flex';
    }

  } catch (err) {
    Toast.show(err.message, 'error');
  }
}

let activeCameraStream = null;

function triggerFileBrowser(e) {
  if (e) {
    e.stopPropagation();
    e.preventDefault();
  }
  const fileInput = document.getElementById('report-image-input');
  if (fileInput) fileInput.click();
}

function triggerCameraCapture(e) {
  if (e) {
    e.stopPropagation();
    e.preventDefault();
  }

  // Check if getUserMedia is supported
  if (navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function') {
    openCameraModal();
  } else {
    // Fallback directly to native camera file input
    const fallbackInput = document.getElementById('camera-fallback-input');
    if (fallbackInput) fallbackInput.click();
  }
}

async function openCameraModal() {
  const modal = document.getElementById('camera-modal');
  const video = document.getElementById('camera-video-stream');
  const overlay = document.getElementById('camera-loading-overlay');
  if (!modal || !video) return;

  modal.style.display = 'flex';
  if (overlay) {
    overlay.style.display = 'flex';
    overlay.textContent = 'Requesting camera access...';
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: { ideal: 'environment' },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    });
    activeCameraStream = stream;
    video.srcObject = stream;
    video.onloadedmetadata = () => {
      video.play();
      if (overlay) overlay.style.display = 'none';
    };
  } catch (err) {
    console.warn('getUserMedia error:', err);
    closeCameraModal();
    // Fallback to native capture input
    Toast.show('Webcam unavailable. Opening system camera app...', 'info');
    const fallbackInput = document.getElementById('camera-fallback-input');
    if (fallbackInput) fallbackInput.click();
  }
}

function closeCameraModal() {
  const modal = document.getElementById('camera-modal');
  const video = document.getElementById('camera-video-stream');
  if (activeCameraStream) {
    activeCameraStream.getTracks().forEach(track => track.stop());
    activeCameraStream = null;
  }
  if (video) video.srcObject = null;
  if (modal) modal.style.display = 'none';
}

function captureCameraPhoto() {
  const video = document.getElementById('camera-video-stream');
  const canvas = document.getElementById('camera-capture-canvas');
  if (!video || !canvas || !activeCameraStream) return;

  const width = video.videoWidth || 640;
  const height = video.videoHeight || 480;

  canvas.width = width;
  canvas.height = height;

  const ctx = canvas.getContext('2d');
  ctx.drawImage(video, 0, 0, width, height);

  // Convert to JPEG data URL
  const dataUrl = canvas.toDataURL('image/jpeg', 0.85);

  currentImageData = dataUrl;
  removeImageFlag = false;

  const promptEl = document.getElementById('image-upload-prompt');
  const previewContainer = document.getElementById('image-preview-container');
  const previewImg = document.getElementById('image-preview');
  const previewName = document.getElementById('image-preview-name');
  const previewSize = document.getElementById('image-preview-size');

  // Estimate size from Base64
  const estimatedKb = (dataUrl.length * 0.75 / 1024).toFixed(1);

  if (previewImg) previewImg.src = currentImageData;
  if (previewName) previewName.textContent = 'camera_capture.jpg';
  if (previewSize) previewSize.textContent = `${estimatedKb} KB (Camera)`;

  if (promptEl) promptEl.style.display = 'none';
  if (previewContainer) previewContainer.style.display = 'flex';

  closeCameraModal();
  Toast.show('Photo captured and attached!', 'success');
}

function setupImageUploader() {
  const zone = document.getElementById('image-upload-zone');
  const input = document.getElementById('report-image-input');
  const fallback = document.getElementById('camera-fallback-input');
  if (!zone || !input) return;

  zone.addEventListener('dragover', (e) => {
    e.preventDefault();
    zone.classList.add('dragover');
  });

  zone.addEventListener('dragleave', () => {
    zone.classList.remove('dragover');
  });

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('dragover');
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleImageFile(e.dataTransfer.files[0]);
    }
  });

  input.addEventListener('change', () => {
    if (input.files && input.files.length > 0) {
      handleImageFile(input.files[0]);
    }
  });

  if (fallback) {
    fallback.addEventListener('change', () => {
      if (fallback.files && fallback.files.length > 0) {
        handleImageFile(fallback.files[0]);
      }
    });
  }
}

function handleImageFile(file) {
  const errEl = document.getElementById('err-image');
  if (errEl) {
    errEl.textContent = '';
    errEl.classList.remove('visible');
  }

  // Validate MIME type
  if (!file.type.match(/^image\/(jpeg|png|webp|gif)$/i)) {
    if (errEl) {
      errEl.textContent = 'Please choose a valid image file (JPEG, PNG, WebP, GIF)';
      errEl.classList.add('visible');
    }
    Toast.show('Unsupported image format', 'error');
    return;
  }

  // Validate size (max 2MB)
  const maxSize = 2 * 1024 * 1024;
  if (file.size > maxSize) {
    if (errEl) {
      errEl.textContent = 'Image size must not exceed 2MB';
      errEl.classList.add('visible');
    }
    Toast.show('Image exceeds 2MB limit', 'error');
    return;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    currentImageData = e.target.result;
    removeImageFlag = false;

    const promptEl = document.getElementById('image-upload-prompt');
    const previewContainer = document.getElementById('image-preview-container');
    const previewImg = document.getElementById('image-preview');
    const previewName = document.getElementById('image-preview-name');
    const previewSize = document.getElementById('image-preview-size');

    previewImg.src = currentImageData;
    previewName.textContent = file.name;
    previewSize.textContent = `${(file.size / 1024).toFixed(1)} KB (Storage)`;

    if (promptEl) promptEl.style.display = 'none';
    if (previewContainer) previewContainer.style.display = 'flex';
  };
  reader.readAsDataURL(file);
}

function removeAttachedPhoto() {
  currentImageData = null;
  removeImageFlag = true;

  const input = document.getElementById('report-image-input');
  if (input) input.value = '';

  const fallback = document.getElementById('camera-fallback-input');
  if (fallback) fallback.value = '';

  const promptEl = document.getElementById('image-upload-prompt');
  const previewContainer = document.getElementById('image-preview-container');
  const previewImg = document.getElementById('image-preview');

  if (previewImg) previewImg.src = '';
  if (previewContainer) previewContainer.style.display = 'none';
  if (promptEl) promptEl.style.display = 'flex';

  const errEl = document.getElementById('err-image');
  if (errEl) {
    errEl.textContent = '';
    errEl.classList.remove('visible');
  }
}

async function handleReportSubmit(event) {
  event.preventDefault();
  clearErrors();

  const title = document.getElementById('report-title').value.trim();
  const categoryId = document.getElementById('report-category').value;
  const incidentDate = document.getElementById('report-date').value;
  const color = document.getElementById('report-color').value.trim();
  const brand = document.getElementById('report-brand').value.trim();
  const locationZone = document.getElementById('report-zone').value;
  const locationName = document.getElementById('report-location').value.trim();
  const description = document.getElementById('report-description').value.trim();

  const btn = document.getElementById('btn-submit-report');
  btn.disabled = true;
  btn.textContent = editReportId ? 'Saving changes...' : 'Publishing report...';

  const payload = {
    title,
    categoryId: Number(categoryId),
    incidentDate,
    color: color || null,
    brand: brand || null,
    locationZone,
    locationName,
    description,
    imageData: currentImageData || null
  };

  if (editReportId && removeImageFlag) {
    payload.removeImage = true;
  }

  try {
    let result;
    if (editReportId) {
      result = await API.put(`/api/reports/${editReportId}`, payload);
      Toast.show('Report updated successfully!', 'success');
      setTimeout(() => {
        window.location.href = `/item-details.html?id=${editReportId}`;
      }, 700);
    } else {
      payload.reportType = selectedReportType;
      result = await API.post('/api/reports', payload);
      Toast.show('Report published successfully! Matching counter-reports are being calculated.', 'success');
      setTimeout(() => {
        window.location.href = `/item-details.html?id=${result.id}`;
      }, 700);
    }
  } catch (err) {
    btn.disabled = false;
    btn.textContent = editReportId ? 'Save Changes' : 'Publish Report';

    if (err.data && err.data.fieldErrors) {
      Object.entries(err.data.fieldErrors).forEach(([field, msg]) => {
        const errorEl = document.getElementById(`err-${field}`);
        if (errorEl) {
          errorEl.textContent = msg;
          errorEl.classList.add('visible');
        }
      });
    } else {
      const genErr = document.getElementById('form-general-error');
      genErr.textContent = err.message || 'Failed to submit report';
      genErr.classList.add('visible');
    }
  }

  return false;
}

function clearErrors() {
  document.querySelectorAll('.form-error').forEach(el => {
    el.textContent = '';
    el.classList.remove('visible');
  });
}
