import React, { useEffect, useState } from 'react';
import { UserPlus, Link2, ShieldAlert } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';

export function TeachersPage() {
  const toast = useToast();
  const [tab, setTab] = useState('teachers');
  const [teachers, setTeachers] = useState([]);
  const [staff, setStaff] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [sections, setSections] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [teacherModal, setTeacherModal] = useState(false);
  const [staffModal, setStaffModal] = useState(false);
  const [assignModal, setAssignModal] = useState(false);
  const [tForm, setTForm] = useState({ fullName: '', email: '', phone: '', qualification: '', specialization: '', username: '', password: '' });
  const [sForm, setSForm] = useState({ fullName: '', roleTitle: '', department: '', phone: '', salary: '' });
  const [aForm, setAForm] = useState({ teacherId: '', sectionId: '', subjectId: '', classTeacher: false });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [tRes, stRes, aRes, secRes, subRes] = await Promise.all([
        api.get('/teachers?size=100'),
        api.get('/staff?size=100'),
        api.get('/teachers/assignments'),
        api.get('/academics/sections'),
        api.get('/academics/subjects'),
      ]);
      setTeachers(tRes.data?.data?.content || []);
      setStaff(stRes.data?.data?.content || []);
      setAssignments(aRes.data?.data || []);
      setSections(secRes.data?.data || []);
      setSubjects(subRes.data?.data || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadAll(); }, []);

  const createTeacher = async (e) => {
    e.preventDefault();
    try {
      await api.post('/teachers', tForm);
      toast.success('Teacher created');
      setTeacherModal(false);
      setTForm({ fullName: '', email: '', phone: '', qualification: '', specialization: '', username: '', password: '' });
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const createStaff = async (e) => {
    e.preventDefault();
    try {
      await api.post('/staff', { ...sForm, salary: sForm.salary ? Number(sForm.salary) : null });
      toast.success('Staff member added');
      setStaffModal(false);
      setSForm({ fullName: '', roleTitle: '', department: '', phone: '', salary: '' });
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const assignTeacher = async (e) => {
    e.preventDefault();
    try {
      await api.post('/teachers/assignments', {
        teacherId: Number(aForm.teacherId),
        sectionId: Number(aForm.sectionId),
        subjectId: Number(aForm.subjectId),
        classTeacher: aForm.classTeacher,
      });
      toast.success('Subject & class assigned to teacher');
      setAssignModal(false);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Teachers & Non-Teaching Staff"
        subtitle="Manage faculty profiles, subject-section allocations, and campus staff."
        actions={
          <>
            <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setAssignModal(true)}>Assign Subject</button>
            <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setStaffModal(true)}>Add Staff</button>
            <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setTeacherModal(true)}><UserPlus size={14} className="me-1" /> Add Teacher</button>
          </>
        }
      />

      <ul className="nav nav-tabs mb-3">
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'teachers' ? 'active' : ''}`} onClick={() => setTab('teachers')}>Teachers ({teachers.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'assignments' ? 'active' : ''}`} onClick={() => setTab('assignments')}>Class Assignments ({assignments.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'staff' ? 'active' : ''}`} onClick={() => setTab('staff')}>Non-Teaching Staff ({staff.length})</button></li>
      </ul>

      {tab === 'teachers' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Emp Code</th><th>Name</th><th>Email / Phone</th><th>Qualification</th><th>Specialization</th><th>Status</th></tr></thead>
            <tbody>
              {teachers.map((t) => (
                <tr key={t.id}>
                  <td className="fw-semibold text-primary">{t.employeeCode}</td>
                  <td><div className="fw-semibold">{t.fullName}</div><div className="small text-muted">@{t.username}</div></td>
                  <td className="small">{t.email}<br />{t.phone}</td>
                  <td>{t.qualification || '-'}</td>
                  <td>{t.specialization || '-'}</td>
                  <td><StatusBadge status={t.employmentStatus} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'assignments' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Class &amp; Section</th><th>Subject</th><th>Assigned Teacher</th><th>Class Teacher</th></tr></thead>
            <tbody>
              {assignments.map((a) => (
                <tr key={a.id}>
                  <td className="fw-semibold">{a.className} - {a.sectionName}</td>
                  <td>{a.subjectName} ({a.subjectCode})</td>
                  <td>{a.teacherName}</td>
                  <td>{a.classTeacher ? <span className="badge bg-success-subtle text-success">Yes</span> : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'staff' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Emp Code</th><th>Name</th><th>Role</th><th>Department</th><th>Phone</th><th>Status</th></tr></thead>
            <tbody>
              {staff.map((s) => (
                <tr key={s.id}>
                  <td className="fw-semibold">{s.employeeCode}</td>
                  <td className="fw-semibold">{s.fullName}</td>
                  <td>{s.roleTitle}</td>
                  <td>{s.department || '-'}</td>
                  <td>{s.phone}</td>
                  <td><StatusBadge status={s.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal show={teacherModal} title="Add New Teacher" onClose={() => setTeacherModal(false)} size="md">
        <form onSubmit={createTeacher} className="row g-2">
          <div className="col-6"><label className="form-label small">Full Name *</label><input required className="form-control form-control-sm" value={tForm.fullName} onChange={(e) => setTForm({ ...tForm, fullName: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Email *</label><input type="email" required className="form-control form-control-sm" value={tForm.email} onChange={(e) => setTForm({ ...tForm, email: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Phone *</label><input required className="form-control form-control-sm" value={tForm.phone} onChange={(e) => setTForm({ ...tForm, phone: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Qualification</label><input className="form-control form-control-sm" value={tForm.qualification} onChange={(e) => setTForm({ ...tForm, qualification: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Specialization</label><input className="form-control form-control-sm" value={tForm.specialization} onChange={(e) => setTForm({ ...tForm, specialization: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Username (optional)</label><input className="form-control form-control-sm" value={tForm.username} onChange={(e) => setTForm({ ...tForm, username: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Save Teacher</button></div>
        </form>
      </Modal>

      <Modal show={staffModal} title="Add Non-Teaching Staff" onClose={() => setStaffModal(false)} size="md">
        <form onSubmit={createStaff} className="row g-2">
          <div className="col-6"><label className="form-label small">Full Name *</label><input required className="form-control form-control-sm" value={sForm.fullName} onChange={(e) => setSForm({ ...sForm, fullName: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Role Title *</label><input required className="form-control form-control-sm" value={sForm.roleTitle} onChange={(e) => setSForm({ ...sForm, roleTitle: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Department</label><input className="form-control form-control-sm" value={sForm.department} onChange={(e) => setSForm({ ...sForm, department: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Phone *</label><input required className="form-control form-control-sm" value={sForm.phone} onChange={(e) => setSForm({ ...sForm, phone: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Salary</label><input type="number" className="form-control form-control-sm" value={sForm.salary} onChange={(e) => setSForm({ ...sForm, salary: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Save Staff</button></div>
        </form>
      </Modal>

      <Modal show={assignModal} title="Assign Teacher to Class & Subject" onClose={() => setAssignModal(false)} size="md">
        <form onSubmit={assignTeacher} className="d-flex flex-column gap-3">
          <div>
            <label className="form-label small fw-semibold">Teacher *</label>
            <select required className="form-select form-select-sm" value={aForm.teacherId} onChange={(e) => setAForm({ ...aForm, teacherId: e.target.value })}>
              <option value="">Select Teacher</option>
              {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName} ({t.employeeCode})</option>)}
            </select>
          </div>
          <div>
            <label className="form-label small fw-semibold">Class &amp; Section *</label>
            <select required className="form-select form-select-sm" value={aForm.sectionId} onChange={(e) => setAForm({ ...aForm, sectionId: e.target.value })}>
              <option value="">Select Section</option>
              {sections.map((s) => <option key={s.id} value={s.id}>{s.className} - {s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label small fw-semibold">Subject *</label>
            <select required className="form-select form-select-sm" value={aForm.subjectId} onChange={(e) => setAForm({ ...aForm, subjectId: e.target.value })}>
              <option value="">Select Subject</option>
              {subjects.map((sub) => <option key={sub.id} value={sub.id}>{sub.name} ({sub.code})</option>)}
            </select>
          </div>
          <div className="form-check">
            <input type="checkbox" className="form-check-input" id="isCtCheck" checked={aForm.classTeacher}
              onChange={(e) => setAForm({ ...aForm, classTeacher: e.target.checked })} />
            <label className="form-check-label small" htmlFor="isCtCheck">Designate as Class Teacher for this section</label>
          </div>
          <div className="text-end"><button type="submit" className="btn tgs-btn-navy btn-sm">Assign</button></div>
        </form>
      </Modal>
    </div>
  );
}

export function ParentsPage() {
  const toast = useToast();
  const [parents, setParents] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [linkOpen, setLinkOpen] = useState(false);
  const [pForm, setPForm] = useState({ fullName: '', email: '', phone: '', occupation: '', address: '' });
  const [lForm, setLForm] = useState({ parentId: '', studentId: '', relationship: 'FATHER', primary: true });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [pRes, sRes] = await Promise.all([api.get('/parents?size=100'), api.get('/students?size=200')]);
      setParents(pRes.data?.data?.content || []);
      setStudents(sRes.data?.data?.content || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadAll(); }, []);

  const createParent = async (e) => {
    e.preventDefault();
    try {
      await api.post('/parents', pForm);
      toast.success('Parent registered');
      setCreateOpen(false);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const linkStudent = async (e) => {
    e.preventDefault();
    try {
      await api.post('/parents/link-student', {
        parentId: Number(lForm.parentId),
        studentId: Number(lForm.studentId),
        relationship: lForm.relationship,
        primary: lForm.primary,
      });
      toast.success('Student linked to parent');
      setLinkOpen(false);
      loadAll();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Parent Directory & Multi-Child Links"
        subtitle="Manage parent accounts and link siblings under a single parent login."
        actions={
          <>
            <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setLinkOpen(true)}><Link2 size={14} className="me-1" /> Link Child</button>
            <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setCreateOpen(true)}><UserPlus size={14} className="me-1" /> Add Parent</button>
          </>
        }
      />

      <div className="tgs-card overflow-hidden">
        <table className="table tgs-table table-hover mb-0">
          <thead><tr><th>Parent Name</th><th>Username</th><th>Email / Phone</th><th>Linked Children</th></tr></thead>
          <tbody>
            {parents.map((p) => (
              <tr key={p.id}>
                <td className="fw-semibold">{p.fullName}</td>
                <td>@{p.username}</td>
                <td className="small">{p.email}<br />{p.phone}</td>
                <td>
                  {(p.children || []).length === 0 ? '-' : p.children.map((c) => (
                    <span key={c.studentId} className="badge bg-primary-subtle text-primary me-1">
                      {c.fullName} ({c.className || '-'}-{c.sectionName || '-'})
                    </span>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal show={createOpen} title="Register Parent Account" onClose={() => setCreateOpen(false)} size="md">
        <form onSubmit={createParent} className="row g-2">
          <div className="col-6"><label className="form-label small">Full Name *</label><input required className="form-control form-control-sm" value={pForm.fullName} onChange={(e) => setPForm({ ...pForm, fullName: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Email *</label><input type="email" required className="form-control form-control-sm" value={pForm.email} onChange={(e) => setPForm({ ...pForm, email: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Phone *</label><input required className="form-control form-control-sm" value={pForm.phone} onChange={(e) => setPForm({ ...pForm, phone: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Occupation</label><input className="form-control form-control-sm" value={pForm.occupation} onChange={(e) => setPForm({ ...pForm, occupation: e.target.value })} /></div>
          <div className="col-12"><label className="form-label small">Address</label><input className="form-control form-control-sm" value={pForm.address} onChange={(e) => setPForm({ ...pForm, address: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Create Parent</button></div>
        </form>
      </Modal>

      <Modal show={linkOpen} title="Link Student to Parent" onClose={() => setLinkOpen(false)} size="md">
        <form onSubmit={linkStudent} className="d-flex flex-column gap-3">
          <div>
            <label className="form-label small fw-semibold">Parent *</label>
            <select required className="form-select form-select-sm" value={lForm.parentId} onChange={(e) => setLForm({ ...lForm, parentId: e.target.value })}>
              <option value="">Select Parent</option>
              {parents.map((p) => <option key={p.id} value={p.id}>{p.fullName} ({p.phone})</option>)}
            </select>
          </div>
          <div>
            <label className="form-label small fw-semibold">Student *</label>
            <select required className="form-select form-select-sm" value={lForm.studentId} onChange={(e) => setLForm({ ...lForm, studentId: e.target.value })}>
              <option value="">Select Student</option>
              {students.map((s) => <option key={s.id} value={s.id}>{s.fullName} ({s.admissionNumber})</option>)}
            </select>
          </div>
          <div>
            <label className="form-label small fw-semibold">Relationship</label>
            <select className="form-select form-select-sm" value={lForm.relationship} onChange={(e) => setLForm({ ...lForm, relationship: e.target.value })}>
              <option value="FATHER">FATHER</option><option value="MOTHER">MOTHER</option><option value="GUARDIAN">GUARDIAN</option>
            </select>
          </div>
          <div className="text-end"><button type="submit" className="btn tgs-btn-navy btn-sm">Link Child</button></div>
        </form>
      </Modal>
    </div>
  );
}

export function UsersPage() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({ username: '', email: '', phone: '', fullName: '', password: '', role: 'ADMIN' });

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/users?size=100');
      setUsers(res.data?.data?.content || []);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadUsers(); }, []);

  const toggleStatus = async (u) => {
    try {
      await api.patch(`/users/${u.id}/status`, { enabled: !u.enabled, accountLocked: u.accountLocked });
      toast.success(`User ${u.username} ${u.enabled ? 'deactivated' : 'activated'}`);
      loadUsers();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  const createUser = async (e) => {
    e.preventDefault();
    try {
      await api.post('/users', {
        username: form.username,
        email: form.email,
        phone: form.phone,
        fullName: form.fullName,
        password: form.password,
        roles: [form.role],
      });
      toast.success('User account created');
      setCreateOpen(false);
      setForm({ username: '', email: '', phone: '', fullName: '', password: '', role: 'ADMIN' });
      loadUsers();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="User Accounts & Access Control"
        subtitle="Activate/deactivate portal accounts and provision administrative, accountant, or librarian users."
        actions={<button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setCreateOpen(true)}><UserPlus size={14} className="me-1" /> Create User</button>}
      />
      <div className="tgs-card overflow-hidden">
        <table className="table tgs-table table-hover mb-0">
          <thead><tr><th>Username</th><th>Full Name</th><th>Email</th><th>Roles</th><th>Status</th><th className="text-end">Action</th></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td className="fw-semibold">@{u.username}</td>
                <td>{u.fullName}</td>
                <td className="small">{u.email}</td>
                <td>{(u.roles || []).map((r) => <span key={r} className="badge bg-primary-subtle text-primary me-1">{r}</span>)}</td>
                <td><StatusBadge status={u.enabled ? 'ACTIVE' : 'ARCHIVED'} /></td>
                <td className="text-end">
                  <button type="button" className={`btn btn-sm ${u.enabled ? 'btn-outline-danger' : 'btn-outline-success'}`} onClick={() => toggleStatus(u)}>
                    <ShieldAlert size={13} className="me-1" /> {u.enabled ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal show={createOpen} title="Create Portal User" onClose={() => setCreateOpen(false)} size="md">
        <form onSubmit={createUser} className="row g-2">
          <div className="col-6"><label className="form-label small">Full Name *</label><input required className="form-control form-control-sm" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Username *</label><input required className="form-control form-control-sm" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Email *</label><input type="email" required className="form-control form-control-sm" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Phone</label><input className="form-control form-control-sm" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Role *</label>
            <select className="form-select form-select-sm" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {['SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'ACCOUNTANT', 'LIBRARIAN', 'TEACHER'].map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div className="col-6"><label className="form-label small">Password *</label><input type="password" required minLength={8} className="form-control form-control-sm" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Create User</button></div>
        </form>
      </Modal>
    </div>
  );
}
