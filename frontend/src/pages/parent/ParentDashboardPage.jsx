import React, { useEffect, useState, useCallback } from 'react';
import {
  Users, CalendarCheck, FileSpreadsheet, IndianRupee, BookOpen, Download, Send, MessageSquare
} from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PageHeader, StatCard, LoadingSkeleton, StatusBadge, EmptyState, Modal } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

export default function ParentDashboardPage() {
  const toast = useToast();
  const [children, setChildren] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [childData, setChildData] = useState(null);
  const [exams, setExams] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [reportCard, setReportCard] = useState(null);
  const [announcements, setAnnouncements] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [childLoading, setChildLoading] = useState(false);
  const [leaveModal, setLeaveModal] = useState(false);
  const [leaveForm, setLeaveForm] = useState({ startDate: '', endDate: '', reason: '' });

  useEffect(() => {
    Promise.all([api.get('/parents/my-children'), api.get('/examinations'), api.get('/announcements')])
      .then(([cRes, eRes, aRes]) => {
        const list = cRes.data?.data || [];
        setChildren(list);
        if (list.length > 0) setSelectedId(list[0].studentId);
        const exList = eRes.data?.data || [];
        setExams(exList);
        if (exList.length > 0) setSelectedExamId(String(exList[0].id));
        setAnnouncements(aRes.data?.data || []);
      })
      .catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const loadChildDetails = useCallback(async (studentId) => {
    if (!studentId) return;
    setChildLoading(true);
    try {
      const [profileRes, attRes, hwRes, feeRes, payRes, lvRes] = await Promise.all([
        api.get(`/students/${studentId}`),
        api.get(`/attendance/student/${studentId}`),
        api.get(`/homework/student/${studentId}`),
        api.get(`/fees/student/${studentId}`),
        api.get(`/fees/student/${studentId}/payments`),
        api.get(`/leaves/student/${studentId}`),
      ]);
      const profile = profileRes.data?.data;
      let materials = [];
      if (profile?.currentEnrollment?.classId) {
        const matRes = await api.get(`/homework/materials?classId=${profile.currentEnrollment.classId}`);
        materials = matRes.data?.data || [];
      }
      setChildData({
        profile,
        attendance: attRes.data?.data,
        homework: hwRes.data?.data || [],
        materials,
        fees: feeRes.data?.data || [],
        payments: payRes.data?.data || [],
        leaves: lvRes.data?.data || [],
      });
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setChildLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedId) loadChildDetails(selectedId);
  }, [selectedId, loadChildDetails]);

  useEffect(() => {
    if (selectedId && selectedExamId) {
      api.get(`/examinations/report-card?studentId=${selectedId}&examinationId=${selectedExamId}`)
        .then((res) => setReportCard(res.data?.data || null))
        .catch(() => setReportCard(null));
    }
  }, [selectedId, selectedExamId]);

  const submitLeave = async (e) => {
    e.preventDefault();
    try {
      await api.post('/leaves/student', { ...leaveForm, studentId: selectedId });
      toast.success('Leave application submitted for review');
      setLeaveModal(false);
      setLeaveForm({ startDate: '', endDate: '', reason: '' });
      loadChildDetails(selectedId);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={5} />;
  if (children.length === 0) {
    return <EmptyState title="No linked children found" description="Please contact the school office to link your child's admission record." />;
  }

  const activeChild = children.find((c) => c.studentId === selectedId) || children[0];
  const pendingFeeTotal = (childData?.fees || []).reduce((acc, f) => acc + Number(f.balanceAmount || 0), 0);

  return (
    <div>
      <PageHeader
        title="Parent Portal"
        subtitle="Switch between linked children to view attendance, report cards, homework, and fee receipts."
        actions={
          <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setLeaveModal(true)}>
            <Send size={14} className="me-1" /> Apply Student Leave
          </button>
        }
      />

      {/* Multi-Child Selector */}
      <div className="tgs-card p-3 mb-4">
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div className="small fw-bold text-muted d-flex align-items-center gap-2">
            <Users size={16} /> Linked Children ({children.length}):
          </div>
          <div className="d-flex flex-wrap gap-2">
            {children.map((c) => (
              <button
                key={c.studentId}
                type="button"
                onClick={() => setSelectedId(c.studentId)}
                className={`btn btn-sm ${c.studentId === selectedId ? 'tgs-btn-navy' : 'btn-outline-secondary'}`}
              >
                {c.fullName} ({c.className || '-'}-{c.sectionName || '-'})
              </button>
            ))}
          </div>
        </div>
      </div>

      {childLoading || !childData ? (
        <LoadingSkeleton rows={5} />
      ) : (
        <>
          <div className="row g-3 mb-4">
            <div className="col-sm-6 col-lg-3">
              <StatCard
                title="Active Student"
                value={activeChild.className ? `${activeChild.className} - ${activeChild.sectionName}` : '-'}
                subtitle={`Adm: ${activeChild.admissionNumber}`}
                icon={Users}
                color="primary"
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <StatCard
                title="Attendance Rate"
                value={`${childData.attendance?.attendancePercentage ?? 0}%`}
                subtitle={`${childData.attendance?.presentDays ?? 0} / ${childData.attendance?.totalDays ?? 0} days`}
                icon={CalendarCheck}
                color="green"
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <StatCard
                title="Homework Assigned"
                value={childData.homework.length}
                subtitle={`${childData.materials.length} study materials`}
                icon={BookOpen}
                color="navy"
              />
            </div>
            <div className="col-sm-6 col-lg-3">
              <StatCard
                title="Pending Fees"
                value={formatCurrency(pendingFeeTotal)}
                subtitle={`${childData.payments.length} receipt(s)`}
                icon={IndianRupee}
                color="gold"
              />
            </div>
          </div>

          <ul className="nav nav-tabs mb-3">
            {[
              ['overview', 'Profile & Remarks'],
              ['attendance', 'Attendance'],
              ['results', 'Exam Results & Report Card'],
              ['homework', 'Homework & Materials'],
              ['fees', 'Fees & Receipts'],
              ['leaves', 'Leave Requests'],
              ['notices', 'Announcements'],
            ].map(([key, label]) => (
              <li className="nav-item" key={key}>
                <button
                  type="button"
                  className={`nav-link small fw-semibold ${activeTab === key ? 'active' : ''}`}
                  onClick={() => setActiveTab(key)}
                >
                  {label}
                </button>
              </li>
            ))}
          </ul>

          {activeTab === 'overview' && (
            <div className="row g-4">
              <div className="col-lg-6">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3">Student Profile</h6>
                  <dl className="row small mb-0">
                    <dt className="col-5 text-muted">Full Name</dt><dd className="col-7 fw-semibold">{childData.profile.fullName}</dd>
                    <dt className="col-5 text-muted">Admission No</dt><dd className="col-7">{childData.profile.admissionNumber}</dd>
                    <dt className="col-5 text-muted">Class &amp; Section</dt><dd className="col-7">{childData.profile.currentEnrollment?.className} - {childData.profile.currentEnrollment?.sectionName} (Roll {childData.profile.currentEnrollment?.rollNumber})</dd>
                    <dt className="col-5 text-muted">Date of Birth</dt><dd className="col-7">{formatDate(childData.profile.dateOfBirth)}</dd>
                    <dt className="col-5 text-muted">Blood Group</dt><dd className="col-7">{childData.profile.bloodGroup || '-'}</dd>
                    <dt className="col-5 text-muted">Address</dt><dd className="col-7">{childData.profile.address || '-'}</dd>
                  </dl>
                </div>
              </div>
              <div className="col-lg-6">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3 d-flex align-items-center gap-2"><MessageSquare size={17} /> Teacher Remarks</h6>
                  {(childData.profile.teacherRemarks || []).length === 0 ? (
                    <p className="text-muted small mb-0">No teacher remarks recorded yet.</p>
                  ) : (
                    childData.profile.teacherRemarks.map((r) => (
                      <div key={r.id} className="p-2 border rounded mb-2 small">
                        <div className="d-flex justify-content-between">
                          <span className="fw-semibold">{r.teacherName}</span>
                          <span className="badge bg-light text-dark border">{r.remarkType}</span>
                        </div>
                        <div className="text-secondary mt-1">{r.remarkText}</div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'attendance' && (
            <div className="tgs-card p-4">
              <h6 className="fw-bold mb-3">Attendance History</h6>
              <div className="table-responsive">
                <table className="table tgs-table table-sm mb-0">
                  <thead><tr><th>Date</th><th>Session</th><th>Status</th><th>Remarks</th></tr></thead>
                  <tbody>
                    {(childData.attendance?.records || []).map((r) => (
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

          {activeTab === 'results' && (
            <div className="tgs-card p-4">
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div className="d-flex align-items-center gap-2">
                  <FileSpreadsheet size={18} className="text-primary" />
                  <select className="form-select form-select-sm" style={{ width: 260 }} value={selectedExamId} onChange={(e) => setSelectedExamId(e.target.value)}>
                    {exams.map((ex) => <option key={ex.id} value={ex.id}>{ex.name}</option>)}
                  </select>
                </div>
                {selectedExamId && (
                  <button
                    type="button"
                    className="btn tgs-btn-navy btn-sm d-inline-flex align-items-center gap-1"
                    onClick={() => downloadFile(`/examinations/report-card/pdf?studentId=${selectedId}&examinationId=${selectedExamId}`, `ReportCard-${activeChild.admissionNumber}.pdf`)}
                  >
                    <Download size={14} /> Download Report Card PDF
                  </button>
                )}
              </div>
              {!reportCard || (reportCard.subjects || []).length === 0 ? (
                <p className="text-muted small mb-0">No marks published for this examination yet.</p>
              ) : (
                <>
                  <div className="table-responsive">
                    <table className="table tgs-table table-sm">
                      <thead>
                        <tr>
                          <th>Subject</th>
                          {reportCard.activityGradedClass ? <th>Activity Rating</th> : <><th>Max</th><th>Obtained</th></>}
                          <th>Grade</th>
                          <th>Remarks</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reportCard.subjects.map((m) => (
                          <tr key={m.id}>
                            <td className="fw-semibold">{m.subjectName}</td>
                            {reportCard.activityGradedClass ? (
                              <td>{m.activityRating || '-'}</td>
                            ) : (
                              <><td>{m.maxMarks}</td><td>{m.absent ? 'AB' : m.marksObtained}</td></>
                            )}
                            <td><span className="badge bg-primary-subtle text-primary">{m.grade}</span></td>
                            <td>{m.descriptiveRemarks || '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <div className="p-3 bg-light rounded d-flex flex-wrap gap-4 small fw-semibold">
                    {!reportCard.activityGradedClass && (
                      <>
                        <span>Total: {reportCard.totalMarksObtained} / {reportCard.totalMaxMarks}</span>
                        <span>Percentage: {reportCard.percentage}%</span>
                      </>
                    )}
                    <span>Overall Grade: {reportCard.overallGrade}</span>
                    <span>Status: <StatusBadge status={reportCard.resultStatus} /></span>
                  </div>
                </>
              )}
            </div>
          )}

          {activeTab === 'homework' && (
            <div className="row g-4">
              <div className="col-lg-7">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3">Homework Assignments</h6>
                  {childData.homework.length === 0 ? <p className="text-muted small mb-0">No homework assigned.</p> : (
                    childData.homework.map((hw) => (
                      <div key={hw.id} className="p-3 border rounded mb-2">
                        <div className="d-flex justify-content-between align-items-center mb-1">
                          <span className="badge bg-primary-subtle text-primary">{hw.subjectName}</span>
                          <small className="text-muted">Due: {formatDate(hw.dueDate)}</small>
                        </div>
                        <div className="fw-semibold">{hw.title}</div>
                        <div className="small text-muted">{hw.description}</div>
                        {hw.mySubmission && <div className="mt-1"><StatusBadge status={hw.mySubmission.status} /></div>}
                      </div>
                    ))
                  )}
                </div>
              </div>
              <div className="col-lg-5">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3">Study Materials</h6>
                  {childData.materials.length === 0 ? <p className="text-muted small mb-0">No study materials uploaded.</p> : (
                    childData.materials.map((m) => (
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
            </div>
          )}

          {activeTab === 'fees' && (
            <div className="row g-4">
              <div className="col-lg-7">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3">Fee Ledger</h6>
                  <div className="table-responsive">
                    <table className="table tgs-table table-sm mb-0">
                      <thead><tr><th>Fee</th><th>Net</th><th>Paid</th><th>Balance</th><th>Status</th></tr></thead>
                      <tbody>
                        {childData.fees.map((f) => (
                          <tr key={f.id}>
                            <td className="fw-semibold">{f.feeName}</td>
                            <td>{formatCurrency(f.netAmount)}</td>
                            <td>{formatCurrency(f.paidAmount)}</td>
                            <td className="fw-semibold">{formatCurrency(f.balanceAmount)}</td>
                            <td><StatusBadge status={f.status} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
              <div className="col-lg-5">
                <div className="tgs-card p-4 h-100">
                  <h6 className="fw-bold mb-3">Payment Receipts</h6>
                  {childData.payments.length === 0 ? <p className="text-muted small mb-0">No payments recorded yet.</p> : (
                    childData.payments.map((p) => (
                      <div key={p.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center">
                        <div className="small">
                          <div className="fw-semibold">{p.receiptNumber}</div>
                          <div className="text-muted">{formatCurrency(p.amount)} • {p.paymentMode} • {formatDate(p.paidAt)}</div>
                        </div>
                        <button type="button" className="btn btn-sm btn-outline-primary"
                          onClick={() => downloadFile(`/fees/payments/${p.id}/receipt`, `${p.receiptNumber}.pdf`)}>
                          <Download size={14} /> PDF
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'leaves' && (
            <div className="tgs-card p-4">
              <h6 className="fw-bold mb-3">Leave Applications</h6>
              {childData.leaves.length === 0 ? <p className="text-muted small mb-0">No leave requests submitted.</p> : (
                <div className="table-responsive">
                  <table className="table tgs-table table-sm mb-0">
                    <thead><tr><th>Dates</th><th>Reason</th><th>Status</th><th>Review Remarks</th></tr></thead>
                    <tbody>
                      {childData.leaves.map((l) => (
                        <tr key={l.id}>
                          <td>{formatDate(l.startDate)} – {formatDate(l.endDate)}</td>
                          <td>{l.reason}</td>
                          <td><StatusBadge status={l.status} /></td>
                          <td>{l.reviewRemarks || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === 'notices' && (
            <div className="tgs-card p-4">
              <h6 className="fw-bold mb-3">School Announcements</h6>
              {announcements.map((a) => (
                <div key={a.id} className="p-3 border rounded mb-2">
                  <div className="d-flex justify-content-between"><span className="fw-semibold">{a.title}</span><small className="text-muted">{formatDate(a.publishedAt)}</small></div>
                  <div className="small text-secondary mt-1">{a.content}</div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      <Modal show={leaveModal} title={`Apply Leave for ${activeChild.fullName}`} onClose={() => setLeaveModal(false)} size="md">
        <form onSubmit={submitLeave} className="row g-3">
          <div className="col-6">
            <label className="form-label small fw-semibold">Start Date *</label>
            <input type="date" required className="form-control" value={leaveForm.startDate}
              onChange={(e) => setLeaveForm({ ...leaveForm, startDate: e.target.value })} />
          </div>
          <div className="col-6">
            <label className="form-label small fw-semibold">End Date *</label>
            <input type="date" required className="form-control" value={leaveForm.endDate}
              onChange={(e) => setLeaveForm({ ...leaveForm, endDate: e.target.value })} />
          </div>
          <div className="col-12">
            <label className="form-label small fw-semibold">Reason *</label>
            <textarea rows={3} required className="form-control" value={leaveForm.reason}
              onChange={(e) => setLeaveForm({ ...leaveForm, reason: e.target.value })} />
          </div>
          <div className="col-12 text-end">
            <button type="submit" className="btn tgs-btn-navy btn-sm">Submit Request</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
