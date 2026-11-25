import React, { useEffect, useState } from 'react';
import { Plus, IndianRupee, Download, BellRing, Send } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

export default function FeesPage() {
  const toast = useToast();
  const [tab, setTab] = useState('ledger');
  const [structures, setStructures] = useState([]);
  const [studentFees, setStudentFees] = useState([]);
  const [report, setReport] = useState(null);
  const [classes, setClasses] = useState([]);
  const [gateway, setGateway] = useState({ provider: 'MANUAL_ONLY', onlineGatewayConfigured: false });
  const [loading, setLoading] = useState(true);

  const [structOpen, setStructOpen] = useState(false);
  const [sForm, setSForm] = useState({ classId: '', name: '', feeType: 'TUITION', amount: '', dueDate: '' });
  const [payModal, setPayModal] = useState(null);
  const [payForm, setPayForm] = useState({ amount: '', paymentMode: 'CASH', transactionReference: '', remarks: '' });
  const [concModal, setConcModal] = useState(null);
  const [concForm, setConcForm] = useState({ concessionAmount: 0, scholarshipAmount: 0, concessionReason: '' });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [fsR, sfR, repR, cR, gwR] = await Promise.all([
        api.get('/fees/structures'),
        api.get('/fees/student-fees'),
        api.get('/fees/report'),
        api.get('/academics/classes'),
        api.get('/fees/gateway-status'),
      ]);
      setStructures(fsR.data?.data || []);
      setStudentFees(sfR.data?.data || []);
      setReport(repR.data?.data);
      setClasses(cR.data?.data || []);
      setGateway(gwR.data?.data || { provider: 'MANUAL_ONLY', onlineGatewayConfigured: false });
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, []);

  const createStructure = async (e) => {
    e.preventDefault();
    try {
      await api.post('/fees/structures', { ...sForm, classId: Number(sForm.classId), amount: Number(sForm.amount) });
      toast.success('Fee structure created');
      setStructOpen(false);
      setSForm({ classId: '', name: '', feeType: 'TUITION', amount: '', dueDate: '' });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const assignToClass = async (feeStructureId) => {
    try {
      const res = await api.post('/fees/assign', { feeStructureId });
      toast.success(`Assigned fee to ${(res.data?.data || []).length} student(s)`);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const recordPayment = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/fees/payments', {
        studentFeeId: payModal.id,
        amount: Number(payForm.amount),
        paymentMode: payForm.paymentMode,
        transactionReference: payForm.transactionReference,
        remarks: payForm.remarks,
      });
      toast.success(`Payment recorded! Receipt: ${res.data?.data?.receiptNumber}`);
      setPayModal(null);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const saveConcession = async (e) => {
    e.preventDefault();
    try {
      await api.patch(`/fees/student-fees/${concModal.id}/concession`, {
        concessionAmount: Number(concForm.concessionAmount),
        scholarshipAmount: Number(concForm.scholarshipAmount),
        concessionReason: concForm.concessionReason,
      });
      toast.success('Concession / scholarship updated');
      setConcModal(null);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const sendReminders = async () => {
    try {
      const res = await api.post('/fees/reminders');
      toast.success(`Fee reminders sent (${res.data?.data?.notificationsSent || 0} notifications)`);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Fee Management & Receipts"
        subtitle={`Gateway Mode: ${gateway.provider} (${gateway.onlineGatewayConfigured ? 'Online Verified' : 'Manual Cash / UPI / Bank Transfer'})`}
        actions={
          <>
            <button type="button" className="btn btn-outline-warning text-dark btn-sm" onClick={sendReminders}><BellRing size={14} className="me-1" /> Send Fee Reminders</button>
            <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setStructOpen(true)}><Plus size={14} className="me-1" /> New Fee Structure</button>
          </>
        }
      />

      <ul className="nav nav-tabs mb-3">
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'ledger' ? 'active' : ''}`} onClick={() => setTab('ledger')}>Student Fee Ledger ({studentFees.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'structures' ? 'active' : ''}`} onClick={() => setTab('structures')}>Fee Structures ({structures.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'payments' ? 'active' : ''}`} onClick={() => setTab('payments')}>Receipts &amp; Summary</button></li>
      </ul>

      {tab === 'ledger' && (
        <div className="tgs-card overflow-hidden">
          <div className="table-responsive">
            <table className="table tgs-table table-hover mb-0">
              <thead><tr><th>Adm. No</th><th>Student</th><th>Class</th><th>Fee</th><th>Net</th><th>Paid</th><th>Balance</th><th>Status</th><th className="text-end">Actions</th></tr></thead>
              <tbody>
                {studentFees.map((sf) => (
                  <tr key={sf.id}>
                    <td>{sf.admissionNumber}</td>
                    <td className="fw-semibold">{sf.studentName}</td>
                    <td>{sf.className}</td>
                    <td>{sf.feeName}</td>
                    <td>{formatCurrency(sf.netAmount)}</td>
                    <td className="text-success">{formatCurrency(sf.paidAmount)}</td>
                    <td className="fw-bold">{formatCurrency(sf.balanceAmount)}</td>
                    <td><StatusBadge status={sf.status} /></td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <button type="button" className="btn btn-outline-secondary"
                          onClick={() => { setConcModal(sf); setConcForm({ concessionAmount: sf.concessionAmount, scholarshipAmount: sf.scholarshipAmount, concessionReason: sf.concessionReason || '' }); }}>
                          Concession
                        </button>
                        {Number(sf.balanceAmount) > 0 && (
                          <button type="button" className="btn tgs-btn-navy"
                            onClick={() => { setPayModal(sf); setPayForm({ amount: sf.balanceAmount, paymentMode: 'CASH', transactionReference: '', remarks: '' }); }}>
                            <IndianRupee size={13} /> Pay
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'structures' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Class</th><th>Fee Name</th><th>Type</th><th>Amount</th><th>Due Date</th><th className="text-end">Assign</th></tr></thead>
            <tbody>
              {structures.map((fs) => (
                <tr key={fs.id}>
                  <td className="fw-semibold">{fs.className}</td>
                  <td>{fs.name}</td>
                  <td><span className="badge bg-light text-dark border">{fs.feeType}</span></td>
                  <td className="fw-bold">{formatCurrency(fs.amount)}</td>
                  <td>{formatDate(fs.dueDate)}</td>
                  <td className="text-end">
                    <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => assignToClass(fs.id)}>
                      <Send size={13} className="me-1" /> Assign to Class
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'payments' && (
        <div className="tgs-card p-4">
          <div className="d-flex flex-wrap gap-4 mb-4 small fw-semibold">
            <span>Total Collected: <strong className="text-success">{formatCurrency(report?.totalCollected)}</strong></span>
            <span>Pending Balance: <strong className="text-danger">{formatCurrency(report?.totalPending)}</strong></span>
            <span>Paid: {report?.paidCount ?? 0}</span>
            <span>Partial: {report?.partialCount ?? 0}</span>
            <span>Pending: {report?.pendingCount ?? 0}</span>
          </div>
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th>Receipt #</th><th>Student</th><th>Fee</th><th>Mode</th><th>Amount</th><th>Date</th><th>Receipt</th></tr></thead>
            <tbody>
              {(report?.recentPayments || []).map((p) => (
                <tr key={p.id}>
                  <td className="fw-semibold">{p.receiptNumber}</td>
                  <td>{p.studentName}</td>
                  <td>{p.feeName}</td>
                  <td>{p.paymentMode}</td>
                  <td className="fw-bold">{formatCurrency(p.amount)}</td>
                  <td>{formatDate(p.paidAt)}</td>
                  <td>
                    <button type="button" className="btn btn-sm btn-outline-primary"
                      onClick={() => downloadFile(`/fees/payments/${p.id}/receipt`, `${p.receiptNumber}.pdf`)}>
                      <Download size={13} /> PDF
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal show={structOpen} title="Create Fee Structure" onClose={() => setStructOpen(false)} size="md">
        <form onSubmit={createStructure} className="row g-2">
          <div className="col-6"><label className="form-label small">Class *</label>
            <select required className="form-select form-select-sm" value={sForm.classId} onChange={(e) => setSForm({ ...sForm, classId: e.target.value })}>
              <option value="">Select Class</option>{classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Fee Type *</label>
            <select className="form-select form-select-sm" value={sForm.feeType} onChange={(e) => setSForm({ ...sForm, feeType: e.target.value })}>
              {['TUITION', 'ADMISSION', 'EXAMINATION', 'TRANSPORT', 'OTHER'].map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className="col-12"><label className="form-label small">Name *</label><input required className="form-control form-control-sm" value={sForm.name} onChange={(e) => setSForm({ ...sForm, name: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Amount (INR) *</label><input type="number" step="0.01" required className="form-control form-control-sm" value={sForm.amount} onChange={(e) => setSForm({ ...sForm, amount: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Due Date</label><input type="date" className="form-control form-control-sm" value={sForm.dueDate} onChange={(e) => setSForm({ ...sForm, dueDate: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Create Fee Structure</button></div>
        </form>
      </Modal>

      <Modal show={!!payModal} title={`Record Payment — ${payModal?.studentName}`} onClose={() => setPayModal(null)} size="sm">
        <form onSubmit={recordPayment} className="d-flex flex-column gap-2">
          <div className="small text-muted">Remaining Balance: <strong>{formatCurrency(payModal?.balanceAmount)}</strong></div>
          <div><label className="form-label small">Amount (INR) *</label><input type="number" step="0.01" max={payModal?.balanceAmount} required className="form-control form-control-sm" value={payForm.amount} onChange={(e) => setPayForm({ ...payForm, amount: e.target.value })} /></div>
          <div><label className="form-label small">Payment Mode *</label>
            <select className="form-select form-select-sm" value={payForm.paymentMode} onChange={(e) => setPayForm({ ...payForm, paymentMode: e.target.value })}>
              <option value="CASH">CASH</option><option value="UPI">UPI</option><option value="BANK_TRANSFER">BANK_TRANSFER</option>
              {gateway.onlineGatewayConfigured && <option value="ONLINE_GATEWAY">ONLINE_GATEWAY</option>}
            </select>
          </div>
          <div><label className="form-label small">Transaction Ref / UTR</label><input className="form-control form-control-sm" value={payForm.transactionReference} onChange={(e) => setPayForm({ ...payForm, transactionReference: e.target.value })} /></div>
          <button type="submit" className="btn tgs-btn-navy btn-sm mt-2">Record Payment &amp; Generate Receipt</button>
        </form>
      </Modal>

      <Modal show={!!concModal} title={`Concession / Scholarship — ${concModal?.studentName}`} onClose={() => setConcModal(null)} size="sm">
        <form onSubmit={saveConcession} className="d-flex flex-column gap-2">
          <div><label className="form-label small">Concession Amount (INR)</label><input type="number" min={0} className="form-control form-control-sm" value={concForm.concessionAmount} onChange={(e) => setConcForm({ ...concForm, concessionAmount: e.target.value })} /></div>
          <div><label className="form-label small">Scholarship Amount (INR)</label><input type="number" min={0} className="form-control form-control-sm" value={concForm.scholarshipAmount} onChange={(e) => setConcForm({ ...concForm, scholarshipAmount: e.target.value })} /></div>
          <div><label className="form-label small">Reason</label><input className="form-control form-control-sm" value={concForm.concessionReason} onChange={(e) => setConcForm({ ...concForm, concessionReason: e.target.value })} /></div>
          <button type="submit" className="btn tgs-btn-navy btn-sm mt-2">Apply Concession</button>
        </form>
      </Modal>
    </div>
  );
}
