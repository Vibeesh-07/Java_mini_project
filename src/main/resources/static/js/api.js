/**
 * ReFind Shared API & UI Utility
 * Handles REST requests, CSRF token attachment, session status, and UI toasts/modals.
 */

// Toast notification helper
const Toast = {
  container: null,

  init() {
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.className = 'toast-container';
      document.body.appendChild(this.container);
    }
  },

  show(message, type = 'info', title = '') {
    this.init();
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let iconSvg = '';
    if (type === 'success') {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
    } else if (type === 'error') {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    } else if (type === 'warning') {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
    } else {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
    }

    toast.innerHTML = `
      <div class="toast-icon">${iconSvg}</div>
      <div class="toast-content">
        ${title ? `<div class="toast-title">${title}</div>` : ''}
        <div class="toast-message">${message}</div>
      </div>
      <button class="toast-close" aria-label="Close">&times;</button>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 200);
    });

    this.container.appendChild(toast);

    setTimeout(() => {
      if (toast.parentElement) {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 200);
      }
    }, 4500);
  }
};

// Confirmation Dialog Helper
const ConfirmModal = {
  show(title, message, onConfirm, confirmText = 'Confirm', confirmClass = 'btn-danger') {
    let overlay = document.getElementById('global-confirm-modal');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'global-confirm-modal';
      overlay.className = 'modal-overlay';
      overlay.innerHTML = `
        <div class="modal-box">
          <div class="modal-header">
            <h3 class="modal-title" id="confirm-modal-title">Confirm Action</h3>
            <button class="modal-close" id="confirm-modal-close">&times;</button>
          </div>
          <div class="modal-body" id="confirm-modal-body">
            <p></p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" id="confirm-modal-cancel">Cancel</button>
            <button class="btn ${confirmClass}" id="confirm-modal-action">${confirmText}</button>
          </div>
        </div>
      `;
      document.body.appendChild(overlay);
    }

    const titleEl = overlay.querySelector('#confirm-modal-title');
    const bodyEl = overlay.querySelector('#confirm-modal-body p');
    const actionBtn = overlay.querySelector('#confirm-modal-action');
    const cancelBtn = overlay.querySelector('#confirm-modal-cancel');
    const closeBtn = overlay.querySelector('#confirm-modal-close');

    titleEl.textContent = title;
    bodyEl.textContent = message;
    actionBtn.textContent = confirmText;
    actionBtn.className = `btn ${confirmClass}`;

    const cleanup = () => {
      overlay.classList.remove('active');
    };

    const handleConfirm = () => {
      cleanup();
      if (typeof onConfirm === 'function') onConfirm();
    };

    actionBtn.onclick = handleConfirm;
    cancelBtn.onclick = cleanup;
    closeBtn.onclick = cleanup;

    overlay.classList.add('active');
  }
};

// Keeps important account messages visible after a page redirect.
const FlashMessage = {
  key: 'psgitech.flash-message',

  set(message, type = 'success') {
    try {
      sessionStorage.setItem(this.key, JSON.stringify({ message, type }));
    } catch (e) {
      console.warn('Unable to save page message:', e);
    }
  },

  showPending() {
    let pending;
    try {
      pending = JSON.parse(sessionStorage.getItem(this.key) || 'null');
      sessionStorage.removeItem(this.key);
    } catch (e) {
      return;
    }
    if (!pending?.message) return;

    const wrap = document.createElement('div');
    wrap.className = 'container flash-message-wrap';
    const banner = document.createElement('div');
    banner.className = `flash-message flash-message-${pending.type || 'info'}`;
    banner.setAttribute('role', pending.type === 'error' ? 'alert' : 'status');
    const text = document.createElement('span');
    text.textContent = pending.message;
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'flash-message-close';
    close.setAttribute('aria-label', 'Dismiss message');
    close.textContent = '×';
    close.addEventListener('click', () => wrap.remove());
    banner.append(text, close);
    wrap.appendChild(banner);

    const header = document.querySelector('.navbar');
    if (header) header.insertAdjacentElement('afterend', wrap);
    else document.body.prepend(wrap);
  }
};

// API Client
const API = {
  // Read CSRF token from cookie
  getCsrfTokenFromCookie() {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i].trim();
      if (c.indexOf(name) === 0) {
        return c.substring(name.length, c.length);
      }
    }
    return '';
  },

  async ensureCsrfToken() {
    let token = this.getCsrfTokenFromCookie();
    if (!token) {
      try {
        const res = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
        if (res.ok) {
          const data = await res.json();
          token = data.token;
        }
      } catch (e) {
        console.warn('Failed to prefetch CSRF token:', e);
      }
    }
    return token;
  },

  async request(url, options = {}) {
    const config = {
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json',
        ...(options.headers || {})
      },
      ...options
    };

    const method = (config.method || 'GET').toUpperCase();
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json';
        config.body = JSON.stringify(config.body);
      }

      // Attach CSRF Token
      const token = await this.ensureCsrfToken();
      if (token) {
        config.headers['X-XSRF-TOKEN'] = token;
      }
    }

    try {
      const response = await fetch(url, config);

      if (response.status === 204) {
        return null;
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (!response.ok) {
        const error = new Error(data && data.message ? data.message : `Request failed with status ${response.status}`);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      throw err;
    }
  },

  get(url, params = {}) {
    const queryString = new URLSearchParams(
      Object.entries(params).filter(([_, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    const fullUrl = queryString ? `${url}?${queryString}` : url;
    return this.request(fullUrl, { method: 'GET' });
  },

  post(url, body = {}) {
    return this.request(url, { method: 'POST', body });
  },

  put(url, body = {}) {
    return this.request(url, { method: 'PUT', body });
  },

  patch(url, body = {}) {
    return this.request(url, { method: 'PATCH', body });
  },

  delete(url) {
    return this.request(url, { method: 'DELETE' });
  }
};

// Global Auth State Handler
const Auth = {
  currentUser: null,
  isAuthenticated: false,

  async checkAuth() {
    try {
      const res = await API.get('/api/auth/me');
      if (res && res.authenticated) {
        this.isAuthenticated = true;
        this.currentUser = res.user;
      } else {
        this.isAuthenticated = false;
        this.currentUser = null;
      }
    } catch (e) {
      this.isAuthenticated = false;
      this.currentUser = null;
    }
    this.updateNavbar();
    return this.currentUser;
  },

  updateNavbar() {
    const authActions = document.getElementById('nav-auth-actions');
    const myActivityLink = document.getElementById('nav-my-activity');
    const adminLink = document.getElementById('nav-admin');

    if (!authActions) return;

    if (this.isAuthenticated && this.currentUser) {
      if (myActivityLink) myActivityLink.style.display = 'block';
      if (adminLink) {
        adminLink.style.display = (this.currentUser.role === 'ROLE_ADMIN') ? 'block' : 'none';
      }

      authActions.innerHTML = `
        <span style="font-size: 0.875rem; color: var(--text-muted); font-weight: 500;">
          Hi, <strong style="color: var(--text-main);">${this.escapeHtml(this.currentUser.name)}</strong>
        </span>
        <button id="btn-logout" class="btn btn-secondary btn-sm">Logout</button>
      `;

      document.getElementById('btn-logout').addEventListener('click', async () => {
        try {
          await API.post('/api/auth/logout');
          Toast.show('Logged out successfully', 'info');
          setTimeout(() => { window.location.href = '/index.html'; }, 400);
        } catch (err) {
          Toast.show('Logout failed', 'error');
        }
      });
    } else {
      if (myActivityLink) myActivityLink.style.display = 'none';
      if (adminLink) adminLink.style.display = 'none';

      authActions.innerHTML = `
        <a href="/login.html" class="btn btn-secondary btn-sm">Login</a>
        <a href="/register.html" class="btn btn-primary btn-sm">Register</a>
      `;
    }
  },

  requireAuth(redirectUrl = null) {
    if (!this.isAuthenticated) {
      const target = redirectUrl || window.location.pathname;
      window.location.href = `/login.html?redirect=${encodeURIComponent(target)}`;
      return false;
    }
    return true;
  },

  requireAdmin() {
    if (!this.isAuthenticated || this.currentUser?.role !== 'ROLE_ADMIN') {
      window.location.href = '/index.html';
      return false;
    }
    return true;
  },

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
};

// Auto-initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  FlashMessage.showPending();
  Auth.checkAuth();

  // Mobile menu toggle
  const mobileToggle = document.querySelector('.mobile-toggle');
  const navLinks = document.querySelector('.nav-links');
  if (mobileToggle && navLinks) {
    mobileToggle.addEventListener('click', () => {
      navLinks.classList.toggle('open');
    });
  }
});
