import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { IndianRupee, Library, KeyRound, CheckCircle2 } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, StatCard, LoadingSkeleton } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

export function AccountantDashboardPage() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/fees/report')
      .then((res) => setReport(res.data?.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSkeleton rows={5} />;
  return (
    <div>
      <PageHeader
        title="Accountant & Finance Dashboard"
        subtitle="Manage class fee structures, collect fees, issue receipts, and send pending reminders."
        actions={<Link to="/portal/fees" className="btn tgs-btn-navy btn-sm">Open Fee Ledger</Link>}
      />
      <div className="row g-3 mb-4">
        <div className="col-md-3"><StatCard title="Total Collected" value={formatCurrency(report?.totalCollected)} icon={IndianRupee} color="green" /></div>
        <div className="col-md-3"><StatCard title="Pending Dues" value={formatCurrency(report?.totalPending)} icon={IndianRupee} color="gold" /></div>
        <div className="col-md-3"><StatCard title="Fully Paid Records" value={report?.paidCount ?? 0} icon={CheckCircle2} color="primary" /></div>
        <div className="col-md-3"><StatCard title="Pending / Partial" value={(report?.pendingCount ?? 0) + (report?.partialCount ?? 0)} icon={IndianRupee} color="danger" /></div>
      </div>
      <div className="tgs-card p-4">
        <h6 className="fw-bold mb-3">Recent Verified Payments</h6>
        <div className="table-responsive">
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th>Receipt #</th><th>Student</th><th>Fee</th><th>Mode</th><th>Amount</th><th>Paid At</th></tr></thead>
            <tbody>
              {(report?.recentPayments || []).map((p) => (
                <tr key={p.id}>
                  <td className="fw-semibold">{p.receiptNumber}</td>
                  <td>{p.studentName} ({p.admissionNumber})</td>
                  <td>{p.feeName}</td>
                  <td>{p.paymentMode}</td>
                  <td className="fw-semibold">{formatCurrency(p.amount)}</td>
                  <td>{formatDate(p.paidAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export function LibrarianDashboardPage() {
  const [dash, setDash] = useState(null);
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api.get('/reports/dashboard'), api.get('/library/issues?status=ISSUED')])
      .then(([dRes, iRes]) => {
        setDash(dRes.data?.data);
        setIssues(iRes.data?.data || []);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSkeleton rows={5} />;
  return (
    <div>
      <PageHeader
        title="Librarian Dashboard"
        subtitle="Manage catalog inventory, issue/return circulation, and overdue late fees."
        actions={<Link to="/portal/library" className="btn tgs-btn-navy btn-sm">Open Library Catalog</Link>}
      />
      <div className="row g-3 mb-4">
        <div className="col-md-6"><StatCard title="Cataloged Book Titles" value={dash?.totalLibraryBooks ?? 0} icon={Library} color="primary" /></div>
        <div className="col-md-6"><StatCard title="Currently Issued Books" value={dash?.issuedBooksCount ?? 0} icon={Library} color="gold" /></div>
      </div>
      <div className="tgs-card p-4">
        <h6 className="fw-bold mb-3">Active Book Issues</h6>
        <div className="table-responsive">
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th>Book</th><th>Borrower</th><th>Issue Date</th><th>Due Date</th></tr></thead>
            <tbody>
              {issues.map((i) => (
                <tr key={i.id}>
                  <td className="fw-semibold">{i.bookTitle}</td>
                  <td>{i.studentName || i.teacherName}</td>
                  <td>{formatDate(i.issueDate)}</td>
                  <td>{formatDate(i.dueDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export function ProfileSettingsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [saving, setSaving] = useState(false);

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.post('/auth/change-password', form);
      toast.success('Password changed successfully');
      setForm({ currentPassword: '', newPassword: '' });
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="My Profile & Security" subtitle="View your account details and update your password." />
      <div className="row g-4">
        <div className="col-lg-5">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Account Details</h6>
            <dl className="row small mb-0">
              <dt className="col-4 text-muted">Full Name</dt><dd className="col-8 fw-semibold">{user?.fullName}</dd>
              <dt className="col-4 text-muted">Username</dt><dd className="col-8">@{user?.username}</dd>
              <dt className="col-4 text-muted">Email</dt><dd className="col-8">{user?.email}</dd>
              <dt className="col-4 text-muted">Phone</dt><dd className="col-8">{user?.phone || '-'}</dd>
              <dt className="col-4 text-muted">Roles</dt><dd className="col-8">{(user?.roles || []).join(', ')}</dd>
            </dl>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3 d-flex align-items-center gap-2"><KeyRound size={17} /> Change Password</h6>
            <form onSubmit={handleChangePassword} className="d-flex flex-column gap-3">
              <div>
                <label className="form-label small fw-semibold">Current Password</label>
                <input type="password" required className="form-control" value={form.currentPassword}
                  onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} />
              </div>
              <div>
                <label className="form-label small fw-semibold">New Password (min 8 chars)</label>
                <input type="password" required minLength={8} className="form-control" value={form.newPassword}
                  onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
              </div>
              <div>
                <button type="submit" className="btn tgs-btn-navy btn-sm" disabled={saving}>
                  {saving ? 'Updating...' : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
