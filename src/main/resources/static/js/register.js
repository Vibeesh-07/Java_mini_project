/**
 * Register page controller.
 */

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('register-form');
  if (form) {
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      handleRegisterSubmit(event);
    });
  }
});

async function handleRegisterSubmit(event) {
  event.preventDefault();
  clearErrors();

  const name = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const contactPhone = document.getElementById('reg-phone').value.trim();
  const password = document.getElementById('reg-password').value;
  const confirm = document.getElementById('reg-confirm').value;

  if (!name) return showRegisterFieldError('err-name', 'Enter your full name.');
  if (!email) return showRegisterFieldError('err-email', 'Enter your PSG iTech email address.');
  if (!/^\S+@psgitech\.ac\.in$/i.test(email)) {
    return showRegisterFieldError('err-email', 'Please use your @psgitech.ac.in college email address.');
  }
  if (!password || password.length < 6) return showRegisterFieldError('err-password', 'Use a password with at least 6 characters.');
  if (!confirm) return showRegisterFieldError('err-confirm', 'Please re-enter your password.');

  if (password !== confirm) {
    return showRegisterFieldError('err-confirm', 'Passwords do not match.');
  }

  const submitBtn = document.getElementById('btn-register-submit');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Creating account...';

  try {
    await API.post('/api/auth/register', { name, email, contactPhone: contactPhone || null, password });

    try {
      const login = await API.post('/api/auth/login', { email, password });
      FlashMessage.set(`Account created successfully. Welcome, ${login.user?.name || name}! You are signed in.`, 'success');
      setTimeout(() => {
        window.location.href = '/my-activity.html';
      }, 700);
    } catch (loginErr) {
      FlashMessage.set('Your account was created successfully, but automatic sign-in did not complete. Please sign in with your new account.', 'info');
      setTimeout(() => {
        window.location.href = '/login.html';
      }, 700);
    }

  } catch (err) {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Create Account';

    if (err.data && err.data.fieldErrors) {
      Object.entries(err.data.fieldErrors).forEach(([field, msg]) => {
        const errorEl = document.getElementById(`err-${field}`);
        if (errorEl) {
          errorEl.textContent = msg;
          errorEl.classList.add('visible');
        }
      });
    } else {
      const genErr = document.getElementById('register-general-error');
      genErr.textContent = err.message || 'Registration failed';
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
  const successEl = document.getElementById('register-success');
  if (successEl) {
    successEl.textContent = '';
    successEl.classList.remove('visible');
  }
}

function showRegisterFieldError(id, message) {
  const errorEl = document.getElementById(id);
  errorEl.textContent = message;
  errorEl.classList.add('visible');
  return false;
}

function showRegisterSuccess(message) {
  const successEl = document.getElementById('register-success');
  successEl.textContent = message;
  successEl.classList.add('visible');
}
