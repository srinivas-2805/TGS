import React, { useEffect, useState } from 'react';
import { Plus, Download, CheckCircle2 } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

export default function ExaminationsPage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');

  const [tab, setTab] = useState('exams');
  const [exams, setExams] = useState([]);
  const [classes, setClasses] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [gradingRules, setGradingRules] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [selectedClassId, setSelectedClassId] = useState('');
  const [examSubjects, setExamSubjects] = useState([]);
  const [selectedExamSubject, setSelectedExamSubject] = useState(null);
  const [classStudents, setClassStudents] = useState([]);
  const [markInputs, setMarkInputs] = useState({});
  const [loading, setLoading] = useState(true);

  const [examModal, setExamModal] = useState(false);
  const [subjModal, setSubjModal] = useState(false);
  const [examForm, setExamForm] = useState({ name: '', examType: 'UNIT_TEST', startDate: '', endDate: '', description: '' });
  const [esForm, setEsForm] = useState({ classId: '', subjectId: '', examDate: '', maxMarks: 100, passingMarks: 35, activityBased: false });

  const loadBase = async () => {
    setLoading(true);
    try {
      const [eRes, cRes, sRes, gRes] = await Promise.all([
        api.get('/examinations'),
        api.get('/academics/classes'),
        api.get('/academics/subjects'),
        api.get('/examinations/grading-rules'),
      ]);
      const eList = eRes.data?.data || [];
      setExams(eList);
      if (eList.length > 0 && !selectedExamId) setSelectedExamId(String(eList[0].id));
      const cList = cRes.data?.data || [];
      setClasses(cList);
      if (cList.length > 0 && !selectedClassId) setSelectedClassId(String(cList[0].id));
      setSubjects(sRes.data?.data || []);
      setGradingRules(gRes.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadBase(); }, []);

  useEffect(() => {
    if (!selectedExamId) return;
    api.get(`/examinations/${selectedExamId}/subjects${selectedClassId ? `?classId=${selectedClassId}` : ''}`)
      .then((res) => setExamSubjects(res.data?.data || []))
      .catch(() => setExamSubjects([]));
  }, [selectedExamId, selectedClassId]);

  const openMarksEntry = async (es) => {
    setSelectedExamSubject(es);
    try {
      const [stuRes, markRes] = await Promise.all([
        api.get(`/students?classId=${es.classId}&status=ACTIVE&size=200`),
        api.get(`/examinations/subjects/${es.id}/marks`),
      ]);
      const stList = stuRes.data?.data?.content || [];
      setClassStudents(stList);
      const existing = {};
      (markRes.data?.data || []).forEach((m) => {
        existing[m.studentId] = {
          marksObtained: m.marksObtained ?? '',
          absent: m.absent,
          activityRating: m.activityRating || 'GOOD',
          descriptiveRemarks: m.descriptiveRemarks || '',
        };
      });
      const init = {};
      stList.forEach((s) => {
        init[s.id] = existing[s.id] || { marksObtained: '', absent: false, activityRating: 'GOOD', descriptiveRemarks: '' };
      });
      setMarkInputs(init);
      setTab('marks');
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const saveMarks = async () => {
    try {
      const marks = classStudents.map((s) => {
        const inp = markInputs[s.id] || {};
        return {
          studentId: s.id,
          marksObtained: selectedExamSubject.activityBased || selectedExamSubject.classActivityGraded ? null : (inp.absent ? 0 : Number(inp.marksObtained)),
          absent: !!inp.absent,
          activityRating: inp.activityRating,
          descriptiveRemarks: inp.descriptiveRemarks,
        };
      });
      await api.post('/examinations/marks', { examinationSubjectId: selectedExamSubject.id, marks });
      toast.success('Marks and grades saved successfully!');
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const createExam = async (e) => {
    e.preventDefault();
    try {
      await api.post('/examinations', examForm);
      toast.success('Examination created');
      setExamModal(false);
      loadBase();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const configureSubject = async (e) => {
    e.preventDefault();
    try {
      await api.post('/examinations/subjects', {
        ...esForm,
        examinationId: Number(selectedExamId),
        classId: Number(esForm.classId || selectedClassId),
        subjectId: Number(esForm.subjectId),
        maxMarks: Number(esForm.maxMarks),
        passingMarks: Number(esForm.passingMarks),
      });
      toast.success('Subject configured for examination');
      setSubjModal(false);
      const res = await api.get(`/examinations/${selectedExamId}/subjects${selectedClassId ? `?classId=${selectedClassId}` : ''}`);
      setExamSubjects(res.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const publishExam = async (id) => {
    try {
      await api.patch(`/examinations/${id}/status`, { status: 'PUBLISHED' });
      toast.success('Examination results published');
      loadBase();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Examinations, Marks & Report Cards"
        subtitle="Manage exam schedules, numeric marks (1st–10th), LKG/UKG activity assessments, and PDF report cards."
        actions={
          isAdmin && (
            <>
              <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setSubjModal(true)}>Configure Subject</button>
              <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setExamModal(true)}><Plus size={14} className="me-1" /> Create Exam</button>
            </>
          )
        }
      />

      <ul className="nav nav-tabs mb-3">
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'exams' ? 'active' : ''}`} onClick={() => setTab('exams')}>Examinations &amp; Schedule</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'marks' ? 'active' : ''}`} onClick={() => setTab('marks')}>Marks Entry</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'grading' ? 'active' : ''}`} onClick={() => setTab('grading')}>Grading Rules</button></li>
      </ul>

      {tab === 'exams' && (
        <div className="row g-4">
          <div className="col-lg-5">
            <div className="tgs-card p-4">
              <h6 className="fw-bold mb-3">Examinations</h6>
              {exams.map((ex) => (
                <div
                  key={ex.id}
                  onClick={() => setSelectedExamId(String(ex.id))}
                  className={`p-3 border rounded mb-2 ${String(ex.id) === selectedExamId ? 'border-primary bg-primary-subtle' : ''}`}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="d-flex justify-content-between align-items-center mb-1">
                    <span className="fw-bold small">{ex.name}</span>
                    <StatusBadge status={ex.status} />
                  </div>
                  <div className="small text-muted">{ex.examType} • {formatDate(ex.startDate)} – {formatDate(ex.endDate)}</div>
                  {isAdmin && ex.status !== 'PUBLISHED' && (
                    <button type="button" className="btn btn-sm btn-outline-success mt-2" onClick={(e) => { e.stopPropagation(); publishExam(ex.id); }}>
                      <CheckCircle2 size={13} className="me-1" /> Publish Results
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="col-lg-7">
            <div className="tgs-card p-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 className="fw-bold mb-0">Configured Exam Subjects</h6>
                <select className="form-select form-select-sm" style={{ width: 180 }} value={selectedClassId} onChange={(e) => setSelectedClassId(e.target.value)}>
                  <option value="">All Classes</option>
                  {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <table className="table tgs-table table-sm mb-0">
                <thead><tr><th>Class</th><th>Subject</th><th>Mode</th><th>Max / Pass</th><th className="text-end">Action</th></tr></thead>
                <tbody>
                  {examSubjects.map((es) => (
                    <tr key={es.id}>
                      <td className="fw-semibold">{es.className}</td>
                      <td>{es.subjectName}</td>
                      <td>{es.activityBased || es.classActivityGraded ? <span className="badge bg-warning-subtle text-dark">Activity</span> : 'Marks'}</td>
                      <td>{es.maxMarks} / {es.passingMarks}</td>
                      <td className="text-end">
                        <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => openMarksEntry(es)}>Enter Marks</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {tab === 'marks' && (
        <div className="tgs-card p-4">
          {!selectedExamSubject ? (
            <p className="text-muted small mb-0">Select an examination subject from the &ldquo;Examinations &amp; Schedule&rdquo; tab to enter marks.</p>
          ) : (
            <>
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <h6 className="fw-bold mb-0">{selectedExamSubject.className} — {selectedExamSubject.subjectName}</h6>
                  <small className="text-muted">Max Marks: {selectedExamSubject.maxMarks} • Passing: {selectedExamSubject.passingMarks}</small>
                </div>
                <button type="button" className="btn tgs-btn-navy btn-sm" onClick={saveMarks}>Save All Marks</button>
              </div>
              <table className="table tgs-table table-sm mb-0">
                <thead>
                  <tr>
                    <th>Adm. No</th><th>Student</th>
                    {selectedExamSubject.activityBased || selectedExamSubject.classActivityGraded ? <th>Activity Rating</th> : <><th>Marks</th><th>Absent</th></>}
                    <th>Descriptive Remarks</th><th>Report Card</th>
                  </tr>
                </thead>
                <tbody>
                  {classStudents.map((s) => {
                    const inp = markInputs[s.id] || {};
                    const isAct = selectedExamSubject.activityBased || selectedExamSubject.classActivityGraded;
                    return (
                      <tr key={s.id}>
                        <td>{s.admissionNumber}</td>
                        <td className="fw-semibold">{s.fullName}</td>
                        {isAct ? (
                          <td>
                            <select className="form-select form-select-sm" value={inp.activityRating || 'GOOD'}
                              onChange={(e) => setMarkInputs({ ...markInputs, [s.id]: { ...inp, activityRating: e.target.value } })}>
                              <option value="EXCELLENT">EXCELLENT</option><option value="GOOD">GOOD</option><option value="SATISFACTORY">SATISFACTORY</option><option value="NEEDS_SUPPORT">NEEDS_SUPPORT</option>
                            </select>
                          </td>
                        ) : (
                          <>
                            <td>
                              <input type="number" step="0.5" min={0} max={selectedExamSubject.maxMarks} disabled={inp.absent}
                                className="form-control form-control-sm" style={{ width: 95 }} value={inp.marksObtained ?? ''}
                                onChange={(e) => setMarkInputs({ ...markInputs, [s.id]: { ...inp, marksObtained: e.target.value } })} />
                            </td>
                            <td>
                              <input type="checkbox" checked={!!inp.absent}
                                onChange={(e) => setMarkInputs({ ...markInputs, [s.id]: { ...inp, absent: e.target.checked } })} />
                            </td>
                          </>
                        )}
                        <td>
                          <input className="form-control form-control-sm" value={inp.descriptiveRemarks || ''}
                            onChange={(e) => setMarkInputs({ ...markInputs, [s.id]: { ...inp, descriptiveRemarks: e.target.value } })} />
                        </td>
                        <td>
                          <button type="button" className="btn btn-sm btn-outline-secondary"
                            onClick={() => downloadFile(`/examinations/report-card/pdf?studentId=${s.id}&examinationId=${selectedExamSubject.examinationId}`, `ReportCard-${s.admissionNumber}.pdf`)}>
                            <Download size={13} /> PDF
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </>
          )}
        </div>
      )}

      {tab === 'grading' && (
        <div className="tgs-card p-4">
          <h6 className="fw-bold mb-3">Configurable Grading Scale</h6>
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th>Grade</th><th>Range (%)</th><th>Grade Point</th><th>Description</th><th>Passing</th></tr></thead>
            <tbody>
              {gradingRules.map((g) => (
                <tr key={g.id}>
                  <td className="fw-bold text-primary">{g.gradeName}</td>
                  <td>{g.minPercentage}% – {g.maxPercentage}%</td>
                  <td>{g.gradePoint}</td>
                  <td>{g.description}</td>
                  <td>{g.passing ? 'Yes' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal show={examModal} title="Create Examination" onClose={() => setExamModal(false)} size="md">
        <form onSubmit={createExam} className="row g-2">
          <div className="col-12"><label className="form-label small">Exam Name *</label><input required className="form-control form-control-sm" value={examForm.name} onChange={(e) => setExamForm({ ...examForm, name: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Type</label>
            <select className="form-select form-select-sm" value={examForm.examType} onChange={(e) => setExamForm({ ...examForm, examType: e.target.value })}>
              {['UNIT_TEST', 'FORMATIVE', 'SUMMATIVE', 'HALF_YEARLY', 'ANNUAL'].map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className="col-3"><label className="form-label small">Start Date</label><input type="date" required className="form-control form-control-sm" value={examForm.startDate} onChange={(e) => setExamForm({ ...examForm, startDate: e.target.value })} /></div>
          <div className="col-3"><label className="form-label small">End Date</label><input type="date" required className="form-control form-control-sm" value={examForm.endDate} onChange={(e) => setExamForm({ ...examForm, endDate: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Create Exam</button></div>
        </form>
      </Modal>

      <Modal show={subjModal} title="Configure Exam Subject" onClose={() => setSubjModal(false)} size="md">
        <form onSubmit={configureSubject} className="row g-2">
          <div className="col-6"><label className="form-label small">Class *</label>
            <select required className="form-select form-select-sm" value={esForm.classId || selectedClassId} onChange={(e) => setEsForm({ ...esForm, classId: e.target.value })}>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Subject *</label>
            <select required className="form-select form-select-sm" value={esForm.subjectId} onChange={(e) => setEsForm({ ...esForm, subjectId: e.target.value })}>
              <option value="">Select Subject</option>
              {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="col-4"><label className="form-label small">Exam Date</label><input type="date" className="form-control form-control-sm" value={esForm.examDate} onChange={(e) => setEsForm({ ...esForm, examDate: e.target.value })} /></div>
          <div className="col-4"><label className="form-label small">Max Marks</label><input type="number" className="form-control form-control-sm" value={esForm.maxMarks} onChange={(e) => setEsForm({ ...esForm, maxMarks: e.target.value })} /></div>
          <div className="col-4"><label className="form-label small">Passing Marks</label><input type="number" className="form-control form-control-sm" value={esForm.passingMarks} onChange={(e) => setEsForm({ ...esForm, passingMarks: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Save Configuration</button></div>
        </form>
      </Modal>
    </div>
  );
}
