import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { UserPlus, Search, Eye, ArrowUpRight } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, EmptyState, Modal } from '../../components/CommonUI';

export default function StudentsPage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');

  const [students, setStudents] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [classes, setClasses] = useState([]);
  const [sections, setSections] = useState([]);
  const [parents, setParents] = useState([]);
  const [filters, setFilters] = useState({ q: '', classId: '', sectionId: '', status: 'ACTIVE' });
  const [loading, setLoading] = useState(true);
  const [admitOpen, setAdmitOpen] = useState(false);

  const { register, handleSubmit, watch, reset, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { gender: 'MALE', relationship: 'FATHER' },
  });
  const watchClassId = watch('classId');
  const watchExistingParent = watch('existingParentId');

  useEffect(() => {
    Promise.all([
      api.get('/academics/classes'),
      api.get('/academics/sections'),
      isAdmin ? api.get('/parents?size=200') : Promise.resolve({ data: { data: { content: [] } } }),
    ]).then(([cRes, sRes, pRes]) => {
      setClasses(cRes.data?.data || []);
      setSections(sRes.data?.data || []);
      setParents(pRes.data?.data?.content || []);
    }).catch(() => {});
  }, [isAdmin]);

  const loadStudents = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: '15' });
      if (filters.q) params.set('q', filters.q);
      if (filters.classId) params.set('classId', filters.classId);
      if (filters.sectionId) params.set('sectionId', filters.sectionId);
      if (filters.status) params.set('status', filters.status);
      const res = await api.get(`/students?${params.toString()}`);
      const d = res.data?.data;
      setStudents(d?.content || []);
      setTotalPages(d?.totalPages || 0);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => {
    loadStudents();
  }, [loadStudents]);

  const onAdmit = async (values) => {
    try {
      const payload = {
        ...values,
        classId: Number(values.classId),
        sectionId: Number(values.sectionId),
        existingParentId: values.existingParentId ? Number(values.existingParentId) : null,
      };
      const res = await api.post('/students/admit', payload);
      toast.success(`Admitted ${res.data?.data?.fullName} (${res.data?.data?.admissionNumber})`);
      setAdmitOpen(false);
      reset({ gender: 'MALE', relationship: 'FATHER' });
      loadStudents();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const filteredSections = sections.filter((s) => !filters.classId || String(s.classId) === String(filters.classId));
  const modalSections = sections.filter((s) => !watchClassId || String(s.classId) === String(watchClassId));

  return (
    <div>
      <PageHeader
        title="Student Directory"
        subtitle="Search, filter, admit students, and inspect 9-tab student profiles."
        actions={
          isAdmin && (
            <>
              <Link to="/portal/students/promote" className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-1">
                <ArrowUpRight size={15} /> Promote Students
              </Link>
              <button type="button" className="btn tgs-btn-navy btn-sm d-inline-flex align-items-center gap-1" onClick={() => setAdmitOpen(true)}>
                <UserPlus size={15} /> Admit New Student
              </button>
            </>
          )
        }
      />

      {/* Filters */}
      <div className="tgs-card p-3 mb-4">
        <div className="row g-2 align-items-center">
          <div className="col-md-4">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white"><Search size={15} /></span>
              <input
                className="form-control"
                placeholder="Search by name or admission no..."
                value={filters.q}
                onChange={(e) => { setPage(0); setFilters({ ...filters, q: e.target.value }); }}
              />
            </div>
          </div>
          <div className="col-md-3">
            <select
              className="form-select form-select-sm"
              value={filters.classId}
              onChange={(e) => { setPage(0); setFilters({ ...filters, classId: e.target.value, sectionId: '' }); }}
            >
              <option value="">All Classes (LKG – 10th)</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <select
              className="form-select form-select-sm"
              value={filters.sectionId}
              onChange={(e) => { setPage(0); setFilters({ ...filters, sectionId: e.target.value }); }}
            >
              <option value="">All Sections</option>
              {filteredSections.map((s) => <option key={s.id} value={s.id}>{s.className} - {s.name}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <select
              className="form-select form-select-sm"
              value={filters.status}
              onChange={(e) => { setPage(0); setFilters({ ...filters, status: e.target.value }); }}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="TRANSFERRED">TRANSFERRED</option>
              <option value="WITHDRAWN">WITHDRAWN</option>
              <option value="ARCHIVED">ARCHIVED</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? <LoadingSkeleton rows={6} /> : students.length === 0 ? (
        <EmptyState title="No students match your filters" />
      ) : (
        <div className="tgs-card overflow-hidden">
          <div className="table-responsive">
            <table className="table tgs-table table-hover mb-0">
              <thead>
                <tr>
                  <th>Adm. No</th>
                  <th>Student Name</th>
                  <th>Class &amp; Section</th>
                  <th>Roll No</th>
                  <th>Primary Parent</th>
                  <th>Status</th>
                  <th className="text-end">Action</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.id}>
                    <td className="fw-semibold text-primary">{s.admissionNumber}</td>
                    <td>
                      <div className="fw-semibold">{s.fullName}</div>
                      <div className="text-muted" style={{ fontSize: '0.75rem' }}>{s.gender} • {s.bloodGroup || '-'}</div>
                    </td>
                    <td>{s.className} - {s.sectionName}</td>
                    <td>{s.rollNumber || '-'}</td>
                    <td>
                      <div>{s.primaryParentName || '-'}</div>
                      <div className="text-muted" style={{ fontSize: '0.75rem' }}>{s.primaryParentPhone || ''}</div>
                    </td>
                    <td><StatusBadge status={s.status} /></td>
                    <td className="text-end">
                      <Link to={`/portal/students/${s.id}`} className="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1">
                        <Eye size={14} /> Profile
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && (
            <div className="p-3 border-top d-flex justify-content-between align-items-center small">
              <span>Page {page + 1} of {totalPages}</span>
              <div className="btn-group btn-group-sm">
                <button type="button" className="btn btn-outline-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
                <button type="button" className="btn btn-outline-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Admit Student Modal */}
      <Modal show={admitOpen} title="Admit New Student (Auto-generates Admission No & Portal Accounts)" onClose={() => setAdmitOpen(false)} size="xl">
        <form onSubmit={handleSubmit(onAdmit)} className="row g-3">
          <div className="col-12"><h6 className="fw-bold text-primary border-bottom pb-1 mb-0">1. Student Details</h6></div>
          <div className="col-md-4">
            <label className="form-label small fw-semibold">Full Name *</label>
            <input className={`form-control form-control-sm ${errors.fullName ? 'is-invalid' : ''}`} {...register('fullName', { required: true })} />
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Date of Birth *</label>
            <input type="date" className={`form-control form-control-sm ${errors.dateOfBirth ? 'is-invalid' : ''}`} {...register('dateOfBirth', { required: true })} />
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-semibold">Gender *</label>
            <select className="form-select form-select-sm" {...register('gender', { required: true })}>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Blood Group</label>
            <input className="form-control form-control-sm" placeholder="e.g. O+, B+" {...register('bloodGroup')} />
          </div>

          <div className="col-md-3">
            <label className="form-label small fw-semibold">Class *</label>
            <select className={`form-select form-select-sm ${errors.classId ? 'is-invalid' : ''}`} {...register('classId', { required: true })}>
              <option value="">Select Class</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Section *</label>
            <select className={`form-select form-select-sm ${errors.sectionId ? 'is-invalid' : ''}`} {...register('sectionId', { required: true })}>
              <option value="">Select Section</option>
              {modalSections.map((s) => <option key={s.id} value={s.id}>{s.className} - {s.name}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Previous School</label>
            <input className="form-control form-control-sm" {...register('previousSchool')} />
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Student Portal Username (optional)</label>
            <input className="form-control form-control-sm" placeholder="Auto if blank" {...register('studentUsername')} />
          </div>
          <div className="col-12">
            <label className="form-label small fw-semibold">Address</label>
            <input className="form-control form-control-sm" defaultValue="Kalluru, Khammam, Telangana" {...register('address')} />
          </div>

          <div className="col-12 mt-3"><h6 className="fw-bold text-primary border-bottom pb-1 mb-0">2. Parent / Guardian Information</h6></div>
          <div className="col-md-6">
            <label className="form-label small fw-semibold">Link Existing Parent (for siblings)</label>
            <select className="form-select form-select-sm" {...register('existingParentId')}>
              <option value="">-- Or Create New Parent Below --</option>
              {parents.map((p) => <option key={p.id} value={p.id}>{p.fullName} ({p.phone})</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Relationship</label>
            <select className="form-select form-select-sm" {...register('relationship')}>
              <option value="FATHER">Father</option>
              <option value="MOTHER">Mother</option>
              <option value="GUARDIAN">Guardian</option>
            </select>
          </div>

          {!watchExistingParent && (
            <>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Parent Full Name *</label>
                <input className="form-control form-control-sm" {...register('parentFullName', { required: !watchExistingParent })} />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Parent Email *</label>
                <input type="email" className="form-control form-control-sm" {...register('parentEmail', { required: !watchExistingParent })} />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Parent Phone *</label>
                <input className="form-control form-control-sm" {...register('parentPhone', { required: !watchExistingParent })} />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Occupation</label>
                <input className="form-control form-control-sm" {...register('parentOccupation')} />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Parent Username (optional)</label>
                <input className="form-control form-control-sm" placeholder="Auto if blank" {...register('parentUsername')} />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Parent Password (optional)</label>
                <input type="password" className="form-control form-control-sm" placeholder="Default: Parent@123" {...register('parentPassword')} />
              </div>
            </>
          )}

          <div className="col-12 text-end pt-2">
            <button type="button" className="btn btn-outline-secondary btn-sm me-2" onClick={() => setAdmitOpen(false)}>Cancel</button>
            <button type="submit" className="btn tgs-btn-navy btn-sm" disabled={isSubmitting}>
              {isSubmitting ? 'Admitting...' : 'Complete Admission'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
