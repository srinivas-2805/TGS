import React, { useEffect, useState } from 'react';
import { Plus, Download, Trash2, Check } from 'lucide-react';
import api, { downloadFile, extractErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components/CommonUI';
import { formatCurrency, formatDate } from '../../utils/formatters';

export function TransportPage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');
  const [buses, setBuses] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [allocations, setAllocations] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [assignForm, setAssignForm] = useState({ studentId: '', routeId: '', pickupPoint: '', dropPoint: '' });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [bR, rR, aR, sR, dR] = await Promise.all([
        api.get('/transport/buses'),
        api.get('/transport/routes'),
        api.get('/transport/allocations'),
        api.get('/students?size=200').catch(() => ({ data: { data: { content: [] } } })),
        isAdmin ? api.get('/transport/drivers') : Promise.resolve({ data: { data: [] } }),
      ]);
      setBuses(bR.data?.data || []);
      setRoutes(rR.data?.data || []);
      setAllocations(aR.data?.data || []);
      setStudents(sR.data?.data?.content || []);
      setDrivers(dR.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, [isAdmin]);

  const assignStudent = async (e) => {
    e.preventDefault();
    try {
      await api.post('/transport/allocations', {
        ...assignForm,
        studentId: Number(assignForm.studentId),
        routeId: Number(assignForm.routeId),
      });
      toast.success('Student assigned to transport route');
      setAssignForm({ studentId: '', routeId: '', pickupPoint: '', dropPoint: '' });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader title="School Transport Management" subtitle="Manage buses, drivers, pickup/drop routes, and student transport allocations." />
      <div className="row g-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Routes &amp; Fleet ({routes.length} routes • {buses.length} buses • {drivers.length} drivers)</h6>
            {routes.map((r) => (
              <div key={r.id} className="p-3 border rounded mb-2 small">
                <div className="d-flex justify-content-between"><strong className="text-primary">{r.routeCode} — {r.routeName}</strong><strong>{formatCurrency(r.monthlyFee)}/mo</strong></div>
                <div className="text-muted">Bus: {r.busNumber || '-'} • Driver: {r.driverName || '-'} ({r.driverPhone || '-'})</div>
                <div className="text-muted">Stops: {r.pickupPointsJson}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="col-lg-6">
          <div className="tgs-card p-4 h-100">
            <h6 className="fw-bold mb-3">Assign Student to Route</h6>
            <form onSubmit={assignStudent} className="row g-2 mb-3">
              <div className="col-6">
                <select required className="form-select form-select-sm" value={assignForm.studentId} onChange={(e) => setAssignForm({ ...assignForm, studentId: e.target.value })}>
                  <option value="">Select Student</option>{students.map((s) => <option key={s.id} value={s.id}>{s.fullName} ({s.admissionNumber})</option>)}
                </select>
              </div>
              <div className="col-6">
                <select required className="form-select form-select-sm" value={assignForm.routeId} onChange={(e) => setAssignForm({ ...assignForm, routeId: e.target.value })}>
                  <option value="">Select Route</option>{routes.map((r) => <option key={r.id} value={r.id}>{r.routeName}</option>)}
                </select>
              </div>
              <div className="col-5"><input required className="form-control form-control-sm" placeholder="Pickup Point" value={assignForm.pickupPoint} onChange={(e) => setAssignForm({ ...assignForm, pickupPoint: e.target.value })} /></div>
              <div className="col-5"><input required className="form-control form-control-sm" placeholder="Drop Point" value={assignForm.dropPoint} onChange={(e) => setAssignForm({ ...assignForm, dropPoint: e.target.value })} /></div>
              <div className="col-2"><button type="submit" className="btn tgs-btn-navy btn-sm w-100">Save</button></div>
            </form>
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Student</th><th>Route</th><th>Pickup</th><th>Fee</th></tr></thead>
              <tbody>
                {allocations.map((a) => (
                  <tr key={a.id}><td className="fw-semibold">{a.studentName}</td><td>{a.routeCode}</td><td>{a.pickupPoint}</td><td>{formatCurrency(a.monthlyFee)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export function LibraryPage() {
  const toast = useToast();
  const [books, setBooks] = useState([]);
  const [issues, setIssues] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookModal, setBookModal] = useState(false);
  const [issueModal, setIssueModal] = useState(false);
  const [bForm, setBForm] = useState({ isbn: '', title: '', author: '', category: 'Science', totalCopies: 5, shelfLocation: '' });
  const [iForm, setIForm] = useState({ bookId: '', studentId: '', dueDate: '' });

  const loadAll = async () => {
    setLoading(true);
    try {
      const [bR, iR, sR] = await Promise.all([
        api.get('/library/books?size=100'),
        api.get('/library/issues'),
        api.get('/students?size=200').catch(() => ({ data: { data: { content: [] } } })),
      ]);
      setBooks(bR.data?.data?.content || []);
      setIssues(iR.data?.data || []);
      setStudents(sR.data?.data?.content || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, []);

  const addBook = async (e) => {
    e.preventDefault();
    try {
      await api.post('/library/books', { ...bForm, totalCopies: Number(bForm.totalCopies) });
      toast.success('Book added to catalog');
      setBookModal(false);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const issueBook = async (e) => {
    e.preventDefault();
    try {
      await api.post('/library/issues', { bookId: Number(iForm.bookId), studentId: Number(iForm.studentId), dueDate: iForm.dueDate });
      toast.success('Book issued');
      setIssueModal(false);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const returnBook = async (id) => {
    try {
      await api.patch(`/library/issues/${id}/return?lateFeePaid=true`);
      toast.success('Book returned and inventory updated');
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader
        title="Library Inventory & Circulation"
        subtitle="Manage cataloged books, student issues/returns, and configurable overdue late fees."
        actions={
          <>
            <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setIssueModal(true)}>Issue Book</button>
            <button type="button" className="btn tgs-btn-navy btn-sm" onClick={() => setBookModal(true)}><Plus size={14} className="me-1" /> Add Book</button>
          </>
        }
      />

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="tgs-card p-4">
            <h6 className="fw-bold mb-3">Book Catalog ({books.length})</h6>
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Title</th><th>Category</th><th>Available</th><th>Shelf</th></tr></thead>
              <tbody>
                {books.map((b) => (
                  <tr key={b.id}><td className="fw-semibold">{b.title}<div className="small text-muted">{b.author}</div></td><td>{b.category}</td><td>{b.availableCopies}/{b.totalCopies}</td><td>{b.shelfLocation || '-'}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="tgs-card p-4">
            <h6 className="fw-bold mb-3">Borrowing History</h6>
            <table className="table tgs-table table-sm mb-0">
              <thead><tr><th>Book</th><th>Borrower</th><th>Due</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {issues.map((i) => (
                  <tr key={i.id}>
                    <td className="fw-semibold">{i.bookTitle}</td>
                    <td>{i.studentName || i.teacherName}</td>
                    <td>{formatDate(i.dueDate)}</td>
                    <td><StatusBadge status={i.status} /></td>
                    <td className="text-end">
                      {i.status === 'ISSUED' && <button type="button" className="btn btn-sm btn-outline-success" onClick={() => returnBook(i.id)}>Return</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <Modal show={bookModal} title="Add Book" onClose={() => setBookModal(false)} size="md">
        <form onSubmit={addBook} className="row g-2">
          <div className="col-8"><label className="form-label small">Title *</label><input required className="form-control form-control-sm" value={bForm.title} onChange={(e) => setBForm({ ...bForm, title: e.target.value })} /></div>
          <div className="col-4"><label className="form-label small">ISBN</label><input className="form-control form-control-sm" value={bForm.isbn} onChange={(e) => setBForm({ ...bForm, isbn: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Author *</label><input required className="form-control form-control-sm" value={bForm.author} onChange={(e) => setBForm({ ...bForm, author: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Category *</label><input required className="form-control form-control-sm" value={bForm.category} onChange={(e) => setBForm({ ...bForm, category: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Total Copies</label><input type="number" min={1} required className="form-control form-control-sm" value={bForm.totalCopies} onChange={(e) => setBForm({ ...bForm, totalCopies: e.target.value })} /></div>
          <div className="col-6"><label className="form-label small">Shelf</label><input className="form-control form-control-sm" value={bForm.shelfLocation} onChange={(e) => setBForm({ ...bForm, shelfLocation: e.target.value })} /></div>
          <div className="col-12 text-end pt-2"><button type="submit" className="btn tgs-btn-navy btn-sm">Add Book</button></div>
        </form>
      </Modal>

      <Modal show={issueModal} title="Issue Book" onClose={() => setIssueModal(false)} size="sm">
        <form onSubmit={issueBook} className="d-flex flex-column gap-2">
          <select required className="form-select form-select-sm" value={iForm.bookId} onChange={(e) => setIForm({ ...iForm, bookId: e.target.value })}>
            <option value="">Select Book</option>{books.filter((b) => b.availableCopies > 0).map((b) => <option key={b.id} value={b.id}>{b.title} ({b.availableCopies} avail)</option>)}
          </select>
          <select required className="form-select form-select-sm" value={iForm.studentId} onChange={(e) => setIForm({ ...iForm, studentId: e.target.value })}>
            <option value="">Select Student</option>{students.map((s) => <option key={s.id} value={s.id}>{s.fullName} ({s.admissionNumber})</option>)}
          </select>
          <input type="date" required className="form-control form-control-sm" value={iForm.dueDate} onChange={(e) => setIForm({ ...iForm, dueDate: e.target.value })} />
          <button type="submit" className="btn tgs-btn-navy btn-sm">Issue Book</button>
        </form>
      </Modal>
    </div>
  );
}

export function CommunicationPage() {
  const { hasAnyRole } = useAuth();
  const toast = useToast();
  const canPublish = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN', 'TEACHER');
  const [announcements, setAnnouncements] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [classes, setClasses] = useState([]);
  const [form, setForm] = useState({ title: '', content: '', category: 'GENERAL', targetAudience: 'ALL', targetClassId: '', isPublic: false });
  const [loading, setLoading] = useState(true);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [aR, nR, cR] = await Promise.all([
        api.get('/announcements'),
        api.get('/notifications/my?page=0&size=20'),
        api.get('/academics/classes').catch(() => ({ data: { data: [] } })),
      ]);
      setAnnouncements(aR.data?.data || []);
      setNotifications(nR.data?.data?.content || []);
      setClasses(cR.data?.data || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, []);

  const publish = async (e) => {
    e.preventDefault();
    try {
      await api.post('/announcements', {
        ...form,
        targetClassId: form.targetClassId ? Number(form.targetClassId) : null,
      });
      toast.success('Announcement published & notifications dispatched');
      setForm({ title: '', content: '', category: 'GENERAL', targetAudience: 'ALL', targetClassId: '', isPublic: false });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader title="Communication & Notifications" subtitle="Publish school-wide or class-targeted announcements and review your notification history." />
      <div className="row g-4">
        {canPublish && (
          <div className="col-lg-5">
            <div className="tgs-card p-4">
              <h6 className="fw-bold mb-3">Publish Announcement</h6>
              <form onSubmit={publish} className="d-flex flex-column gap-2">
                <input required className="form-control form-control-sm" placeholder="Announcement Title *" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
                <div className="row g-2">
                  <div className="col-6">
                    <select className="form-select form-select-sm" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                      {['GENERAL', 'HOLIDAY', 'EXAMINATION', 'PTM', 'EVENT', 'FEE', 'EMERGENCY'].map((c) => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  <div className="col-6">
                    <select className="form-select form-select-sm" value={form.targetAudience} onChange={(e) => setForm({ ...form, targetAudience: e.target.value })}>
                      {['ALL', 'TEACHERS', 'PARENTS', 'STUDENTS', 'CLASS'].map((a) => <option key={a} value={a}>{a}</option>)}
                    </select>
                  </div>
                </div>
                {form.targetAudience === 'CLASS' && (
                  <select className="form-select form-select-sm" value={form.targetClassId} onChange={(e) => setForm({ ...form, targetClassId: e.target.value })}>
                    <option value="">Select Target Class</option>{classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                )}
                <textarea rows={3} required className="form-control form-control-sm" placeholder="Message content *" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
                <div className="form-check">
                  <input type="checkbox" className="form-check-input" id="isPubChk" checked={form.isPublic} onChange={(e) => setForm({ ...form, isPublic: e.target.checked })} />
                  <label className="form-check-label small" htmlFor="isPubChk">Also show on Public Website Notices page</label>
                </div>
                <button type="submit" className="btn tgs-btn-navy btn-sm">Publish &amp; Notify</button>
              </form>
            </div>
          </div>
        )}
        <div className={canPublish ? 'col-lg-7' : 'col-12'}>
          <div className="tgs-card p-4 mb-4">
            <h6 className="fw-bold mb-3">School Announcements</h6>
            {announcements.map((a) => (
              <div key={a.id} className="p-3 border rounded mb-2 small">
                <div className="d-flex justify-content-between"><span className="badge bg-primary-subtle text-primary">{a.category} • {a.targetAudience}</span><span className="text-muted">{formatDate(a.publishedAt)}</span></div>
                <div className="fw-bold mt-1">{a.title}</div>
                <div className="text-secondary">{a.content}</div>
              </div>
            ))}
          </div>
          <div className="tgs-card p-4">
            <h6 className="fw-bold mb-3">My Notification History</h6>
            {notifications.map((n) => (
              <div key={n.id} className="p-2 border rounded mb-2 small">
                <div className="fw-semibold">{n.title}</div>
                <div className="text-muted">{n.message}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function CmsPage() {
  const toast = useToast();
  const [tab, setTab] = useState('content');
  const [content, setContent] = useState([]);
  const [events, setEvents] = useState([]);
  const [gallery, setGallery] = useState([]);
  const [enquiries, setEnquiries] = useState([]);
  const [editing, setEditing] = useState(null);
  const [evForm, setEvForm] = useState({ title: '', description: '', eventDate: '', venue: '', category: 'ACADEMIC', isPublic: true });
  const [galForm, setGalForm] = useState({ title: '', description: '', category: 'CAMPUS', imageUrl: '/favicon.svg', isPublished: true });
  const [loading, setLoading] = useState(true);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [cR, eR, gR, enqR] = await Promise.all([
        api.get('/cms/content'),
        api.get('/cms/events'),
        api.get('/cms/gallery'),
        api.get('/cms/enquiries?size=50'),
      ]);
      setContent(cR.data?.data || []);
      setEvents(eR.data?.data || []);
      setGallery(gR.data?.data || []);
      setEnquiries(enqR.data?.data?.content || []);
    } catch (err) { toast.error(extractErrorMessage(err)); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, []);

  const saveContent = async (e) => {
    e.preventDefault();
    try {
      await api.put('/cms/content', editing);
      toast.success('Website section updated!');
      setEditing(null);
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const addEvent = async (e) => {
    e.preventDefault();
    try {
      await api.post('/cms/events', evForm);
      toast.success('Event published');
      setEvForm({ title: '', description: '', eventDate: '', venue: '', category: 'ACADEMIC', isPublic: true });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const addGallery = async (e) => {
    e.preventDefault();
    try {
      await api.post('/cms/gallery', galForm);
      toast.success('Gallery item added');
      setGalForm({ title: '', description: '', category: 'CAMPUS', imageUrl: '/favicon.svg', isPublished: true });
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  const updateEnquiryStatus = async (id, status) => {
    try {
      await api.patch(`/cms/enquiries/${id}`, { status });
      toast.success('Enquiry updated');
      loadAll();
    } catch (err) { toast.error(extractErrorMessage(err)); }
  };

  if (loading) return <LoadingSkeleton rows={5} />;

  return (
    <div>
      <PageHeader title="Public Website CMS & Admission Enquiries" subtitle="Edit homepage banners, Principal's message, facilities, events, gallery, and track admission enquiries." />
      <ul className="nav nav-tabs mb-3">
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'content' ? 'active' : ''}`} onClick={() => setTab('content')}>Website Sections ({content.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'enquiries' ? 'active' : ''}`} onClick={() => setTab('enquiries')}>Admission Enquiries ({enquiries.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'events' ? 'active' : ''}`} onClick={() => setTab('events')}>Events ({events.length})</button></li>
        <li className="nav-item"><button type="button" className={`nav-link small fw-semibold ${tab === 'gallery' ? 'active' : ''}`} onClick={() => setTab('gallery')}>Gallery ({gallery.length})</button></li>
      </ul>

      {tab === 'content' && (
        <div className="row g-3">
          {content.map((c) => (
            <div className="col-md-6" key={c.id}>
              <div className="tgs-card p-3 h-100">
                <div className="d-flex justify-content-between align-items-center mb-1">
                  <span className="badge bg-primary-subtle text-primary">{c.sectionKey}</span>
                  <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setEditing({ ...c })}>Edit</button>
                </div>
                <h6 className="fw-bold mb-1">{c.title}</h6>
                <p className="small text-muted mb-0" style={{ maxHeight: 65, overflow: 'hidden' }}>{c.bodyContent}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'enquiries' && (
        <div className="tgs-card overflow-hidden">
          <table className="table tgs-table table-hover mb-0">
            <thead><tr><th>Student</th><th>Parent</th><th>Phone</th><th>Class</th><th>Status</th><th className="text-end">Action</th></tr></thead>
            <tbody>
              {enquiries.map((eq) => (
                <tr key={eq.id}>
                  <td className="fw-semibold">{eq.studentName}</td>
                  <td>{eq.parentName}</td>
                  <td>{eq.phone}</td>
                  <td>{eq.applyingClassName}</td>
                  <td><StatusBadge status={eq.status} /></td>
                  <td className="text-end">
                    <button type="button" className="btn btn-sm btn-outline-success" onClick={() => updateEnquiryStatus(eq.id, 'CONTACTED')}>
                      <Check size={13} /> Mark Contacted
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'events' && (
        <div className="tgs-card p-4">
          <form onSubmit={addEvent} className="row g-2 mb-3">
            <div className="col-md-3"><input required className="form-control form-control-sm" placeholder="Event Title" value={evForm.title} onChange={(e) => setEvForm({ ...evForm, title: e.target.value })} /></div>
            <div className="col-md-2"><input type="date" required className="form-control form-control-sm" value={evForm.eventDate} onChange={(e) => setEvForm({ ...evForm, eventDate: e.target.value })} /></div>
            <div className="col-md-2"><input className="form-control form-control-sm" placeholder="Venue" value={evForm.venue} onChange={(e) => setEvForm({ ...evForm, venue: e.target.value })} /></div>
            <div className="col-md-3"><input required className="form-control form-control-sm" placeholder="Description" value={evForm.description} onChange={(e) => setEvForm({ ...evForm, description: e.target.value })} /></div>
            <div className="col-md-2"><button type="submit" className="btn tgs-btn-navy btn-sm w-100">Add Event</button></div>
          </form>
          {events.map((ev) => (
            <div key={ev.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center small">
              <div><strong>{ev.title}</strong> ({formatDate(ev.eventDate)}) — {ev.description}</div>
              <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => api.delete(`/cms/events/${ev.id}`).then(loadAll)}><Trash2 size={13} /></button>
            </div>
          ))}
        </div>
      )}

      {tab === 'gallery' && (
        <div className="tgs-card p-4">
          <form onSubmit={addGallery} className="row g-2 mb-3">
            <div className="col-md-3"><input required className="form-control form-control-sm" placeholder="Title" value={galForm.title} onChange={(e) => setGalForm({ ...galForm, title: e.target.value })} /></div>
            <div className="col-md-2"><input required className="form-control form-control-sm" placeholder="Category" value={galForm.category} onChange={(e) => setGalForm({ ...galForm, category: e.target.value })} /></div>
            <div className="col-md-3"><input required className="form-control form-control-sm" placeholder="Image URL" value={galForm.imageUrl} onChange={(e) => setGalForm({ ...galForm, imageUrl: e.target.value })} /></div>
            <div className="col-md-2"><input className="form-control form-control-sm" placeholder="Caption" value={galForm.description} onChange={(e) => setGalForm({ ...galForm, description: e.target.value })} /></div>
            <div className="col-md-2"><button type="submit" className="btn tgs-btn-navy btn-sm w-100">Add Image</button></div>
          </form>
          {gallery.map((g) => (
            <div key={g.id} className="p-2 border rounded mb-2 d-flex justify-content-between align-items-center small">
              <div><strong>{g.title}</strong> [{g.category}] — {g.imageUrl}</div>
              <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => api.delete(`/cms/gallery/${g.id}`).then(loadAll)}><Trash2 size={13} /></button>
            </div>
          ))}
        </div>
      )}

      <Modal show={!!editing} title={`Edit Website Section: ${editing?.sectionKey}`} onClose={() => setEditing(null)} size="lg">
        {editing && (
          <form onSubmit={saveContent} className="d-flex flex-column gap-2">
            <div><label className="form-label small">Title *</label><input required className="form-control form-control-sm" value={editing.title || ''} onChange={(e) => setEditing({ ...editing, title: e.target.value })} /></div>
            <div><label className="form-label small">Subtitle</label><input className="form-control form-control-sm" value={editing.subtitle || ''} onChange={(e) => setEditing({ ...editing, subtitle: e.target.value })} /></div>
            <div><label className="form-label small">Body Content *</label><textarea rows={5} required className="form-control form-control-sm" value={editing.bodyContent || ''} onChange={(e) => setEditing({ ...editing, bodyContent: e.target.value })} /></div>
            <div><label className="form-label small">Metadata JSON</label><textarea rows={2} className="form-control form-control-sm font-monospace" value={editing.metadataJson || '{}'} onChange={(e) => setEditing({ ...editing, metadataJson: e.target.value })} /></div>
            <div className="text-end"><button type="submit" className="btn tgs-btn-navy btn-sm">Save Section</button></div>
          </form>
        )}
      </Modal>
    </div>
  );
}

export function ReportsPage() {
  const { hasAnyRole } = useAuth();
  const isAdmin = hasAnyRole('SUPER_ADMIN', 'PRINCIPAL', 'ADMIN');
  const isAccountant = hasAnyRole('ACCOUNTANT');
  const isLibrarian = hasAnyRole('LIBRARIAN');

  const reports = [
    ...(isAdmin ? [
      { title: 'Student Strength & Directory', desc: 'Active students grouped by class and section.', pdf: '/reports/students/export?format=pdf', xlsx: '/reports/students/export?format=xlsx', name: 'TGS-Students' },
      { title: 'Student Monthly Attendance', desc: 'Daily student attendance logs for current month.', pdf: '/reports/attendance/export?format=pdf', xlsx: '/reports/attendance/export?format=xlsx', name: 'TGS-Student-Attendance' },
      { title: 'Teacher Attendance Report', desc: 'Faculty attendance records for current month.', pdf: '/reports/teachers-attendance/export?format=pdf', xlsx: '/reports/teachers-attendance/export?format=xlsx', name: 'TGS-Teacher-Attendance' },
    ] : []),
    ...((isAdmin || isAccountant) ? [
      { title: 'Fee Collection & Pending Dues', desc: 'Net payable, paid, and remaining fee balances.', pdf: '/reports/fees/export?format=pdf', xlsx: '/reports/fees/export?format=xlsx', name: 'TGS-Fee-Report' },
      { title: 'Transport Allocation Report', desc: 'Students assigned to buses and routes.', pdf: '/reports/transport/export?format=pdf', xlsx: '/reports/transport/export?format=xlsx', name: 'TGS-Transport-Report' },
    ] : []),
    ...((isAdmin || isLibrarian) ? [
      { title: 'Library Inventory Report', desc: 'Cataloged books, total copies, and availability.', pdf: '/reports/library/export?format=pdf', xlsx: '/reports/library/export?format=xlsx', name: 'TGS-Library-Inventory' },
    ] : []),
  ];

  return (
    <div>
      <PageHeader title="Reports & Exports" subtitle="Download formatted PDF and Excel (.xlsx) reports across school operations." />
      <div className="row g-3">
        {reports.map((r) => (
          <div className="col-md-6 col-lg-4" key={r.name}>
            <div className="tgs-card p-4 h-100 d-flex flex-column justify-content-between">
              <div>
                <h6 className="fw-bold mb-1">{r.title}</h6>
                <p className="small text-muted mb-3">{r.desc}</p>
              </div>
              <div className="d-flex gap-2">
                <button type="button" className="btn tgs-btn-navy btn-sm flex-grow-1" onClick={() => downloadFile(r.pdf, `${r.name}.pdf`)}>
                  <Download size={14} className="me-1" /> PDF
                </button>
                <button type="button" className="btn btn-outline-success btn-sm flex-grow-1" onClick={() => downloadFile(r.xlsx, `${r.name}.xlsx`)}>
                  <Download size={14} className="me-1" /> Excel
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
