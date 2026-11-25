import React, { useEffect, useState } from 'react';
import { Plus, Download, Trash2, CheckCircle } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';

export function AcademicsPage() {
  const toast = useToast();
  const [years, setYears] = useState([]);
  const [classes, setClasses] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [secForm, setSecForm] = useState({ classId: '', name: 'A', capacity: 40, roomNumber: '', classTeacherId: '' });
  const [subForm, setSubForm] = useState({ name: '', code: '', description: '', isActivityBased: false });
  const [yrForm, setYrForm] = useState({ name: '', startDate: '', endDate: '', isCurrent: false });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [yR, cR, sR, subR, tR] = await Promise.all([
        api.get('/academics/academic-years'),
        api.get('/academics/classes'),
        api.get('/academics/sections'),
        api.get('/academics/subjects'),
        api.get('/teachers?size=100'),
      ]);
      setYears(yR.data?.data || []);
      setClasses(cR.data?.data || []);
      setSections(sR.data?.data || []);
      setSubjects(subR.data?.data || []);
      setTeachers(tR.data?.data?.content || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadAll(); }, []);

  const addYear = async (e) => {
    e.preventDefault();
    try {
      await api.post('/academics/academic-years', yrForm);
      toast.success('Academic year added');
      setYrForm({ name: '', startDate: '', endDate: '', isCurrent: false });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const setCurrentYear = async (id) => {
    try {
      await api.patch(`/academics/academic-years/${id}/set-current`);
      toast.success('Current academic year updated');
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const addSection = async (e) => {
    e.preventDefault();
    try {
      await api.post('/academics/sections', {
        ...secForm,
        classId: Number(secForm.classId),
        capacity: Number(secForm.capacity),
        classTeacherId: secForm.classTeacherId ? Number(secForm.classTeacherId) : null,
      });
      toast.success('Section created');
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const addSubject = async (e) => {
    e.preventDefault();
    try {
      await api.post('/academics/subjects', subForm);
      toast.success('Subject created');
      setSubForm({ name: '', code: '', description: '', isActivityBased: false });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader title="Academics: Years, Classes, Sections & Subjects" subtitle="Configure LKG to 10th Class structure, sections, capacities, and class teachers." />

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Academic Years</h6>
            <form onSubmit={addYear} className="row g-2 mb-3">
              <div className="col-4"><input required className="form-control form-control-sm" placeholder="2027-2028" value={yrForm.name} onChange={(e) => setYrForm({ ...yrForm, name: e.target.value })} /></div>
              <div className="col-3"><input type="date" required className="form-control form-control-sm" value={yrForm.startDate} onChange={(e) => setYrForm({ ...yrForm, startDate: e.target.value })} /></div>
              <div className="col-3"><input type="date" required className="form-control form-control-sm" value={yrForm.endDate} onChange={(e) => setYrForm({ ...yrForm, endDate: e.target.value })} /></div>
              <div className="col-2"><button type="submit" className="btn tgs-btn-navy btn-sm w-100"><Plus size={14} /></button></div>
            </form>
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Session</th><th>Dates</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {years.map((y) => (
                  <tr key={y.id}>
                    <td className="fw-semibold">{y.name}</td>
                    <td className="small">{y.startDate} to {y.endDate}</td>
                    <td>{y.current ? <span className="badge bg-success">CURRENT</span> : <StatusBadge status={y.status} />}</td>
                    <td className="text-end">
                      {!y.current && <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setCurrentYear(y.id)}><CheckCircle size={13} /> Set Current</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Subjects ({subjects.length})</h6>
            <form onSubmit={addSubject} className="row g-2 mb-3">
              <div className="col-5"><input required className="form-control form-control-sm" placeholder="Subject Name" value={subForm.name} onChange={(e) => setSubForm({ ...subForm, name: e.target.value })} /></div>
              <div className="col-3"><input required className="form-control form-control-sm" placeholder="Code" value={subForm.code} onChange={(e) => setSubForm({ ...subForm, code: e.target.value })} /></div>
              <div className="col-4"><button type="submit" className="btn tgs-btn-navy btn-sm w-100">Add Subject</button></div>
            </form>
            <div className="d-flex flex-wrap gap-2">
              {subjects.map((sub) => (
                <span key={sub.id} className="badge bg-light text-dark border p-2">
                  {sub.name} ({sub.code}) {sub.activityBased ? '• Activity' : ''}
                </span>
              ))}
            </div>
          </div>
        </div>

        <div className="col-12">
          <div className="tgs-card p-4">
            <h6 className="fw-bold mb-3">Class Sections &amp; Class Teachers</h6>
            <form onSubmit={addSection} className="row g-2 mb-3">
              <div className="col-md-3">
                <select required className="form-select form-select-sm" value={secForm.classId} onChange={(e) => setSecForm({ ...secForm, classId: e.target.value })}>
                  <option value="">Select Class</option>
                  {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <div className="col-md-2"><input required className="form-control form-control-sm" placeholder="Section (A/B/C)" value={secForm.name} onChange={(e) => setSecForm({ ...secForm, name: e.target.value })} /></div>
              <div className="col-md-2"><input type="number" required className="form-control form-control-sm" placeholder="Capacity" value={secForm.capacity} onChange={(e) => setSecForm({ ...secForm, capacity: e.target.value })} /></div>
              <div className="col-md-2"><input className="form-control form-control-sm" placeholder="Room" value={secForm.roomNumber} onChange={(e) => setSecForm({ ...secForm, roomNumber: e.target.value })} /></div>
              <div className="col-md-2">
                <select className="form-select form-select-sm" value={secForm.classTeacherId} onChange={(e) => setSecForm({ ...secForm, classTeacherId: e.target.value })}>
                  <option value="">Class Teacher</option>
                  {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
                </select>
              </div>
              <div className="col-md-1"><button type="submit" className="btn tgs-btn-navy btn-sm w-100">Add</button></div>
            </form>
            <div className="table-responsive">
              <table className="table tgs-table table-sm mb-0">
                <thead><tr><th>Class</th><th>Section</th><th>Capacity</th><th>Room</th><th>Class Teacher</th></tr></thead>
                <tbody>
                  {sections.map((s) => (
                    <tr key={s.id}>
                      <td className="fw-semibold">{s.className}</td>
                      <td>{s.name}</td>
                      <td>{s.capacity}</td>
                      <td>{s.roomNumber || '-'}</td>
                      <td>{s.classTeacherName || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function TimetablePage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [sectionId, setSectionId] = useState('');
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    dayOfWeek: 'MONDAY', periodNumber: 1, startTime: '09:00', endTime: '09:45',
    break: false, breakLabel: '', subjectId: '', teacherId: '', roomNumber: '',
  });

  useEffect(() => {
    Promise.all([
      api.get('/academics/sections'),
      api.get('/academics/subjects'),
      isAdmin ? api.get('/teachers?size=100') : Promise.resolve({ data: { data: { content: [] } } }),
    ]).then(([sR, subR, tR]) => {
      const sList = sR.data?.data || [];
      setSections(sList);
      if (sList.length > 0) setSectionId(String(sList[0].id));
      setSubjects(subR.data?.data || []);
      setTeachers(tR.data?.data?.content || []);
    }).finally(() => setLoading(false));
  }, [isAdmin]);

  const loadTimetable = async (sid) => {
    if (!sid) return;
    try {
      const res = await api.get(`/timetables/section/${sid}`);
      setSlots(res.data?.data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  useEffect(() => {
    if (sectionId) loadTimetable(sectionId);
  }, [sectionId]);

  const saveSlot = async (e) => {
    e.preventDefault();
    try {
      await api.post('/timetables', {
        ...form,
        sectionId: Number(sectionId),
        periodNumber: Number(form.periodNumber),
        startTime: form.startTime.length === 5 ? `${form.startTime}:00` : form.startTime,
        endTime: form.endTime.length === 5 ? `${form.endTime}:00` : form.endTime,
        subjectId: form.break ? null : Number(form.subjectId),
        teacherId: form.break ? null : Number(form.teacherId),
      });
      toast.success('Timetable period saved');
      setModalOpen(false);
      loadTimetable(sectionId);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const deleteSlot = async (id) => {
    try {
      await api.delete(`/timetables/${id}`);
      toast.success('Slot deleted');
      loadTimetable(sectionId);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Weekly Timetable Management"
        subtitle="Section-wise & teacher-wise period scheduling with automatic teacher & room conflict detection."
        actions={
          <>
            {sectionId && (
              <button type="button" className="btn btn-outline-primary btn-sm"
                onClick={() => downloadFile(`/timetables/section/${sectionId}/pdf`, `TGS-Timetable-${sectionId}.pdf`)}>
                <Download size={14} className="me-1" /> Export PDF
              </button>
            )}
            {isAdmin && <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setModalOpen(true)}><Plus size={14} className="me-1" /> Add Period</button>}
          </>
        }
      />

      <div className="tgs-card p-3 mb-4 d-flex align-items-center gap-3">
        <label className="small fw-semibold mb-0">Class &amp; Section:</label>
        <select className="form-select form-select-sm" style={{ maxWidth: 280 }} value={sectionId} onChange={(e) => setSectionId(e.target.value)}>
          {sections.map((s) => <option key={s.id} value={s.id}>{s.className} - {s.name}</option>)}
        </select>
      </div>

      <div className="tgs-card overflow-hidden">
        <table className="table tgs-table table-hover mb-0">
          <thead><tr><th>Day</th><th>Period</th><th>Time</th><th>Subject / Break</th><th>Teacher</th><th>Room</th>{isAdmin && <th></th>}</tr></thead>
          <tbody>
            {slots.length === 0 ? (
              <tr><td colSpan={7} className="text-center text-muted py-4">No timetable slots configured for this section yet.</td></tr>
            ) : (
              slots.map((s) => (
                <tr key={s.id}>
                  <td className="fw-semibold">{s.dayOfWeek}</td>
                  <td>Period {s.periodNumber}</td>
                  <td>{s.startTime?.slice(0, 5)} – {s.endTime?.slice(0, 5)}</td>
                  <td>{s.break ? <span className="badge bg-warning-subtle text-dark">{s.breakLabel || 'Break'}</span> : s.subjectName}</td>
                  <td>{s.teacherName || '-'}</td>
                  <td>{s.roomNumber || '-'}</td>
                  {isAdmin && (
                    <td className="text-end">
                      <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => deleteSlot(s.id)}><Trash2 size={13} /></button>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Modal show={modalOpen} title="Add Timetable Period" onClose={() => setModalOpen(false)} size="md">
        <form onSubmit={saveSlot} className="row g-2">
          <div className="col-6">
            <label className="form-label small">Day</label>
            <select className="form-select form-select-sm" value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}>
              {['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Period #</label><input type="number" min={1} className="form-control form-control-sm" value={form.periodNumber} onChange={(e) => setForm({ ...form, periodNumber: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Start Time</label><input type="time" required className="form-control form-control-sm" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">End Time</label><input type="time" required className="form-control form-control-sm" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></div>
          <div className="col-12 form-check ms-2 mt-2">
            <input type="checkbox" className="form-check-input" id="isBreakChk" checked={form.break} onChange={(e) => setForm({ ...form, break: e.target.checked })} />
            <label className="form-check-label small" htmlFor="isBreakChk">Break / Recess Period</label>
          </div>
          {form.break ? (
            <div className="col-12"><label className="form-label small">Break Label</label><input className="form-control form-control-sm" placeholder="Lunch / Short Break" value={form.breakLabel} onChange={(e) => setForm({ ...form, breakLabel: e.target.value })} /></div>
          ) : (
            <>
              <div className="col-6">
                <label className="form-label small">Subject *</label>
                <select required className="form-select form-select-sm" value={form.subjectId} onChange={(e) => setForm({ ...form, subjectId: e.target.value })}>
                  <option value="">Select Subject</option>
                  {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div className="col-6">
                <label className="form-label small">Teacher *</label>
                <select required className="form-select form-select-sm" value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })}>
                  <option value="">Select Teacher</option>
                  {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
                </select>
              </div>
              <div className="col-6"><label className="form-label small">Room Number</label><input className="form-control form-control-sm" value={form.roomNumber} onChange={(e) => setForm({ ...form, roomNumber: e.target.value })} /></div>
            </>
          )}
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Save Slot</button></div>
        </form>
      </Modal>
    </div>
  );
}
