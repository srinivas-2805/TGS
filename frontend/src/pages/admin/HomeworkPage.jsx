import React, { useEffect, useState } from 'react';
import { Plus, Download, Upload, Eye } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

export default function HomeworkPage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isTeacher = hasAnyRole('TEACHER');

  const [homework, setHomework] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hwModal, setHwModal] = useState(false);
  const [matModal, setMatModal] = useState(false);
  const [submissionsModal, setSubmissionsModal] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [hwForm, setHwForm] = useState({ sectionId: '', subjectId: '', title: '', description: '', dueDate: '', file: null });
  const [matForm, setMatForm] = useState({ sectionId: '', subjectId: '', title: '', description: '', materialType: 'NOTES', file: null });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [hwR, matR, secR, subR] = await Promise.all([
        api.get('/homework'),
        api.get('/homework/materials'),
        api.get('/academics/sections'),
        api.get('/academics/subjects'),
      ]);
      setHomework(hwR.data?.data || []);
      setMaterials(matR.data?.data || []);
      setSections(secR.data?.data || []);
      setSubjects(subR.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, []);

  const createHomework = async (e) => {
    e.preventDefault();
    try {
      const fd = new FormData();
      fd.append('sectionId', hwForm.sectionId);
      fd.append('subjectId', hwForm.subjectId);
      fd.append('title', hwForm.title);
      fd.append('description', hwForm.description);
      fd.append('dueDate', hwForm.dueDate);
      fd.append('allowSubmission', 'true');
      if (hwForm.file) fd.append('file', hwForm.file);
      await api.post('/homework/with-file', fd);
      toast.success('Homework published');
      setHwModal(false);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const uploadMaterial = async (e) => {
    e.preventDefault();
    try {
      const fd = new FormData();
      fd.append('sectionId', matForm.sectionId);
      fd.append('subjectId', matForm.subjectId);
      fd.append('title', matForm.title);
      if (matForm.description) fd.append('description', matForm.description);
      fd.append('materialType', matForm.materialType);
      fd.append('file', matForm.file);
      await api.post('/homework/materials', fd);
      toast.success('Study material uploaded');
      setMatModal(false);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const viewSubmissions = async (hw) => {
    setSubmissionsModal(hw);
    try {
      const res = await api.get(`/homework/${hw.id}/submissions`);
      setSubmissions(res.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const reviewSub = async (subId, status) => {
    try {
      await api.patch(`/homework/submissions/${subId}/review`, { status, teacherFeedback: status === 'REVIEWED' ? 'Good work' : 'Please revise and resubmit' });
      toast.success('Submission reviewed');
      viewSubmissions(submissionsModal);
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Homework & Study Materials"
        subtitle="Create homework assignments, review student submissions, and share study resources."
        actions={
          isTeacher && (
            <>
              <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setMatModal(true)}><Upload size={14} className="me-1" /> Upload Material</button>
              <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setHwModal(true)}><Plus size={14} className="me-1" /> Create Homework</button>
            </>
          )
        }
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Homework Assignments ({homework.length})</h6>
            {homework.map((hw) => (
              <div key={hw.id} className="p-3 border rounded mb-2">
                <div className="d-flex justify-content-between align-items-center mb-1">
                  <span className="badge bg-primary-subtle text-primary">{hw.className}-{hw.sectionName} • {hw.subjectName}</span>
                  <small className="text-muted">Due: {formatDate(hw.dueDate)}</small>
                </div>
                <div className="fw-bold">{hw.title}</div>
                <p className="small text-muted mb-2">{hw.description}</p>
                <div className="d-flex gap-2">
                  {hw.attachmentName && (
                    <button type="button" className="btn btn-sm btn-outline-secondary"
                      onClick={() => downloadFile(`/homework/${hw.id}/attachment`, hw.attachmentName)}>
                      <Download size={13} /> File
                    </button>
                  )}
                  <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => viewSubmissions(hw)}>
                    <Eye size={13} className="me-1" /> Submissions
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="col-lg-5">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Study Materials ({materials.length})</h6>
            {materials.map((m) => (
              <div key={m.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center small">
                <div>
                  <div className="fw-semibold">{m.title}</div>
                  <div className="text-muted">{m.className} • {m.subjectName} • {m.materialType}</div>
                </div>
                <button type="button" className="btn btn-sm btn-outline-primary"
                  onClick={() => downloadFile(`/homework/materials/${m.id}/download`, m.fileName)}>
                  <Download size={13} />
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      <Modal show={hwModal} title="Create Homework" onClose={() => setHwModal(false)} size="md">
        <form onSubmit={createHomework} className="row g-2">
          <div className="col-6"><label className="form-label small">Section *</label>
            <select required className="form-select form-select-sm" value={hwForm.sectionId} onChange={(e) => setHwForm({ ...hwForm, sectionId: e.target.value })}>
              <option value="">Select</option>{sections.map((s) => <option key={s.id} value={s.id}>{s.className}-{s.name}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Subject *</label>
            <select required className="form-select form-select-sm" value={hwForm.subjectId} onChange={(e) => setHwForm({ ...hwForm, subjectId: e.target.value })}>
              <option value="">Select</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="col-8"><label className="form-label small">Title *</label><input required className="form-control form-control-sm" value={hwForm.title} onChange={(e) => setHwForm({ ...hwForm, title: e.target.value })} /></div>
          <div className="col-4"><label className="form-label small">Due Date *</label><input type="date" required className="form-control form-control-sm" value={hwForm.dueDate} onChange={(e) => setHwForm({ ...hwForm, dueDate: e.target.value })} /></div>
          <div className="col-12"><label className="form-label small">Instructions *</label><textarea rows={3} required className="form-control form-control-sm" value={hwForm.description} onChange={(e) => setHwForm({ ...hwForm, description: e.target.value })} /></div>
          <div className="col-12"><label className="form-label small">Attachment (PDF/Image)</label><input type="file" className="form-control form-control-sm" onChange={(e) => setHwForm({ ...hwForm, file: e.target.files?.[0] || null })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Publish Homework</button></div>
        </form>
      </Modal>

      <Modal show={matModal} title="Upload Study Material" onClose={() => setMatModal(false)} size="md">
        <form onSubmit={uploadMaterial} className="row g-2">
          <div className="col-6"><label className="form-label small">Section *</label>
            <select required className="form-select form-select-sm" value={matForm.sectionId} onChange={(e) => setMatForm({ ...matForm, sectionId: e.target.value })}>
              <option value="">Select</option>{sections.map((s) => <option key={s.id} value={s.id}>{s.className}-{s.name}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Subject *</label>
            <select required className="form-select form-select-sm" value={matForm.subjectId} onChange={(e) => setMatForm({ ...matForm, subjectId: e.target.value })}>
              <option value="">Select</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="col-8"><label className="form-label small">Title *</label><input required className="form-control form-control-sm" value={matForm.title} onChange={(e) => setMatForm({ ...matForm, title: e.target.value })} /></div>
          <div className="col-4"><label className="form-label small">Type</label><select className="form-select form-select-sm" value={matForm.materialType} onChange={(e) => setMatForm({ ...matForm, materialType: e.target.value })}><option value="NOTES">NOTES</option><option value="WORKSHEET">WORKSHEET</option><option value="SYLLABUS">SYLLABUS</option></select></div>
          <div className="col-12"><label className="form-label small">File *</label><input type="file" required className="form-control form-control-sm" onChange={(e) => setMatForm({ ...matForm, file: e.target.files?.[0] || null })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Upload</button></div>
        </form>
      </Modal>

      <Modal show={!!submissionsModal} title={`Submissions — ${submissionsModal?.title}`} onClose={() => setSubmissionsModal(null)} size="lg">
        {submissions.length === 0 ? <p className="text-muted small mb-0">No student submissions yet.</p> : (
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th>Student</th><th>Response</th><th>Status</th><th className="text-end">Review</th></tr></thead>
            <tbody>
              {submissions.map((sub) => (
                <tr key={sub.id}>
                  <td className="fw-semibold">{sub.studentName}</td>
                  <td className="small">{sub.submissionText || sub.attachmentName || '-'}</td>
                  <td><StatusBadge status={sub.status} /></td>
                  <td className="text-end">
                    {isTeacher && (
                      <div className="btn-group btn-group-sm">
                        <button type="button" className="btn btn-outline-success" onClick={() => reviewSub(sub.id, 'REVIEWED')}>Approve</button>
                        <button type="button" className="btn btn-outline-warning" onClick={() => reviewSub(sub.id, 'RESUBMIT')}>Resubmit</button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Modal>
    </div>
  );
}
