import React, { useState, useEffect, useCallback } from 'react';
import { NavLink, Outlet, Link, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, GraduationCap, UserCheck, CalendarCheck, BookOpen,
  FileSpreadsheet, IndianRupee, Clock, Bus, Library, Megaphone, Globe, BarChart3,
  Bell, Menu, LogOut, UserCog, Briefcase, ClipboardList, CheckCheck, KeyRound, Home
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import api from '../services/api';
import { SchoolLogo } from '../components/CommonUI';
import { ROLE_LABELS, formatDateTime } from '../utils/formatters';

export default function PortalLayout() {
  const { user, logout, hasAnyRole } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [notifOpen, setNotifOpen] = useState(false);

  const loadNotifications = useCallback(async () => {
    try {
      const [cntRes, listRes] = await Promise.all([
        api.get('/notifications/unread-count'),
        api.get('/notifications/my?page=0&size=8'),
      ]);
      setUnreadCount(cntRes.data?.data || 0);
      setNotifications(listRes.data?.data?.content || []);
    } catch {
      // ignore silent poll errors
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const markOneRead = async (id) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      loadNotifications();
    } catch {
      // ignore
    }
  };

  const markAllRead = async () => {
    try {
      await api.patch('/notifications/read-all');
      loadNotifications();
      toast.success('All notifications marked as read');
    } catch {
      // ignore
    }
  };

  const handleLogout = async () => {
    await logout();
    toast.info('Signed out safely');
    navigate('/login');
  };

  const isSchoolAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');
  const isTeacher = hasAnyRole('TEACHER');
  const isAccountant = hasAnyRole('ACCOUNTANT');
  const isLibrarian = hasAnyRole('LIBRARIAN');
  const isParent = hasAnyRole('PARENT');
  const isStudent = hasAnyRole('STUDENT');

  const navGroups = [];

  if (isSchoolAdmin) {
    navGroups.push({
      label: 'Overview',
      items: [
        { to: '/portal/admin', label: 'Admin Dashboard', icon: LayoutDashboard },
        { to: '/portal/reports', label: 'Reports & Analytics', icon: BarChart3 },
      ],
    });
    navGroups.push({
      label: 'People & Academics',
      items: [
        { to: '/portal/students', label: 'Students', icon: GraduationCap },
        { to: '/portal/students/promote', label: 'Promote Students', icon: UserCheck },
        { to: '/portal/teachers', label: 'Teachers & Staff', icon: Briefcase },
        { to: '/portal/parents', label: 'Parents', icon: Users },
        { to: '/portal/academics', label: 'Classes & Subjects', icon: BookOpen },
        { to: '/portal/timetables', label: 'Timetable', icon: Clock },
      ],
    });
    navGroups.push({
      label: 'Operations',
      items: [
        { to: '/portal/attendance', label: 'Attendance & Leaves', icon: CalendarCheck },
        { to: '/portal/examinations', label: 'Examinations', icon: FileSpreadsheet },
        { to: '/portal/homework', label: 'Homework & Materials', icon: ClipboardList },
        { to: '/portal/fees', label: 'Fees & Receipts', icon: IndianRupee },
        { to: '/portal/transport', label: 'Transport', icon: Bus },
        { to: '/portal/library', label: 'Library', icon: Library },
      ],
    });
    navGroups.push({
      label: 'Communication & CMS',
      items: [
        { to: '/portal/communication', label: 'Announcements', icon: Megaphone },
        { to: '/portal/cms', label: 'Website CMS & Enquiries', icon: Globe },
        { to: '/portal/users', label: 'User Accounts & Roles', icon: UserCog },
      ],
    });
  } else if (isTeacher) {
    navGroups.push({
      label: 'Teacher Workspace',
      items: [
        { to: '/portal/teacher', label: 'Teacher Dashboard', icon: LayoutDashboard },
        { to: '/portal/students', label: 'My Students', icon: GraduationCap },
        { to: '/portal/attendance', label: 'Attendance & Leaves', icon: CalendarCheck },
        { to: '/portal/homework', label: 'Homework & Materials', icon: ClipboardList },
        { to: '/portal/examinations', label: 'Marks & Report Cards', icon: FileSpreadsheet },
        { to: '/portal/timetables', label: 'My Timetable', icon: Clock },
        { to: '/portal/communication', label: 'Announcements', icon: Megaphone },
      ],
    });
  } else if (isAccountant) {
    navGroups.push({
      label: 'Finance Workspace',
      items: [
        { to: '/portal/accountant', label: 'Finance Dashboard', icon: LayoutDashboard },
        { to: '/portal/fees', label: 'Fee Management', icon: IndianRupee },
        { to: '/portal/transport', label: 'Transport Fees', icon: Bus },
        { to: '/portal/reports', label: 'Fee Reports', icon: BarChart3 },
        { to: '/portal/communication', label: 'Announcements', icon: Megaphone },
      ],
    });
  } else if (isLibrarian) {
    navGroups.push({
      label: 'Library Workspace',
      items: [
        { to: '/portal/librarian', label: 'Library Dashboard', icon: LayoutDashboard },
        { to: '/portal/library', label: 'Catalog & Circulation', icon: Library },
        { to: '/portal/reports', label: 'Library Reports', icon: BarChart3 },
        { to: '/portal/communication', label: 'Announcements', icon: Megaphone },
      ],
    });
  } else if (isParent) {
    navGroups.push({
      label: 'Parent Portal',
      items: [
        { to: '/portal/parent', label: 'Children Dashboard', icon: LayoutDashboard },
        { to: '/portal/communication', label: 'Notices & Inbox', icon: Megaphone },
      ],
    });
  } else if (isStudent) {
    navGroups.push({
      label: 'Student Portal',
      items: [
        { to: '/portal/student', label: 'My Dashboard', icon: LayoutDashboard },
        { to: '/portal/communication', label: 'Announcements', icon: Megaphone },
      ],
    });
  }

  navGroups.push({
    label: 'Account',
    items: [
      { to: '/portal/profile', label: 'Profile & Password', icon: KeyRound },
    ],
  });

  const primaryRole = user?.roles?.[0] ? ROLE_LABELS[user.roles[0]] || user.roles[0] : 'User';

  return (
    <div className="tgs-portal-shell">
      {sidebarOpen && <div className="tgs-sidebar-backdrop d-lg-none" onClick={() => setSidebarOpen(false)} />}

      {/* Sidebar */}
      <aside className={`tgs-sidebar p-3 ${sidebarOpen ? 'open' : ''}`}>
        <div className="d-flex align-items-center gap-2 px-2 pb-3 mb-2 border-bottom border-white border-opacity-10">
          <SchoolLogo variant="compact" height={38} />
          <div className="overflow-hidden">
            <div className="fw-bold text-white text-truncate" style={{ fontSize: '0.92rem' }}>TGS School ERP</div>
            <div className="text-warning text-truncate" style={{ fontSize: '0.7rem' }}>Kalluru, Khammam</div>
          </div>
        </div>

        <nav className="nav flex-column flex-grow-1 overflow-auto pe-1">
          {navGroups.map((group) => (
            <div key={group.label}>
              <div className="tgs-sidebar-section">{group.label}</div>
              {group.items.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === '/portal/students' || item.to === '/portal/admin'}
                    onClick={() => setSidebarOpen(false)}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    <Icon size={17} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="pt-3 mt-2 border-top border-white border-opacity-10 d-flex flex-column gap-2">
          <Link to="/" className="btn btn-outline-light btn-sm w-100 d-flex align-items-center justify-content-center gap-2">
            <Home size={15} /> Public Website
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="btn btn-warning btn-sm w-100 fw-semibold d-flex align-items-center justify-content-center gap-2"
          >
            <LogOut size={15} /> Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-grow-1 d-flex flex-column min-w-0 tgs-portal-main">
        <header className="tgs-topbar px-3 px-lg-4 d-flex align-items-center justify-content-between gap-3">
          <div className="d-flex align-items-center gap-2">
            <button
              type="button"
              className="btn btn-light btn-sm d-lg-none"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open Navigation"
            >
              <Menu size={20} />
            </button>
            <div className="d-none d-sm-block">
              <span className="badge bg-primary-subtle text-primary me-2">{primaryRole}</span>
              <span className="text-muted small">Telangana Grammar School Management Platform</span>
            </div>
          </div>

          <div className="d-flex align-items-center gap-3 position-relative">
            <button
              type="button"
              className="btn btn-light btn-sm position-relative rounded-circle p-2"
              onClick={() => setNotifOpen(!notifOpen)}
              aria-label="Notifications"
            >
              <Bell size={18} />
              {unreadCount > 0 && (
                <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style={{ fontSize: '0.65rem' }}>
                  {unreadCount}
                </span>
              )}
            </button>

            {notifOpen && (
              <div
                className="position-absolute end-0 top-100 mt-2 bg-white rounded-3 shadow-lg border p-3"
                style={{ width: 330, zIndex: 1050 }}
              >
                <div className="d-flex justify-content-between align-items-center pb-2 mb-2 border-bottom">
                  <span className="fw-bold small">Notifications ({unreadCount} unread)</span>
                  <button type="button" className="btn btn-link btn-sm p-0 text-decoration-none" onClick={markAllRead}>
                    <CheckCheck size={14} className="me-1" />Mark all read
                  </button>
                </div>
                <div style={{ maxHeight: 280, overflowY: 'auto' }} className="d-flex flex-column gap-2">
                  {notifications.length === 0 ? (
                    <div className="text-muted small text-center py-3">No notifications yet</div>
                  ) : (
                    notifications.map((n) => (
                      <div
                        key={n.id}
                        onClick={() => !n.read && markOneRead(n.id)}
                        className={`p-2 rounded-2 small border ${n.read ? 'bg-white text-muted' : 'bg-primary-subtle border-primary-subtle'}`}
                        style={{ cursor: 'pointer' }}
                      >
                        <div className="fw-semibold text-dark">{n.title}</div>
                        <div>{n.message}</div>
                        <div className="text-muted" style={{ fontSize: '0.7rem' }}>{formatDateTime(n.createdAt)}</div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            <Link to="/portal/profile" className="text-decoration-none text-dark d-flex align-items-center gap-2">
              <div
                className="rounded-circle bg-primary text-white fw-bold d-flex align-items-center justify-content-center"
                style={{ width: 34, height: 34, fontSize: '0.85rem' }}
              >
                {(user?.fullName || 'U').charAt(0).toUpperCase()}
              </div>
              <div className="d-none d-md-block lh-sm">
                <div className="small fw-semibold">{user?.fullName}</div>
                <div className="text-muted" style={{ fontSize: '0.72rem' }}>@{user?.username}</div>
              </div>
            </Link>
          </div>
        </header>

        <div className="p-3 p-lg-4 flex-grow-1">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
