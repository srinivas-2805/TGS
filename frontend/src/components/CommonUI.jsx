import React from 'react';
import { AlertTriangle, Inbox, X } from 'lucide-react';
import mainLogo from '../assets/branding/tgs-logo-main.svg';
import compactLogo from '../assets/branding/tgs-logo-compact.svg';
import docLogo from '../assets/branding/tgs-logo-document.svg';

export function SchoolLogo({ variant = 'main', height = 46, className = '' }) {
  const src = variant === 'compact' ? compactLogo : variant === 'document' ? docLogo : mainLogo;
  return <img src={src} alt="Telangana Grammar School" style={{ height, width: 'auto' }} className={className} />;
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
      <div>
        <h4 className="mb-1 fw-bold text-dark">{title}</h4>
        {subtitle && <p className="text-muted small mb-0">{subtitle}</p>}
      </div>
      {actions && <div className="d-flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function StatCard({ title, value, subtitle, icon: Icon, color = 'primary' }) {
  const colorMap = {
    primary: { bg: '#eff6ff', fg: '#1d4ed8' },
    navy: { bg: '#e2e8f0', fg: '#0f294a' },
    gold: { bg: '#fef3c7', fg: '#b45309' },
    green: { bg: '#ecfdf5', fg: '#059669' },
    danger: { bg: '#fef2f2', fg: '#dc2626' },
  };
  const palette = colorMap[color] || colorMap.primary;

  return (
    <div className="tgs-stat-card h-100 d-flex align-items-center justify-content-between gap-3">
      <div>
        <div className="text-muted small fw-semibold mb-1">{title}</div>
        <div className="fs-4 fw-bold text-dark">{value}</div>
        {subtitle && <div className="text-muted" style={{ fontSize: '0.78rem' }}>{subtitle}</div>}
      </div>
      {Icon && (
        <div className="tgs-stat-icon" style={{ backgroundColor: palette.bg, color: palette.fg }}>
          <Icon size={22} />
        </div>
      )}
    </div>
  );
}

export function StatusBadge({ status }) {
  const norm = String(status || '').toUpperCase();
  const map = {
    ACTIVE: 'success',
    PAID: 'success',
    APPROVED: 'success',
    PRESENT: 'success',
    PUBLISHED: 'success',
    PASS: 'success',
    RETURNED: 'success',
    REVIEWED: 'success',
    PARTIAL: 'warning',
    PENDING: 'warning',
    SCHEDULED: 'info',
    ONGOING: 'info',
    SUBMITTED: 'info',
    ISSUED: 'primary',
    LATE: 'warning',
    HALF_DAY: 'warning',
    APPROVED_LEAVE: 'info',
    ABSENT: 'danger',
    OVERDUE: 'danger',
    REJECTED: 'danger',
    NEEDS_IMPROVEMENT: 'danger',
    ARCHIVED: 'secondary',
    WITHDRAWN: 'secondary',
    TRANSFERRED: 'secondary',
    PROMOTED: 'primary',
  };
  const variant = map[norm] || 'secondary';
  return (
    <span className={`badge bg-${variant}-subtle text-${variant} border border-${variant}-subtle px-2 py-1`}>
      {norm.replace(/_/g, ' ')}
    </span>
  );
}

export function LoadingSkeleton({ rows = 4 }) {
  return (
    <div className="tgs-card p-4">
      <div className="tgs-skeleton mb-3" style={{ height: 22, width: '38%' }} />
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="tgs-skeleton mb-2" style={{ height: 18, width: `${92 - i * 8}%` }} />
      ))}
    </div>
  );
}

export function EmptyState({ title = 'No records found', description = 'Try adjusting your filters or create a new record.', action }) {
  return (
    <div className="tgs-card p-5 text-center">
      <div
        className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-circle"
        style={{ width: 56, height: 56, backgroundColor: '#f1f5f9', color: '#64748b' }}
      >
        <Inbox size={26} />
      </div>
      <h6 className="fw-bold mb-1">{title}</h6>
      <p className="text-muted small mb-3">{description}</p>
      {action}
    </div>
  );
}

export function Modal({ show, title, onClose, children, size = 'lg', footer }) {
  if (!show) return null;
  const maxWidthMap = { sm: '420px', md: '560px', lg: '760px', xl: '980px' };
  return (
    <div
      className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center p-3 no-print"
      style={{ backgroundColor: 'rgba(15, 23, 42, 0.55)', zIndex: 1060 }}
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3 shadow-lg w-100 d-flex flex-column"
        style={{ maxWidth: maxWidthMap[size] || '760px', maxHeight: '92vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="d-flex align-items-center justify-content-between px-4 py-3 border-bottom">
          <h5 className="mb-0 fw-bold tgs-sans">{title}</h5>
          <button type="button" className="btn btn-sm btn-light rounded-circle" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>
        <div className="p-4 overflow-auto">{children}</div>
        {footer && <div className="px-4 py-3 border-top bg-light d-flex justify-content-end gap-2 rounded-bottom-3">{footer}</div>}
      </div>
    </div>
  );
}

export function ConfirmDialog({ show, title, message, confirmLabel = 'Confirm', variant = 'danger', onConfirm, onCancel, busy }) {
  return (
    <Modal
      show={show}
      title={title}
      onClose={onCancel}
      size="sm"
      footer={
        <>
          <button type="button" className="btn btn-outline-secondary btn-sm" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button type="button" className={`btn btn-${variant} btn-sm`} onClick={onConfirm} disabled={busy}>
            {busy ? 'Processing...' : confirmLabel}
          </button>
        </>
      }
    >
      <p className="mb-0 text-secondary">{message}</p>
    </Modal>
  );
}

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="container py-5 text-center">
          <div className="tgs-card p-5 mx-auto" style={{ maxWidth: 520 }}>
            <AlertTriangle size={40} className="text-warning mb-3" />
            <h5 className="fw-bold">Unexpected Interface Error</h5>
            <p className="text-muted small mb-3">{this.state.error?.message || 'An unexpected error occurred.'}</p>
            <button
              type="button"
              className="btn tgs-btn-navy btn-sm"
              onClick={() => this.setState({ hasError: false, error: null })}
            >
              Try Again
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
