import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Lock, LogIn, ArrowLeft, KeyRound, CheckCircle2 } from 'lucide-react';
import { useAuth, getDefaultRouteForRoles } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import api, { extractErrorMessage } from '../../services/api';
import { SchoolLogo } from '../../components/CommonUI';

const DEMO_ACCOUNTS = [
  { role: 'Super Admin', user: 'superadmin', pass: 'Admin@123' },
  { role: 'Principal', user: 'principal', pass: 'Principal@123' },
  { role: 'Admin', user: 'admin', pass: 'Admin@123' },
  { role: 'Teacher', user: 'teacher1', pass: 'Teacher@123' },
  { role: 'Accountant', user: 'accountant', pass: 'Accountant@123' },
  { role: 'Librarian', user: 'librarian', pass: 'Librarian@123' },
  { role: 'Parent (2 children)', user: 'parent1', pass: 'Parent@123' },
  { role: 'Student', user: 'student1', pass: 'Student@123' },
];

export function LoginPage() {
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState('');
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm();

  const onSubmit = async ({ username, password }) => {
    setError('');
    try {
      const user = await login(username, password);
      toast.success(`Welcome back, ${user.fullName}`);
      const target = location.state?.from?.pathname || getDefaultRouteForRoles(user.roles);
      navigate(target, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, 'Invalid username or password'));
    }
  };

  const fillDemo = (u, p) => {
    setValue('username', u);
    setValue('password', p);
  };

  return (
    <div className="min-vh-100 d-flex align-items-center py-5" style={{ background: 'linear-gradient(135deg, #0f294a 0%, #1e3a8a 100%)' }}>
      <div className="container">
        <div className="row justify-content-center g-4 align-items-stretch">
          <div className="col-lg-5">
            <div className="bg-white rounded-4 shadow-lg p-4 p-lg-5 h-100">
              <Link to="/" className="small text-decoration-none text-muted d-inline-flex align-items-center gap-1 mb-3">
                <ArrowLeft size={14} /> Back to School Website
              </Link>
              <div className="mb-3">
                <SchoolLogo variant="main" height={52} />
              </div>
              <h4 className="fw-bold mb-1">ERP Portal Sign In</h4>
              <p className="text-muted small mb-4">Sign in with your assigned school credentials.</p>

              {error && <div className="alert alert-danger small py-2">{error}</div>}

              <form onSubmit={handleSubmit(onSubmit)} noValidate>
                <div className="mb-3">
                  <label htmlFor="login-username" className="form-label small fw-semibold">Username</label>
                  <input
                    id="login-username"
                    className={`form-control ${errors.username ? 'is-invalid' : ''}`}
                    placeholder="Enter username"
                    {...register('username', { required: 'Username is required' })}
                  />
                  {errors.username && <div className="invalid-feedback">{errors.username.message}</div>}
                </div>
                <div className="mb-3">
                  <div className="d-flex justify-content-between align-items-center">
                    <label htmlFor="login-password" className="form-label small fw-semibold">Password</label>
                    <Link to="/forgot-password" className="small text-decoration-none">Forgot password?</Link>
                  </div>
                  <input
                    id="login-password"
                    type="password"
                    className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                    placeholder="Enter password"
                    {...register('password', { required: 'Password is required' })}
                  />
                  {errors.password && <div className="invalid-feedback">{errors.password.message}</div>}
                </div>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="btn tgs-btn-navy w-100 py-2 d-flex align-items-center justify-content-center gap-2"
                >
                  <LogIn size={17} />
                  <span>{isSubmitting ? 'Signing in...' : 'Sign In to Portal'}</span>
                </button>
              </form>
            </div>
          </div>

          <div className="col-lg-5">
            <div className="bg-white bg-opacity-10 text-white rounded-4 p-4 p-lg-5 border border-white border-opacity-25 h-100">
              <div className="d-flex align-items-center gap-2 text-warning fw-semibold small mb-2">
                <Lock size={15} />
                <span>Development / Demo Quick-Fill Credentials</span>
              </div>
              <h5 className="fw-bold mb-2">Role-Based School Portals</h5>
              <p className="small text-white-50 mb-3">
                Click any fictional development account below to test role-based dashboards and permissions:
              </p>
              <div className="row g-2">
                {DEMO_ACCOUNTS.map((acc) => (
                  <div className="col-sm-6" key={acc.user}>
                    <button
                      type="button"
                      onClick={() => fillDemo(acc.user, acc.pass)}
                      className="btn btn-outline-light btn-sm w-100 text-start py-2 px-3"
                    >
                      <div className="fw-semibold">{acc.role}</div>
                      <div className="text-warning" style={{ fontSize: '0.73rem' }}>{acc.user} / {acc.pass}</div>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const { register, handleSubmit, formState: { isSubmitting } } = useForm();

  const onSubmit = async ({ email }) => {
    setError('');
    try {
      await api.post('/auth/forgot-password', { email });
      setSent(true);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center bg-light py-5">
      <div className="container" style={{ maxWidth: 460 }}>
        <div className="tgs-card p-4 p-lg-5">
          <Link to="/login" className="small text-decoration-none text-muted d-inline-flex align-items-center gap-1 mb-3">
            <ArrowLeft size={14} /> Back to Login
          </Link>
          <h4 className="fw-bold mb-1">Forgot Password</h4>
          <p className="text-muted small mb-4">Enter your registered school email address to receive reset instructions.</p>
          {sent ? (
            <div className="alert alert-success small">
              <CheckCircle2 size={16} className="me-1" />
              If an account matches that email, password reset instructions have been issued.
            </div>
          ) : (
            <form onSubmit={handleSubmit(onSubmit)}>
              {error && <div className="alert alert-danger small">{error}</div>}
              <div className="mb-3">
                <label className="form-label small fw-semibold">Registered Email</label>
                <input type="email" required className="form-control" {...register('email', { required: true })} />
              </div>
              <button type="submit" className="btn tgs-btn-navy w-100" disabled={isSubmitting}>
                {isSubmitting ? 'Sending...' : 'Request Reset Token'}
              </button>
            </form>
          )}
          <div className="mt-3 text-center small">
            Already have a reset token? <Link to="/reset-password">Reset Password</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export function ResetPasswordPage() {
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');
  const { register, handleSubmit, formState: { isSubmitting } } = useForm();

  const onSubmit = async (values) => {
    setError('');
    try {
      await api.post('/auth/reset-password', values);
      setDone(true);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center bg-light py-5">
      <div className="container" style={{ maxWidth: 460 }}>
        <div className="tgs-card p-4 p-lg-5">
          <div className="d-flex align-items-center gap-2 text-primary mb-2">
            <KeyRound size={20} />
            <h4 className="fw-bold mb-0">Reset Password</h4>
          </div>
          <p className="text-muted small mb-4">Paste your reset token and choose a new password (min 8 characters).</p>
          {done ? (
            <div className="alert alert-success small">
              Password updated! <Link to="/login" className="fw-semibold">Sign in now</Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit(onSubmit)}>
              {error && <div className="alert alert-danger small">{error}</div>}
              <div className="mb-3">
                <label className="form-label small fw-semibold">Reset Token</label>
                <input required className="form-control" {...register('token', { required: true })} />
              </div>
              <div className="mb-3">
                <label className="form-label small fw-semibold">New Password</label>
                <input type="password" required minLength={8} className="form-control" {...register('newPassword', { required: true })} />
              </div>
              <button type="submit" className="btn tgs-btn-navy w-100" disabled={isSubmitting}>
                {isSubmitting ? 'Updating...' : 'Update Password'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
