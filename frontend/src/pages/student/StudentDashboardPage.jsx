import React, { useEffect, useState } from 'react';
import { BookOpen, CalendarCheck, Clock, Download, FileSpreadsheet, GraduationCap, Upload } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PageHeader, StatCard, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

export default function StudentDashboardPage() {
  const toast = useToast();
  const [profile, setProfile] = useState(null);
  const [attendance, setAttendance] = useState(null);
  const [homework, setHomework] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [timetable, setTimetable] = useState([]);
  const [exams, setExams] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [reportCard, setReportCard] = useState(null);
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitModal, setSubmitModal] = useState(null);
  const [subText, setSubText] = useState('');
  const [subFile, setSubFile] = useState(null);

  const loadAll = async () => {
    setLoading(true);
    try {
      const meRes = await api.get('/students/me');
      const stu = meRes.data?.data;
      setProfile(stu);
      const secId = stu?.currentEnrollment?.sectionId;
      const clsId = stu?.currentEnrollment?.classId;
      const [attRes, hwRes, matRes, ttRes, exRes, annRes] = await Promise.all([
        api.get(`/attendance/student/${stu.id}`),
        api.get(`/homework/student/${stu.id}`),
        clsId ? api.get(`/homework/materials?classId=${clsId}`) : Promise.resolve({ data: { data: [] } }),
        secId ? api.get(`/timetables/section/${secId}`) : Promise.resolve({ data: { data: [] } }),
        api.get('/examinations'),
        api.get('/announcements'),
      ]);
      setAttendance(attRes.data?.data);
      setHomework(hwRes.data?.data || []);
      setMaterials(matRes.data?.data || []);
      setTimetable(ttRes.data?.data || []);
      const exList = exRes.data?.data || [];
      setExams(exList);
      if (exList.length > 0) setSelectedExamId(String(exList[0].id));
      setAnnouncements(annRes.data?.data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  useEffect(() => {
    if (profile?.id && selectedExamId) {
      api.get(`/examinations/report-card?studentId=${profile.id}&examinationId=${selectedExamId}`)
        .then((res) => setReportCard(res.data?.data || null))
        .catch(() => setReportCard(null));
    }
  }, [profile?.id, selectedExamId]);

  const handleHwSubmit = async (e) => {
    e.preventDefault();
    try {
      const fd = new FormData();
      if (subText) fd.append('submissionText', subText);
      if (subFile) fd.append('file', subFile);
      await api.post(`/homework/${submitModal.id}/submit`, fd);
      toast.success('Homework submitted successfully!');
      setSubmitModal(null);
      setSubText('');
      setSubFile(null);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={6} />;
  if (!profile) return null;

  return (
    <div>
      <PageHeader
        title={`Hello, ${profile.fullName}!`}
        subtitle={`${profile.currentEnrollment?.className || ''} - ${profile.currentEnrollment?.sectionName || ''} • Admission No: ${profile.admissionNumber}`}
        actions={
          <button
            type="button"
            className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-1"
            onClick={() => downloadFile(`/students/${profile.id}/id-card/pdf`, `TGS-IDCard-${profile.admissionNumber}.pdf`)}
          >
            <Download size={14} /> Download ID Card
          </button>
        }
      />

      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-lg-3">
          <StatCard title="My Class" value={`${profile.currentEnrollment?.className || '-'} ${profile.currentEnrollment?.sectionName || ''}`} subtitle={`Roll No: ${profile.currentEnrollment?.rollNumber || '-'}`} icon={GraduationCap} color="primary" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Attendance" value={`${attendance?.attendancePercentage ?? 0}%`} subtitle={`${attendance?.presentDays ?? 0} Present Days`} icon={CalendarCheck} color="green" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Homework" value={homework.length} subtitle="Assigned Tasks" icon={BookOpen} color="gold" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Timetable Slots" value={timetable.length} subtitle="Weekly Classes" icon={Clock} color="navy" />
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">My Homework</h6>
            {homework.length === 0 ? <p className="text-muted small mb-0">No homework assigned right now.</p> : (
              homework.map((hw) => (
                <div key={hw.id} className="p-3 border rounded mb-2">
                  <div className="d-flex justify-content-between align-items-center mb-1">
                    <span className="badge bg-primary-subtle text-primary">{hw.subjectName}</span>
                    <small className="text-muted">Due: {formatDate(hw.dueDate)}</small>
                  </div>
                  <div className="fw-semibold">{hw.title}</div>
                  <div className="small text-muted mb-2">{hw.description}</div>
                  <div className="d-flex flex-wrap gap-2 align-items-center">
                    {hw.attachmentName && (
                      <button type="button" className="btn btn-sm btn-outline-secondary"
                        onClick={() => downloadFile(`/homework/${hw.id}/attachment`, hw.attachmentName)}>
                        <Download size={13} /> Attachment
                      </button>
                    )}
                    {hw.allowSubmission && (
                      <button type="button" className="btn btn-sm tgs-btn-navy" onClick={() => setSubmitModal(hw)}>
                        <Upload size={13} className="me-1" /> {hw.mySubmission ? 'Resubmit' : 'Submit Work'}
                      </button>
                    )}
                    {hw.mySubmission && <StatusBadge status={hw.mySubmission.status} />}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0 d-flex align-items-center gap-2"><FileSpreadsheet size={17} /> Examination Results</h6>
              {selectedExamId && (
                <button type="button" className="btn btn-sm btn-outline-primary"
                  onClick={() => downloadFile(`/examinations/report-card/pdf?studentId=${profile.id}&examinationId=${selectedExamId}`, `ReportCard-${profile.admissionNumber}.pdf`)}>
                  <Download size={13} /> PDF
                </button>
              )}
            </div>
            <select className="form-select form-select-sm mb-3" value={selectedExamId} onChange={(e) => setSelectedExamId(e.target.value)}>
              {exams.map((ex) => <option key={ex.id} value={ex.id}>{ex.name} ({ex.status})</option>)}
            </select>
            {reportCard && (reportCard.subjects || []).length > 0 ? (
              <div className="table-responsive">
                <table className="table tgs-table table-sm mb-2">
                  <thead><tr><th>Subject</th><th>Score</th><th>Grade</th></tr></thead>
                  <tbody>
                    {reportCard.subjects.map((m) => (
                      <tr key={m.id}>
                        <td>{m.subjectName}</td>
                        <td>{reportCard.activityGradedClass ? m.activityRating : `${m.marksObtained ?? 'AB'} / ${m.maxMarks}`}</td>
                        <td><span className="badge bg-primary-subtle text-primary">{m.grade}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="small fw-semibold text-primary">Overall: {reportCard.overallGrade} • {reportCard.resultStatus}</div>
              </div>
            ) : (
              <p className="text-muted small mb-0">No results published for the selected exam.</p>
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Study Materials</h6>
            {materials.length === 0 ? <p className="text-muted small mb-0">No study materials uploaded yet.</p> : (
              materials.map((m) => (
                <div key={m.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center">
                  <div className="small">
                    <div className="fw-semibold">{m.title}</div>
                    <div className="text-muted">{m.subjectName} • {m.materialType}</div>
                  </div>
                  <button type="button" className="btn btn-sm btn-outline-primary"
                    onClick={() => downloadFile(`/homework/materials/${m.id}/download`, m.fileName)}>
                    <Download size={14} />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">My Class Timetable &amp; Announcements</h6>
            <div className="mb-3">
              {timetable.slice(0, 6).map((slot) => (
                <div key={slot.id} className="small py-1 border-bottom d-flex justify-content-between">
                  <span><strong>{slot.dayOfWeek}</strong> P{slot.periodNumber} ({slot.startTime?.slice(0, 5)})</span>
                  <span>{slot.break ? slot.breakLabel : `${slot.subjectName} (${slot.teacherName || ''})`}</span>
                </div>
              ))}
            </div>
            {announcements.slice(0, 3).map((a) => (
              <div key={a.id} className="p-2 bg-light rounded mb-1 small">
                <div className="fw-semibold">{a.title}</div>
                <div className="text-muted">{a.content}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <Modal show={!!submitModal} title={`Submit: ${submitModal?.title}`} onClose={() => setSubmitModal(null)} size="md">
        <form onSubmit={handleHwSubmit} className="d-flex flex-column gap-3">
          <div>
            <label className="form-label small fw-semibold">Answer / Notes</label>
            <textarea rows={4} className="form-control" value={subText} onChange={(e) => setSubText(e.target.value)} placeholder="Type your response or notes" />
          </div>
          <div>
            <label className="form-label small fw-semibold">Upload File (PDF / Image, optional)</label>
            <input type="file" className="form-control" onChange={(e) => setSubFile(e.target.files?.[0] || null)} />
          </div>
          <div className="text-end">
            <button type="submit" className="btn tgs-btn-navy btn-sm">Submit Assignment</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
