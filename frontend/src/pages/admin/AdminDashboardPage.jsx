import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
  LineChart, Line, AreaChart, Area
} from 'recharts';
import {
  GraduationCap, Users, Briefcase, School, CalendarCheck, IndianRupee,
  AlertCircle, FileSpreadsheet, UserPlus, Megaphone
} from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { PageHeader, StatCard, LoadingSkeleton, StatusBadge } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

export default function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/reports/dashboard')
      .then((res) => setData(res.data?.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSkeleton rows={6} />;
  if (error) return <div className="alert alert-danger">{error}</div>;
  if (!data) return null;

  return (
    <div>
      <PageHeader
        title="Administration Dashboard"
        subtitle="Real-time overview of Telangana Grammar School (LKG – 10th Class)"
        actions={
          <>
            <Link to="/portal/students" className="btn tgs-btn-navy btn-sm d-inline-flex align-items-center gap-1">
              <UserPlus size={15} /> Admit Student
            </Link>
            <Link to="/portal/communication" className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-1">
              <Megaphone size={15} /> New Announcement
            </Link>
          </>
        }
      />

      {/* Primary KPIs */}
      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-xl-3">
          <StatCard title="Total Active Students" value={data.totalStudents} icon={GraduationCap} color="primary" subtitle={`${data.totalClasses} classes • ${data.totalSections} sections`} />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard title="Teachers & Staff" value={`${data.totalTeachers} / ${data.totalStaff}`} icon={Briefcase} color="navy" subtitle="Teaching / Non-teaching" />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard
            title="Today's Attendance"
            value={`${data.todayPresent} Present`}
            icon={CalendarCheck}
            color="green"
            subtitle={`${data.todayAbsent} absent • ${data.todayLate} late • ${data.todayLeave} leave`}
          />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard
            title="Fee Collection / Pending"
            value={formatCurrency(data.totalFeeCollected)}
            icon={IndianRupee}
            color="gold"
            subtitle={`Pending: ${formatCurrency(data.pendingFees)}`}
          />
        </div>
      </div>

      {/* Secondary KPI strip */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <div className="tgs-card p-3 d-flex justify-content-between align-items-center">
            <div>
              <div className="text-muted small">Upcoming / Ongoing Exams</div>
              <div className="fw-bold fs-5">{data.upcomingExaminations}</div>
            </div>
            <FileSpreadsheet size={22} className="text-primary" />
          </div>
        </div>
        <div className="col-md-4">
          <div className="tgs-card p-3 d-flex justify-content-between align-items-center">
            <div>
              <div className="text-muted small">Total Classes &amp; Sections</div>
              <div className="fw-bold fs-5">{data.totalClasses} Classes • {data.totalSections} Sections</div>
            </div>
            <School size={22} className="text-success" />
          </div>
        </div>
        <div className="col-md-4">
          <div className="tgs-card p-3 d-flex justify-content-between align-items-center">
            <div>
              <div className="text-muted small">Library Books / Issued</div>
              <div className="fw-bold fs-5">{data.totalLibraryBooks} Titles • {data.issuedBooksCount} Issued</div>
            </div>
            <Users size={22} className="text-warning" />
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="row g-4 mb-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Class-wise Student Distribution</h6>
            <div style={{ width: '100%', height: 260 }}>
              <ResponsiveContainer>
                <BarChart data={data.classWiseDistribution || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="count" name="Students" fill="#1d4ed8" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Monthly Fee Collection (INR)</h6>
            <div style={{ width: '100%', height: 260 }}>
              <ResponsiveContainer>
                <AreaChart data={data.monthlyFeeCollection || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(v) => formatCurrency(v)} />
                  <Area type="monotone" dataKey="collected" name="Collected" stroke="#0f294a" fill="#dbeafe" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Monthly Attendance Rate (%)</h6>
            <div style={{ width: '100%', height: 240 }}>
              <ResponsiveContainer>
                <LineChart data={data.monthlyAttendance || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="month" tick={{ fontSize: 11 }} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Line type="monotone" dataKey="percentage" name="Attendance %" stroke="#10b981" strokeWidth={2.5} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Latest Exam Performance (Subject Avg %) &amp; Admissions</h6>
            <div style={{ width: '100%', height: 240 }}>
              <ResponsiveContainer>
                <BarChart data={(data.examPerformance?.length ? data.examPerformance : data.admissionStatistics) || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey={data.examPerformance?.length ? 'subject' : 'label'} tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar
                    dataKey={data.examPerformance?.length ? 'averagePercentage' : 'count'}
                    name={data.examPerformance?.length ? 'Avg %' : 'Admissions'}
                    fill="#f59e0b"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Admissions, Upcoming Exams & Announcements */}
      <div className="row g-4">
        <div className="col-lg-4">
          <div className="tgs-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">Recent Admissions</h6>
              <Link to="/portal/students" className="small text-decoration-none">View All</Link>
            </div>
            {(data.recentAdmissions || []).length === 0 ? (
              <div className="text-muted small">No admissions recorded.</div>
            ) : (
              <div className="d-flex flex-column gap-2">
                {data.recentAdmissions.map((s) => (
                  <Link key={s.id} to={`/portal/students/${s.id}`} className="text-decoration-none text-dark p-2 rounded border d-flex justify-content-between align-items-center">
                    <div>
                      <div className="fw-semibold small">{s.fullName}</div>
                      <div className="text-muted" style={{ fontSize: '0.73rem' }}>{s.admissionNumber} • {formatDate(s.admissionDate)}</div>
                    </div>
                    <StatusBadge status={s.status} />
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-4">
          <div className="tgs-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">Upcoming Examinations</h6>
              <Link to="/portal/examinations" className="small text-decoration-none">Manage</Link>
            </div>
            {(data.upcomingExamsList || []).length === 0 ? (
              <div className="text-muted small">No upcoming examinations scheduled.</div>
            ) : (
              <div className="d-flex flex-column gap-2">
                {data.upcomingExamsList.map((ex) => (
                  <div key={ex.id} className="p-2 rounded border d-flex justify-content-between align-items-center">
                    <div>
                      <div className="fw-semibold small">{ex.name}</div>
                      <div className="text-muted" style={{ fontSize: '0.73rem' }}>{formatDate(ex.startDate)} – {formatDate(ex.endDate)}</div>
                    </div>
                    <StatusBadge status={ex.status} />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-4">
          <div className="tgs-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">Recent Announcements</h6>
              <Link to="/portal/communication" className="small text-decoration-none">View All</Link>
            </div>
            {(data.recentAnnouncements || []).length === 0 ? (
              <div className="text-muted small">No announcements published.</div>
            ) : (
              <div className="d-flex flex-column gap-2">
                {data.recentAnnouncements.map((a) => (
                  <div key={a.id} className="p-2 rounded border">
                    <div className="fw-semibold small">{a.title}</div>
                    <div className="text-muted" style={{ fontSize: '0.73rem' }}>
                      {a.category} • Audience: {a.targetAudience} • {formatDate(a.publishedAt)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {data.todayMarked === 0 && (
        <div className="alert alert-info mt-4 small d-flex align-items-center gap-2 mb-0">
          <AlertCircle size={17} />
          <span>No student attendance has been marked for today yet. Teachers or Administrators can mark daily attendance from the Attendance module.</span>
        </div>
      )}
    </div>
  );
}
