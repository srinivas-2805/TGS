import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, CalendarCheck, ClipboardList, Clock, FileSpreadsheet, Megaphone } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, StatCard, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

export default function TeacherDashboardPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [assignments, setAssignments] = useState([]);
  const [timetable, setTimetable] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [myLeaves, setMyLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [leaveModal, setLeaveModal] = useState(false);
  const [leaveForm, setLeaveForm] = useState({ startDate: '', endDate: '', reason: '' });

  const loadData = async () => {
    setLoading(true);
    try {
      const [aRes, ttRes, annRes, lvRes] = await Promise.all([
        api.get('/teachers/my-assignments'),
        api.get('/timetables/teacher/my'),
        api.get('/announcements'),
        api.get('/leaves/teacher/my'),
      ]);
      setAssignments(aRes.data?.data || []);
      setTimetable(ttRes.data?.data || []);
      setAnnouncements(annRes.data?.data || []);
      setMyLeaves(lvRes.data?.data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const submitLeave = async (e) => {
    e.preventDefault();
    try {
      await api.post('/leaves/teacher', leaveForm);
      toast.success('Teacher leave application submitted');
      setLeaveModal(false);
      setLeaveForm({ startDate: '', endDate: '', reason: '' });
      loadData();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  const uniqueSections = new Set(assignments.map((a) => a.sectionId)).size;

  return (
    <div>
      <PageHeader
        title={`Welcome, ${user?.fullName}`}
        subtitle="Teacher Portal — Assigned Classes, Attendance, Homework, Marks & Timetable"
        actions={
          <>
            <Link to="/portal/attendance" className="btn tgs-btn-navy btn-sm">Mark Attendance</Link>
            <Link to="/portal/homework" className="btn btn-outline-primary btn-sm">Assign Homework</Link>
            <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setLeaveModal(true)}>
              Apply for Leave
            </button>
          </>
        }
      />

      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Assigned Sections" value={uniqueSections} icon={BookOpen} color="primary" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Subject Allocations" value={assignments.length} icon={FileSpreadsheet} color="navy" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Weekly Periods" value={timetable.length} icon={Clock} color="gold" />
        </div>
        <div className="col-sm-6 col-lg-3">
          <StatCard title="Leave Requests" value={myLeaves.length} icon={CalendarCheck} color="green" />
        </div>
      </div>

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">My Assigned Classes &amp; Subjects</h6>
            {assignments.length === 0 ? (
              <p className="text-muted small mb-0">No class/subject assignments found for current academic year.</p>
            ) : (
              <div className="table-responsive">
                <table className="table tgs-table table-sm mb-0">
                  <thead>
                    <tr>
                      <th>Class &amp; Section</th>
                      <th>Subject</th>
                      <th>Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {assignments.map((a) => (
                      <tr key={a.id}>
                        <td className="fw-semibold">{a.className} - {a.sectionName}</td>
                        <td>{a.subjectName} ({a.subjectCode})</td>
                        <td>{a.classTeacher ? <span className="badge bg-warning-subtle text-dark">Class Teacher</span> : 'Subject Teacher'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="mt-3 d-flex flex-wrap gap-2">
              <Link to="/portal/examinations" className="btn btn-sm btn-outline-primary">Enter Exam Marks</Link>
              <Link to="/portal/students" className="btn btn-sm btn-outline-secondary">View Students &amp; Add Remarks</Link>
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">My Teaching Schedule</h6>
            {timetable.length === 0 ? (
              <p className="text-muted small mb-0">No timetable periods assigned yet.</p>
            ) : (
              <div className="table-responsive">
                <table className="table tgs-table table-sm mb-0">
                  <thead>
                    <tr>
                      <th>Day</th>
                      <th>Period</th>
                      <th>Class</th>
                      <th>Subject</th>
                      <th>Room</th>
                    </tr>
                  </thead>
                  <tbody>
                    {timetable.map((slot) => (
                      <tr key={slot.id}>
                        <td className="fw-semibold">{slot.dayOfWeek}</td>
                        <td>P{slot.periodNumber} ({slot.startTime?.slice(0, 5)}–{slot.endTime?.slice(0, 5)})</td>
                        <td>{slot.className}-{slot.sectionName}</td>
                        <td>{slot.subjectName}</td>
                        <td>{slot.roomNumber || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3 d-flex align-items-center gap-2"><Megaphone size={17} /> School Announcements</h6>
            <div className="d-flex flex-column gap-2">
              {announcements.slice(0, 5).map((a) => (
                <div key={a.id} className="p-2 border rounded">
                  <div className="fw-semibold small">{a.title}</div>
                  <div className="small text-muted">{a.content}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3 d-flex align-items-center gap-2"><ClipboardList size={17} /> My Leave History</h6>
            {myLeaves.length === 0 ? (
              <p className="text-muted small mb-0">No leave applications submitted.</p>
            ) : (
              <div className="d-flex flex-column gap-2">
                {myLeaves.map((l) => (
                  <div key={l.id} className="p-2 border rounded d-flex justify-content-between align-items-center">
                    <div>
                      <div className="small fw-semibold">{formatDate(l.startDate)} to {formatDate(l.endDate)}</div>
                      <div className="small text-muted">{l.reason}</div>
                    </div>
                    <StatusBadge status={l.status} />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <Modal show={leaveModal} title="Apply for Teacher Leave" onClose={() => setLeaveModal(false)} size="md">
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
            <button type="submit" className="btn tgs-btn-navy btn-sm">Submit Leave Request</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
