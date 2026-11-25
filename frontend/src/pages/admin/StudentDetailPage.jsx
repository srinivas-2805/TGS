import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Download, Upload, MessageSquarePlus, Edit3, Archive } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

const TABS = [
  ['personal', '1. Personal Info'],
  ['parents', '2. Parent Info'],
  ['attendance', '3. Attendance'],
  ['performance', '4. Academic Performance'],
  ['exams', '5. Exam Results'],
  ['homework', '6. Homework'],
  ['fees', '7. Fee Details'],
  ['documents', '8. Documents'],
  ['remarks', '9. Teacher Remarks'],
];

export default function StudentDetailPage() {
  const { id } = useParams();
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');
  const canRemark = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER');

  const [tab, setTab] = useState('personal');
  const [student, setStudent] = useState(null);
  const [attendance, setAttendance] = useState(null);
  const [homework, setHomework] = useState([]);
  const [fees, setFees] = useState([]);
  const [payments, setPayments] = useState([]);
  const [exams, setExams] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [reportCard, setReportCard] = useState(null);
  const [loading, setLoading] = useState(true);

  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [remarkForm, setRemarkForm] = useState({ remarkType: 'ACADEMIC', remarkText: '' });
  const [docUpload, setDocUpload] = useState({ documentType: 'CERTIFICATE', title: '', file: null });
  const [statusModal, setStatusModal] = useState(false);
  const [statusForm, setStatusForm] = useState({ status: 'TRANSFERRED', reason: '' });

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [sRes, aRes, hRes, fRes, pRes, eRes] = await Promise.all([
        api.get(`/students/${id}`),
        api.get(`/attendance/student/${id}`),
        api.get(`/homework/student/${id}`),
        api.get(`/fees/student/${id}`).catch(() => ({ data: { data: [] } })),
        api.get(`/fees/student/${id}/payments`).catch(() => ({ data: { data: [] } })),
        api.get('/examinations'),
      ]);
      const st = sRes.data?.data;
      setStudent(st);
      setEditForm({
        fullName: st.fullName,
        dateOfBirth: st.dateOfBirth,
        gender: st.gender,
        bloodGroup: st.bloodGroup || '',
        address: st.address || '',
        previousSchool: st.previousSchool || '',
        remarks: st.remarks || '',
        rollNumber: st.currentEnrollment?.rollNumber || '',
      });
      setAttendance(aRes.data?.data);
      setHomework(hRes.data?.data || []);
      setFees(fRes.data?.data || []);
      setPayments(pRes.data?.data || []);
      const exList = eRes.data?.data || [];
      setExams(exList);
      if (exList.length > 0 && !selectedExamId) setSelectedExamId(String(exList[0].id));
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    if (id && selectedExamId) {
      api.get(`/examinations/report-card?studentId=${id}&examinationId=${selectedExamId}`)
        .then((res) => setReportCard(res.data?.data || null))
        .catch(() => setReportCard(null));
    }
  }, [id, selectedExamId]);

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/students/${id}`, editForm);
      toast.success('Student profile updated');
      setEditOpen(false);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const handleStatusChange = async (e) => {
    e.preventDefault();
    try {
      await api.patch(`/students/${id}/status`, statusForm);
      toast.success('Student status updated cleanly (historical records preserved)');
      setStatusModal(false);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const handleDocUpload = async (e) => {
    e.preventDefault();
    if (!docUpload.file) return;
    try {
      const fd = new FormData();
      fd.append('documentType', docUpload.documentType);
      if (docUpload.title) fd.append('title', docUpload.title);
      fd.append('file', docUpload.file);
      await api.post(`/students/${id}/documents`, fd);
      toast.success('Document uploaded securely');
      setDocUpload({ documentType: 'CERTIFICATE', title: '', file: null });
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const handleAddRemark = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/students/${id}/remarks`, remarkForm);
      toast.success('Teacher remark added');
      setRemarkForm({ remarkType: 'ACADEMIC', remarkText: '' });
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={7} />;
  if (!student) return null;

  return (
    <div>
      <div className="mb-2">
        <Link to="/portal/students" className="small text-decoration-none d-inline-flex align-items-center gap-1">
          <ArrowLeft size={14} /> Back to Student Directory
        </Link>
      </div>

      <PageHeader
        title={student.fullName}
        subtitle={`Admission No: ${student.admissionNumber} • ${student.currentEnrollment?.className || '-'} - ${student.currentEnrollment?.sectionName || '-'} • Roll No: ${student.currentEnrollment?.rollNumber || '-'}`}
        actions={
          <>
            <button type="button" className="btn btn-outline-primary btn-sm"
              onClick={() => downloadFile(`/students/${id}/id-card/pdf`, `TGS-IDCard-${student.admissionNumber}.pdf`)}>
              <Download size={14} className="me-1" /> ID Card PDF
            </button>
            {isAdmin && (
              <>
                <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setEditOpen(true)}>
                  <Edit3 size={14} className="me-1" /> Edit Profile
                </button>
                <button type="button" className="btn btn-outline-danger btn-sm" onClick={() => setStatusModal(true)}>
                  <Archive size={14} className="me-1" /> Transfer / Archive
                </button>
              </>
            )}
          </>
        }
      />

      {/* 9 Profile Tabs */}
      <ul className="nav nav-tabs mb-3 flex-nowrap overflow-auto">
        {TABS.map(([key, label]) => (
          <li className="nav-item" key={key}>
            <button
              type="button"
              className={`nav-link small fw-semibold text-nowrap ${tab === key ? 'active' : ''}`}
              onClick={() => setTab(key)}
            >
              {label}
            </button>
          </li>
        ))}
      </ul>

      {tab === 'personal' && (
        <div className="tgs-card p-4">
          <div className="row g-3 small">
            <div className="col-md-4"><span className="text-muted d-block">Full Name</span><strong>{student.fullName}</strong></div>
            <div className="col-md-4"><span className="text-muted d-block">Admission Number</span><strong>{student.admissionNumber}</strong></div>
            <div className="col-md-4"><span className="text-muted d-block">Status</span><StatusBadge status={student.status} /></div>
            <div className="col-md-4"><span className="text-muted d-block">Date of Birth</span>{formatDate(student.dateOfBirth)}</div>
            <div className="col-md-4"><span className="text-muted d-block">Gender</span>{student.gender}</div>
            <div className="col-md-4"><span className="text-muted d-block">Blood Group</span>{student.bloodGroup || '-'}</div>
            <div className="col-md-4"><span className="text-muted d-block">Admission Date</span>{formatDate(student.admissionDate)}</div>
            <div className="col-md-4"><span className="text-muted d-block">Portal Username</span>@{student.username}</div>
            <div className="col-md-4"><span className="text-muted d-block">Previous School</span>{student.previousSchool || '-'}</div>
            <div className="col-12"><span className="text-muted d-block">Residential Address</span>{student.address || '-'}</div>
            <div className="col-12"><span className="text-muted d-block">Administrative Notes</span>{student.remarks || '-'}</div>
          </div>
        </div>
      )}

      {tab === 'parents' && (
        <div className="row g-3">
          {(student.parents || []).map((p) => (
            <div className="col-md-6" key={p.parentId}>
              <div className="tgs-card p-4">
                <div className="d-flex justify-content-between mb-2">
                  <h6 className="fw-bold mb-0">{p.fullName}</h6>
                  <span className="badge bg-primary-subtle text-primary">{p.relationship}</span>
                </div>
                <div className="small text-muted">Phone: {p.phone}</div>
                <div className="small text-muted">Email: {p.email}</div>
                <div className="small text-muted">Occupation: {p.occupation || '-'}</div>
                <div className="small text-muted">Address: {p.address || '-'}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'attendance' && (
        <div className="tgs-card p-4">
          <div className="d-flex flex-wrap gap-4 mb-3 small fw-semibold">
            <span>Attendance: <strong className="text-primary">{attendance?.attendancePercentage ?? 0}%</strong></span>
            <span>Present: {attendance?.presentDays ?? 0}</span>
            <span>Absent: {attendance?.absentDays ?? 0}</span>
            <span>Late: {attendance?.lateDays ?? 0}</span>
            <span>Approved Leave: {attendance?.leaveDays ?? 0}</span>
          </div>
          <div className="table-responsive">
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Date</th><th>Session</th><th>Status</th><th>Remarks</th></tr></thead>
              <tbody>
                {(attendance?.records || []).map((r) => (
                  <tr key={r.id}>
                    <td>{formatDate(r.attendanceDate)}</td>
                    <td>{r.session}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td>{r.remarks || r.correctionReason || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'performance' && (
        <div className="tgs-card p-4">
          <h6 className="fw-bold mb-3">Academic Enrollment &amp; Promotion History</h6>
          <div className="table-responsive">
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Academic Year</th><th>Class</th><th>Section</th><th>Roll No</th><th>Status</th><th>Remarks</th></tr></thead>
              <tbody>
                {(student.enrollmentHistory || []).map((e) => (
                  <tr key={e.id}>
                    <td className="fw-semibold">{e.academicYearName}</td>
                    <td>{e.className}</td>
                    <td>{e.sectionName}</td>
                    <td>{e.rollNumber || '-'}</td>
                    <td><StatusBadge status={e.enrollmentStatus} /></td>
                    <td>{e.remarks || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'exams' && (
        <div className="tgs-card p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <select className="form-select form-select-sm" style={{ width: 260 }} value={selectedExamId} onChange={(e) => setSelectedExamId(e.target.value)}>
              {exams.map((ex) => <option key={ex.id} value={ex.id}>{ex.name}</option>)}
            </select>
            {selectedExamId && (
              <button type="button" className="btn tgs-btn-navy btn-sm"
                onClick={() => downloadFile(`/examinations/report-card/pdf?studentId=${id}&examinationId=${selectedExamId}`, `ReportCard-${student.admissionNumber}.pdf`)}>
                <Download size={14} className="me-1" /> Report Card PDF
              </button>
            )}
          </div>
          {!reportCard || (reportCard.subjects || []).length === 0 ? (
            <p className="text-muted small mb-0">No marks recorded for this examination.</p>
          ) : (
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Subject</th><th>Score / Rating</th><th>Grade</th><th>Remarks</th></tr></thead>
              <tbody>
                {reportCard.subjects.map((m) => (
                  <tr key={m.id}>
                    <td className="fw-semibold">{m.subjectName}</td>
                    <td>{reportCard.activityGradedClass ? m.activityRating : `${m.marksObtained ?? 'AB'} / ${m.maxMarks}`}</td>
                    <td>{m.grade}</td>
                    <td>{m.descriptiveRemarks || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'homework' && (
        <div className="tgs-card p-4">
          <h6 className="fw-bold mb-3">Assigned Homework</h6>
          {homework.length === 0 ? <p className="text-muted small mb-0">No homework records.</p> : (
            homework.map((hw) => (
              <div key={hw.id} className="p-3 border rounded mb-2 small">
                <div className="fw-semibold">{hw.title} ({hw.subjectName})</div>
                <div className="text-muted">Due: {formatDate(hw.dueDate)} • {hw.description}</div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'fees' && (
        <div className="tgs-card p-4">
          <h6 className="fw-bold mb-3">Fee Ledger &amp; Receipts</h6>
          <table className="table tgs-table table-sm mb-4">
            <thead><tr><th>Fee</th><th>Net</th><th>Paid</th><th>Balance</th><th>Status</th></tr></thead>
            <tbody>
              {fees.map((f) => (
                <tr key={f.id}>
                  <td>{f.feeName}</td>
                  <td>{formatCurrency(f.netAmount)}</td>
                  <td>{formatCurrency(f.paidAmount)}</td>
                  <td>{formatCurrency(f.balanceAmount)}</td>
                  <td><StatusBadge status={f.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          <h6 className="fw-bold mb-2">Receipts</h6>
          {payments.map((p) => (
            <div key={p.id} className="d-flex justify-content-between align-items-center p-2 border rounded mb-2 small">
              <span>{p.receiptNumber} • {formatCurrency(p.amount)} ({p.paymentMode})</span>
              <button type="button" className="btn btn-sm btn-outline-primary"
                onClick={() => downloadFile(`/fees/payments/${p.id}/receipt`, `${p.receiptNumber}.pdf`)}>
                <Download size={13} /> Receipt PDF
              </button>
            </div>
          ))}
        </div>
      )}

      {tab === 'documents' && (
        <div className="tgs-card p-4">
          {isAdmin && (
            <form onSubmit={handleDocUpload} className="row g-2 align-items-end pb-3 mb-3 border-bottom">
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Type</label>
                <select className="form-select form-select-sm" value={docUpload.documentType}
                  onChange={(e) => setDocUpload({ ...docUpload, documentType: e.target.value })}>
                  <option value="PHOTO">Student Photo</option>
                  <option value="BIRTH_CERTIFICATE">Birth Certificate</option>
                  <option value="TRANSFER_CERTIFICATE">Transfer Certificate</option>
                  <option value="CERTIFICATE">Other Certificate</option>
                </select>
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Title</label>
                <input className="form-control form-control-sm" value={docUpload.title}
                  onChange={(e) => setDocUpload({ ...docUpload, title: e.target.value })} placeholder="Document title" />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">File (PDF / JPG / PNG)</label>
                <input type="file" required className="form-control form-control-sm"
                  onChange={(e) => setDocUpload({ ...docUpload, file: e.target.files?.[0] || null })} />
              </div>
              <div className="col-md-2">
                <button type="submit" className="btn tgs-btn-navy btn-sm w-100"><Upload size={14} className="me-1" />Upload</button>
              </div>
            </form>
          )}
          {(student.documents || []).length === 0 ? <p className="text-muted small mb-0">No documents uploaded.</p> : (
            student.documents.map((d) => (
              <div key={d.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center small">
                <div>
                  <span className="badge bg-light text-dark border me-2">{d.documentType}</span>
                  <strong>{d.title}</strong> ({d.fileName})
                </div>
                <button type="button" className="btn btn-sm btn-outline-primary"
                  onClick={() => downloadFile(`/students/${id}/documents/${d.id}/download`, d.fileName)}>
                  <Download size={14} /> Download
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'remarks' && (
        <div className="tgs-card p-4">
          {canRemark && (
            <form onSubmit={handleAddRemark} className="row g-2 align-items-end pb-3 mb-3 border-bottom">
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Category</label>
                <select className="form-select form-select-sm" value={remarkForm.remarkType}
                  onChange={(e) => setRemarkForm({ ...remarkForm, remarkType: e.target.value })}>
                  <option value="ACADEMIC">Academic</option>
                  <option value="CONDUCT">Conduct</option>
                  <option value="APPRECIATION">Appreciation</option>
                </select>
              </div>
              <div className="col-md-7">
                <label className="form-label small fw-semibold">Remark</label>
                <input required className="form-control form-control-sm" value={remarkForm.remarkText}
                  onChange={(e) => setRemarkForm({ ...remarkForm, remarkText: e.target.value })} placeholder="Add observation or feedback" />
              </div>
              <div className="col-md-2">
                <button type="submit" className="btn tgs-btn-navy btn-sm w-100"><MessageSquarePlus size={14} className="me-1" />Add</button>
              </div>
            </form>
          )}
          {(student.teacherRemarks || []).map((r) => (
            <div key={r.id} className="p-3 border rounded mb-2 small">
              <div className="d-flex justify-content-between">
                <strong>{r.teacherName}</strong>
                <span className="badge bg-light text-dark border">{r.remarkType} • {r.academicYearName}</span>
              </div>
              <div className="text-secondary mt-1">{r.remarkText}</div>
            </div>
          ))}
        </div>
      )}

      <Modal show={editOpen} title="Edit Student Profile" onClose={() => setEditOpen(false)} size="md">
        <form onSubmit={handleSaveEdit} className="row g-3">
          <div className="col-md-8">
            <label className="form-label small fw-semibold">Full Name</label>
            <input required className="form-control form-control-sm" value={editForm.fullName || ''}
              onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })} />
          </div>
          <div className="col-md-4">
            <label className="form-label small fw-semibold">Roll Number</label>
            <input className="form-control form-control-sm" value={editForm.rollNumber || ''}
              onChange={(e) => setEditForm({ ...editForm, rollNumber: e.target.value })} />
          </div>
          <div className="col-md-4">
            <label className="form-label small fw-semibold">DOB</label>
            <input type="date" required className="form-control form-control-sm" value={editForm.dateOfBirth || ''}
              onChange={(e) => setEditForm({ ...editForm, dateOfBirth: e.target.value })} />
          </div>
          <div className="col-md-4">
            <label className="form-label small fw-semibold">Gender</label>
            <select className="form-select form-select-sm" value={editForm.gender || 'MALE'}
              onChange={(e) => setEditForm({ ...editForm, gender: e.target.value })}>
              <option value="MALE">MALE</option>
              <option value="FEMALE">FEMALE</option>
              <option value="OTHER">OTHER</option>
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label small fw-semibold">Blood Group</label>
            <input className="form-control form-control-sm" value={editForm.bloodGroup || ''}
              onChange={(e) => setEditForm({ ...editForm, bloodGroup: e.target.value })} />
          </div>
          <div className="col-12">
            <label className="form-label small fw-semibold">Address</label>
            <input className="form-control form-control-sm" value={editForm.address || ''}
              onChange={(e) => setEditForm({ ...editForm, address: e.target.value })} />
          </div>
          <div className="col-12 text-end">
            <button type="submit" className="btn tgs-btn-navy btn-sm">Save Changes</button>
          </div>
        </form>
      </Modal>

      <Modal show={statusModal} title="Update Student Status (Transfer / Withdrawal / Archive)" onClose={() => setStatusModal(false)} size="sm">
        <form onSubmit={handleStatusChange} className="d-flex flex-column gap-3">
          <div>
            <label className="form-label small fw-semibold">Status</label>
            <select className="form-select form-select-sm" value={statusForm.status}
              onChange={(e) => setStatusForm({ ...statusForm, status: e.target.value })}>
              <option value="TRANSFERRED">TRANSFERRED</option>
              <option value="WITHDRAWN">WITHDRAWN</option>
              <option value="ARCHIVED">ARCHIVED</option>
              <option value="ACTIVE">REACTIVATE (ACTIVE)</option>
            </select>
          </div>
          <div>
            <label className="form-label small fw-semibold">Reason</label>
            <textarea rows={2} required className="form-control form-control-sm" value={statusForm.reason}
              onChange={(e) => setStatusForm({ ...statusForm, reason: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-danger btn-sm">Apply Status</button>
        </form>
      </Modal>
    </div>
  );
}
