import React, { useEffect, useState } from 'react';
import { CalendarCheck, Edit2, Check, X } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

export default function AttendanceAndLeavePage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');

  const [tab, setTab] = useState('mark');
  const [sections, setSections] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [sectionId, setSectionId] = useState('');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [session, setSession] = useState('FULL_DAY');
  const [students, setStudents] = useState([]);
  const [entries, setEntries] = useState({});
  const [savedRecords, setSavedRecords] = useState([]);
  const [reportRows, setReportRows] = useState([]);
  const [reportRange, setReportRange] = useState({
    from: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10),
    to: new Date().toISOString().slice(0, 10),
  });
  const [leaves, setLeaves] = useState([]);
  const [teacherAttEntries, setTeacherAttEntries] = useState({});
  const [teacherSaved, setTeacherSaved] = useState([]);
  const [correctModal, setCorrectModal] = useState(null);
  const [correctForm, setCorrectForm] = useState({ status: 'PRESENT', reason: '' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/academics/sections'),
      api.get('/leaves'),
      isAdmin ? api.get('/teachers?size=100') : Promise.resolve({ data: { data: { content: [] } } }),
    ]).then(([sRes, lRes, tRes]) => {
      const sList = sRes.data?.data || [];
      setSections(sList);
      if (sList.length > 0) setSectionId(String(sList[0].id));
      setLeaves(lRes.data?.data || []);
      const tList = tRes.data?.data?.content || [];
      setTeachers(tList);
      const tMap = {};
      tList.forEach((t) => { tMap[t.id] = 'PRESENT'; });
      setTeacherAttEntries(tMap);
    }).catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  const loadSectionData = async () => {
    if (!sectionId || !date) return;
    try {
      const [stuRes, attRes] = await Promise.all([
        api.get(`/students?sectionId=${sectionId}&status=ACTIVE&size=150`),
        api.get(`/attendance/section?sectionId=${sectionId}&date=${date}&session=${session}`).catch(() => ({ data: { data: [] } })),
      ]);
      const stList = stuRes.data?.data?.content || [];
      setStudents(stList);
      const saved = attRes.data?.data || [];
      setSavedRecords(saved);
      const map = {};
      stList.forEach((s) => { map[s.id] = 'PRESENT'; });
      setEntries(map);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  useEffect(() => {
    if (sectionId) loadSectionData();
  }, [sectionId, date, session]);

  const saveStudentAttendance = async () => {
    try {
      const records = students.map((s) => ({ studentId: s.id, status: entries[s.id] || 'PRESENT', remarks: '' }));
      await api.post('/attendance/students', { sectionId: Number(sectionId), attendanceDate: date, session, records });
      toast.success('Attendance saved! Absent parent alerts dispatched automatically.');
      loadSectionData();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const correctRecord = async (e) => {
    e.preventDefault();
    try {
      await api.patch(`/attendance/students/${correctModal.id}/correct`, correctForm);
      toast.success('Attendance corrected');
      setCorrectModal(null);
      loadSectionData();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const loadReport = async () => {
    try {
      const res = await api.get(`/attendance/report?from=${reportRange.from}&to=${reportRange.to}${sectionId ? `&sectionId=${sectionId}` : ''}`);
      setReportRows(res.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const loadTeacherAttendance = async () => {
    try {
      const res = await api.get(`/attendance/teachers?date=${date}`);
      setTeacherSaved(res.data?.data || []);
    } catch { setTeacherSaved([]); }
  };

  const saveTeacherAttendance = async () => {
    try {
      const records = teachers.map((t) => ({ teacherId: t.id, status: teacherAttEntries[t.id] || 'PRESENT' }));
      await api.post('/attendance/teachers', { attendanceDate: date, records });
      toast.success('Teacher attendance saved');
      loadTeacherAttendance();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const reviewLeave = async (id, decision) => {
    try {
      await api.patch(`/leaves/${id}/review`, { decision, remarks: `Reviewed (${decision})` });
      toast.success(`Leave ${decision.toLowerCase()} (attendance updated if approved)`);
      const res = await api.get('/leaves');
      setLeaves(res.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader title="Attendance & Leave Management" subtitle="Mark daily attendance, review leave requests, and inspect attendance reports." />

      <ul className="nav nav-tabs mb-3">
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'mark' ? 'active' : ''}`} onClick={() => setTab('mark')}>Student Attendance</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'report' ? 'active' : ''}`} onClick={() => { setTab('report'); loadReport(); }}>Monthly Report</button></li>
        {isAdmin && <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'teachers' ? 'active' : ''}`} onClick={() => { setTab('teachers'); loadTeacherAttendance(); }}>Teacher Attendance</button></li>}
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'leaves' ? 'active' : ''}`} onClick={() => setTab('leaves')}>Leave Review ({leaves.filter((l) => l.status === 'PENDING').length})</button></li>
      </ul>

      {tab === 'mark' && (
        <div className="tgs-card p-4">
          <div className="row g-2 align-items-end mb-4">
            <div className="col-md-4">
              <label className="form-label small fw-semibold">Class &amp; Section</label>
              <select className="form-select form-select-sm" value={sectionId} onChange={(e) => setSectionId(e.target.value)}>
                {sections.map((s) => <option key={s.id} value={s.id}>{s.className} - {s.name}</option>)}
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label small fw-semibold">Date</label>
              <input type="date" className="form-control form-control-sm" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div className="col-md-3">
              <label className="form-label small fw-semibold">Session</label>
              <select className="form-select form-select-sm" value={session} onChange={(e) => setSession(e.target.value)}>
                <option value="FULL_DAY">FULL_DAY</option><option value="MORNING">MORNING</option><option value="AFTERNOON">AFTERNOON</option>
              </select>
            </div>
          </div>

          {savedRecords.length > 0 ? (
            <>
              <div className="alert alert-success small py-2">Attendance has already been marked for this section, date, and session.</div>
              <table className="table tgs-table table-sm mb-0">
                <thead><tr><th>Roll</th><th>Adm. No</th><th>Student</th><th>Status</th><th>Correction</th></tr></thead>
                <tbody>
                  {savedRecords.map((r) => (
                    <tr key={r.id}>
                      <td>{r.rollNumber || '-'}</td>
                      <td>{r.admissionNumber}</td>
                      <td className="fw-semibold">{r.studentName}</td>
                      <td><StatusBadge status={r.status} /></td>
                      <td>
                        {isAdmin && (
                          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => { setCorrectModal(r); setCorrectForm({ status: r.status, reason: '' }); }}>
                            <Edit2 size={13} /> Correct
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          ) : students.length === 0 ? (
            <p className="text-muted small mb-0">No active students in this section.</p>
          ) : (
            <>
              <table className="table tgs-table table-sm">
                <thead><tr><th>Roll</th><th>Adm. No</th><th>Student Name</th><th>Status</th></tr></thead>
                <tbody>
                  {students.map((s) => (
                    <tr key={s.id}>
                      <td>{s.rollNumber || '-'}</td>
                      <td>{s.admissionNumber}</td>
                      <td className="fw-semibold">{s.fullName}</td>
                      <td>
                        <select className="form-select form-select-sm" style={{ maxWidth: 190 }} value={entries[s.id] || 'PRESENT'}
                          onChange={(e) => setEntries({ ...entries, [s.id]: e.target.value })}>
                          <option value="PRESENT">PRESENT</option>
                          <option value="ABSENT">ABSENT</option>
                          <option value="LATE">LATE</option>
                          <option value="HALF_DAY">HALF_DAY</option>
                          <option value="APPROVED_LEAVE">APPROVED_LEAVE</option>
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="text-end">
                <button type="button" className="btn tgs-btn-navy btn-sm" onClick={saveStudentAttendance}>
                  <CalendarCheck size={15} className="me-1" /> Save Attendance
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {tab === 'report' && (
        <div className="tgs-card p-4">
          <div className="row g-2 align-items-end mb-3">
            <div className="col-md-3"><label className="form-label small">From</label><input type="date" className="form-control form-control-sm" value={reportRange.from} onChange={(e) => setReportRange({ ...reportRange, from: e.target.value })} /></div>
            <div className="col-md-3"><label className="form-label small">To</label><input type="date" className="form-control form-control-sm" value={reportRange.to} onChange={(e) => setReportRange({ ...reportRange, to: e.target.value })} /></div>
            <div className="col-md-2"><button type="button" className="btn tgs-btn-navy btn-sm w-100" onClick={loadReport}>Filter</button></div>
          </div>
          <div className="table-responsive">
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Date</th><th>Student</th><th>Class</th><th>Session</th><th>Status</th></tr></thead>
              <tbody>
                {reportRows.map((r) => (
                  <tr key={r.id}>
                    <td>{formatDate(r.attendanceDate)}</td>
                    <td className="fw-semibold">{r.studentName} ({r.admissionNumber})</td>
                    <td>{r.className} - {r.sectionName}</td>
                    <td>{r.session}</td>
                    <td><StatusBadge status={r.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'teachers' && isAdmin && (
        <div className="tgs-card p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="fw-bold mb-0">Teacher Daily Attendance ({formatDate(date)})</h6>
            {teacherSaved.length === 0 && <button type="button" className="btn tgs-btn-navy btn-sm" onClick={saveTeacherAttendance}>Save Teacher Attendance</button>}
          </div>
          {teacherSaved.length > 0 ? (
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Emp Code</th><th>Teacher</th><th>Status</th></tr></thead>
              <tbody>
                {teacherSaved.map((ta) => (
                  <tr key={ta.id}><td>{ta.employeeCode}</td><td className="fw-semibold">{ta.teacherName}</td><td><StatusBadge status={ta.status} /></td></tr>
                ))}
              </tbody>
            </table>
          ) : (
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Emp Code</th><th>Teacher</th><th>Status</th></tr></thead>
              <tbody>
                {teachers.map((t) => (
                  <tr key={t.id}>
                    <td>{t.employeeCode}</td>
                    <td className="fw-semibold">{t.fullName}</td>
                    <td>
                      <select className="form-select form-select-sm" style={{ maxWidth: 180 }} value={teacherAttEntries[t.id] || 'PRESENT'}
                        onChange={(e) => setTeacherAttEntries({ ...teacherAttEntries, [t.id]: e.target.value })}>
                        <option value="PRESENT">PRESENT</option><option value="ABSENT">ABSENT</option><option value="LATE">LATE</option><option value="HALF_DAY">HALF_DAY</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'leaves' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Type</th><th>Applicant</th><th>Dates</th><th>Reason</th><th>Status</th><th className="text-end">Review</th></tr></thead>
            <tbody>
              {leaves.map((l) => (
                <tr key={l.id}>
                  <td><span className="badge bg-light text-dark border">{l.applicantType}</span></td>
                  <td className="fw-semibold">{l.studentName || l.teacherName}</td>
                  <td className="small">{formatDate(l.startDate)} – {formatDate(l.endDate)}</td>
                  <td className="small">{l.reason}</td>
                  <td><StatusBadge status={l.status} /></td>
                  <td className="text-end">
                    {l.status === 'PENDING' && (l.applicantType === 'STUDENT' || isAdmin) && (
                      <div className="btn-group btn-group-sm">
                        <button type="button" className="btn btn-outline-success" onClick={() => reviewLeave(l.id, 'APPROVED')}><Check size={13} /></button>
                        <button type="button" className="btn btn-outline-danger" onClick={() => reviewLeave(l.id, 'REJECTED')}><X size={13} /></button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal show={!!correctModal} title={`Correct Attendance: ${correctModal?.studentName}`} onClose={() => setCorrectModal(null)} size="sm">
        <form onSubmit={correctRecord} className="d-flex flex-column gap-3">
          <div>
            <label className="form-label small">New Status</label>
            <select className="form-select form-select-sm" value={correctForm.status} onChange={(e) => setCorrectForm({ ...correctForm, status: e.target.value })}>
              <option value="PRESENT">PRESENT</option><option value="ABSENT">ABSENT</option><option value="LATE">LATE</option><option value="HALF_DAY">HALF_DAY</option><option value="APPROVED_LEAVE">APPROVED_LEAVE</option>
            </select>
          </div>
          <div>
            <label className="form-label small">Correction Reason *</label>
            <input required className="form-control form-control-sm" value={correctForm.reason} onChange={(e) => setCorrectForm({ ...correctForm, reason: e.target.value })} />
          </div>
          <button type="submit" className="btn tgs-btn-navy btn-sm">Update Record</button>
        </form>
      </Modal>
    </div>
  );
}
