import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ErrorBoundary } from './components/CommonUI';
import ProtectedRoute from './components/ProtectedRoute';

import PublicLayout from './layouts/PublicLayout';
import PortalLayout from './layouts/PortalLayout';

import {
  HomePage, AboutPage, PrincipalMessagePage, VisionMissionPage, AcademicsPage as PublicAcademicsPage,
  ClassesPage, MethodologyPage, FacilitiesPage, GalleryPage, EventsPage, AchievementsPage,
  AdmissionEnquiryPage, ContactPage, NoticesPage,
} from './pages/public/PublicPages';
import { LoginPage, ForgotPasswordPage, ResetPasswordPage } from './pages/auth/AuthPages';

import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import StudentsPage from './pages/admin/StudentsPage';
import StudentDetailPage from './pages/admin/StudentDetailPage';
import PromoteStudentsPage from './pages/admin/PromoteStudentsPage';
import { TeachersPage, ParentsPage, UsersPage } from './pages/admin/TeachersAndParentsPages';
import { AcademicsPage, TimetablePage } from './pages/admin/AcademicsAndTimetablePages';
import AttendanceAndLeavePage from './pages/admin/AttendanceAndLeavePage';
import ExaminationsPage from './pages/admin/ExaminationsPage';
import FeesPage from './pages/admin/FeesPage';
import HomeworkPage from './pages/admin/HomeworkPage';
import { TransportPage, LibraryPage, CommunicationPage, CmsPage, ReportsPage } from './pages/admin/OperationsPages';

import TeacherDashboardPage from './pages/teacher/TeacherDashboardPage';
import ParentDashboardPage from './pages/parent/ParentDashboardPage';
import StudentDashboardPage from './pages/student/StudentDashboardPage';
import { AccountantDashboardPage, LibrarianDashboardPage, ProfileSettingsPage } from './pages/portal/RoleSupportPages';

function PortalHomeRedirect() {
  const { defaultRoute } = useAuth();
  return <Navigate to={defaultRoute} replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      {/* Public School Website */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/principal-message" element={<PrincipalMessagePage />} />
        <Route path="/vision-mission" element={<VisionMissionPage />} />
        <Route path="/academics" element={<PublicAcademicsPage />} />
        <Route path="/classes" element={<ClassesPage />} />
        <Route path="/methodology" element={<MethodologyPage />} />
        <Route path="/facilities" element={<FacilitiesPage />} />
        <Route path="/gallery" element={<GalleryPage />} />
        <Route path="/events" element={<EventsPage />} />
        <Route path="/achievements" element={<AchievementsPage />} />
        <Route path="/admission-enquiry" element={<AdmissionEnquiryPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/notices" element={<NoticesPage />} />
      </Route>

      {/* Authentication */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Protected ERP Portal */}
      <Route path="/portal" element={<ProtectedRoute><PortalLayout /></ProtectedRoute>}>
        <Route index element={<PortalHomeRedirect />} />
        <Route path="admin" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><AdminDashboardPage /></ProtectedRoute>} />
        <Route path="teacher" element={<ProtectedRoute allowedRoles={['TEACHER']}><TeacherDashboardPage /></ProtectedRoute>} />
        <Route path="parent" element={<ProtectedRoute allowedRoles={['PARENT']}><ParentDashboardPage /></ProtectedRoute>} />
        <Route path="student" element={<ProtectedRoute allowedRoles={['STUDENT']}><StudentDashboardPage /></ProtectedRoute>} />
        <Route path="accountant" element={<ProtectedRoute allowedRoles={['ACCOUNTANT']}><AccountantDashboardPage /></ProtectedRoute>} />
        <Route path="librarian" element={<ProtectedRoute allowedRoles={['LIBRARIAN']}><LibrarianDashboardPage /></ProtectedRoute>} />

        <Route path="students" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER', 'ACCOUNTANT', 'LIBRARIAN']}><StudentsPage /></ProtectedRoute>} />
        <Route path="students/promote" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><PromoteStudentsPage /></ProtectedRoute>} />
        <Route path="students/:id" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER', 'ACCOUNTANT', 'LIBRARIAN']}><StudentDetailPage /></ProtectedRoute>} />

        <Route path="teachers" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><TeachersPage /></ProtectedRoute>} />
        <Route path="parents" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><ParentsPage /></ProtectedRoute>} />
        <Route path="users" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><UsersPage /></ProtectedRoute>} />

        <Route path="academics" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><AcademicsPage /></ProtectedRoute>} />
        <Route path="timetables" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER']}><TimetablePage /></ProtectedRoute>} />
        <Route path="attendance" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER']}><AttendanceAndLeavePage /></ProtectedRoute>} />
        <Route path="examinations" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER']}><ExaminationsPage /></ProtectedRoute>} />
        <Route path="homework" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER']}><HomeworkPage /></ProtectedRoute>} />
        <Route path="fees" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'ACCOUNTANT']}><FeesPage /></ProtectedRoute>} />
        <Route path="transport" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'ACCOUNTANT']}><TransportPage /></ProtectedRoute>} />
        <Route path="library" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'LIBRARIAN']}><LibraryPage /></ProtectedRoute>} />
        <Route path="communication" element={<CommunicationPage />} />
        <Route path="cms" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN']}><CmsPage /></ProtectedRoute>} />
        <Route path="reports" element={<ProtectedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'ACCOUNTANT', 'LIBRARIAN']}><ReportsPage /></ProtectedRoute>} />
        <Route path="profile" element={<ProfileSettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </ErrorBoundary>
  );
}
