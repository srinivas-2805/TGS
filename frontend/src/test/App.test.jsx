import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import api from '../services/api';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { AppRoutes } from '../App';
import { clearPublicOverviewCache } from '../hooks/usePublicOverview';

vi.mock('../services/api', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    },
  };
});

function renderAt(route) {
  return render(
    <ToastProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={[route]}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    </ToastProvider>
  );
}

describe('Telangana Grammar School Frontend Suite', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    clearPublicOverviewCache();
    localStorage.clear();
  });

  it('renders Public Website homepage with CMS data and school location', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        data: {
          content: [
            { sectionKey: 'HOME_HERO', title: 'Nurturing Knowledge & Character', bodyContent: 'LKG to 10th Class in Kalluru', metadataJson: '{}' },
            { sectionKey: 'ABOUT_US', title: 'About Telangana Grammar School', bodyContent: 'Holistic education', metadataJson: '{}' },
            { sectionKey: 'CONTACT_INFO', title: 'Contact', bodyContent: 'Office details', metadataJson: '{"address":"Kalluru, Khammam, Telangana"}' },
          ],
          announcements: [{ id: 1, title: 'Session Open', content: '2026-27 active', category: 'GENERAL', publishedAt: '2026-06-01T00:00:00Z' }],
          events: [],
          gallery: [],
          classes: [{ id: 1, name: 'LKG', levelOrder: 1, category: 'PRE_PRIMARY', activityGraded: true }],
        },
      },
    });

    renderAt('/');

    await waitFor(() => {
      expect(screen.getByText('Nurturing Knowledge & Character')).toBeInTheDocument();
    });
    expect(screen.getAllByText(/Kalluru, Khammam/i).length).toBeGreaterThan(0);
    expect(screen.getByText('Session Open')).toBeInTheDocument();
  });

  it('validates and submits Admission Enquiry form to backend', async () => {
    api.get.mockResolvedValue({ data: { data: { content: [], announcements: [], events: [], gallery: [], classes: [{ id: 1, name: '5th Class' }] } } });
    api.post.mockResolvedValueOnce({ data: { success: true, data: { id: 10 } } });

    renderAt('/admission-enquiry');

    fireEvent.click(screen.getByRole('button', { name: /Submit Enquiry/i }));
    await waitFor(() => {
      expect(screen.getByText(/Student name is required/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText('Enter student name'), { target: { value: 'Teja Test' } });
    fireEvent.change(screen.getByPlaceholderText('Enter parent name'), { target: { value: 'Rao Test' } });
    fireEvent.change(screen.getByPlaceholderText('10-digit mobile number'), { target: { value: '9000000999' } });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '5th Class' } });

    fireEvent.click(screen.getByRole('button', { name: /Submit Enquiry/i }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/public/enquiries', expect.objectContaining({
        studentName: 'Teja Test',
        parentName: 'Rao Test',
        applyingClassName: '5th Class',
      }));
      expect(screen.getByText(/enquiry has been received/i)).toBeInTheDocument();
    });
  });

  it('redirects unauthenticated portal access to Login and logs in user', async () => {
    api.post.mockResolvedValueOnce({
      data: {
        data: {
          accessToken: 'jwt-123',
          refreshToken: 'ref-123',
          user: { id: 1, username: 'admin', fullName: 'Ramesh Babu', roles: ['ADMIN'] },
        },
      },
    });
    api.get.mockResolvedValue({
      data: {
        data: {
          totalStudents: 4, totalTeachers: 3, totalStaff: 2, totalClasses: 12, totalSections: 24,
          todayPresent: 2, todayAbsent: 0, todayLate: 0, todayLeave: 0, todayMarked: 2,
          totalFeeCollected: 9000, pendingFees: 70000, upcomingExaminations: 1,
          totalLibraryBooks: 3, issuedBooksCount: 1,
          recentAdmissions: [], recentAnnouncements: [], upcomingExamsList: [],
          classWiseDistribution: [], monthlyAttendance: [], monthlyFeeCollection: [],
          examPerformance: [], admissionStatistics: [],
        },
      },
    });

    renderAt('/portal/admin');

    expect(await screen.findByText(/ERP Portal Sign In/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'Admin@123' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign In to Portal/i }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/auth/login', { username: 'admin', password: 'Admin@123' });
      expect(screen.getByText(/Administration Dashboard/i)).toBeInTheDocument();
    });
  });

  it('supports multi-child switching on Parent Dashboard', async () => {
    localStorage.setItem('tgs_access_token', 'tok');
    localStorage.setItem('tgs_user', JSON.stringify({ id: 10, username: 'parent1', fullName: 'Venkat Kumar', roles: ['PARENT'] }));

    api.get.mockImplementation((url) => {
      if (url === '/auth/me') {
        return Promise.resolve({ data: { data: { id: 10, username: 'parent1', fullName: 'Venkat Kumar', roles: ['PARENT'] } } });
      }
      if (url === '/parents/my-children') {
        return Promise.resolve({
          data: {
            data: [
              { studentId: 101, admissionNumber: 'TGS-2026-1001', fullName: 'Aarav Kumar', className: '5th Class', sectionName: 'A' },
              { studentId: 102, admissionNumber: 'TGS-2026-1002', fullName: 'Ananya Kumar', className: 'LKG', sectionName: 'A' },
            ],
          },
        });
      }
      if (url === '/students/101') {
        return Promise.resolve({ data: { data: { id: 101, fullName: 'Aarav Kumar', admissionNumber: 'TGS-2026-1001', currentEnrollment: { classId: 7, className: '5th Class', sectionName: 'A', rollNumber: '01' }, teacherRemarks: [] } } });
      }
      if (url === '/students/102') {
        return Promise.resolve({ data: { data: { id: 102, fullName: 'Ananya Kumar', admissionNumber: 'TGS-2026-1002', currentEnrollment: { classId: 1, className: 'LKG', sectionName: 'A', rollNumber: '01' }, teacherRemarks: [] } } });
      }
      if (url.startsWith('/attendance/student/')) {
        return Promise.resolve({ data: { data: { attendancePercentage: 100, presentDays: 5, totalDays: 5, records: [] } } });
      }
      if (url.startsWith('/notifications')) {
        return Promise.resolve({ data: { data: { content: [] } } });
      }
      return Promise.resolve({ data: { data: [] } });
    });

    renderAt('/portal/parent');

    expect(await screen.findByText('Aarav Kumar')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Ananya Kumar/i }));

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/students/102');
      expect(screen.getByText('Ananya Kumar')).toBeInTheDocument();
    });
  });
});
