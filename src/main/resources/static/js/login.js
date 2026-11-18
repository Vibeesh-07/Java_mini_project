/**
 * Login page controller.
 */

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('login-form');
  if (form) {
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      handleLoginSubmit(event);
    });
  }

  const toggleBtn = document.getElementById('toggle-password');
  const pwdInput = document.getElementById('login-password');

  if (toggleBtn && pwdInput) {
    toggleBtn.addEventListener('click', () => {
      const isPassword = pwdInput.type === 'password';
      pwdInput.type = isPassword ? 'text' : 'password';
      toggleBtn.innerHTML = isPassword
        ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>'
        : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="10" r="3"/></svg>';
    });
  }
});

async function handleLoginSubmit(event) {
  event.preventDefault();
  const errorEl = document.getElementById('login-error');
  const successEl = document.getElementById('login-success');
  errorEl.textContent = '';
  errorEl.classList.remove('visible');
  successEl.textContent = '';
  successEl.classList.remove('visible');
  document.querySelectorAll('#err-login-email, #err-login-password').forEach(el => {
    el.textContent = '';
    el.classList.remove('visible');
  });

  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const submitBtn = document.getElementById('btn-login-submit');

  if (!email) {
    showLoginFieldError('err-login-email', 'Enter your PSG iTech email address.');
    return false;
  }
  if (!/^\S+@\S+\.\S+$/.test(email)) {
    showLoginFieldError('err-login-email', 'Enter a valid email address.');
    return false;
  }
  if (!password) {
    showLoginFieldError('err-login-password', 'Enter your password.');
    return false;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Signing in...';

  try {
    const res = await API.post('/api/auth/login', { email, password });
    FlashMessage.set(`Login successful. Welcome back, ${res.user?.name || 'user'}!`, 'success');

    const params = new URLSearchParams(window.location.search);
    const redirectUrl = params.get('redirect') || (res.user?.role === 'ROLE_ADMIN' ? '/admin.html' : '/my-activity.html');

    setTimeout(() => {
      window.location.href = redirectUrl;
    }, 600);
  } catch (err) {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Sign In';

    errorEl.textContent = err.message || 'Invalid email or password';
    errorEl.classList.add('visible');
  }

  return false;
}

function showLoginFieldError(id, message) {
  const field = document.getElementById(id);
  field.textContent = message;
  field.classList.add('visible');
}
