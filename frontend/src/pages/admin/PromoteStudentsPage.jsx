import React, { useEffect, useState } from 'react';
import { ArrowUpRight } from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PageHeader, LoadingSkeleton, StatusBadge } from '../../components/CommonUI';

export default function PromoteStudentsPage() {
  const toast = useToast();
  const [years, setYears] = useState([]);
  const [classes, setClasses] = useState([]);
  const [sections, setSections] = useState([]);
  const [fromYearId, setFromYearId] = useState('');
  const [toYearId, setToYearId] = useState('');
  const [sourceClassId, setSourceClassId] = useState('');
  const [targetClassId, setTargetClassId] = useState('');
  const [targetSectionId, setTargetSectionId] = useState('');
  const [students, setStudents] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [promoting, setPromoting] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get('/academics/academic-years'),
      api.get('/academics/classes'),
      api.get('/academics/sections'),
    ]).then(([yRes, cRes, sRes]) => {
      const yList = yRes.data?.data || [];
      setYears(yList);
      const curr = yList.find((y) => y.current);
      if (curr) setFromYearId(String(curr.id));
      setClasses(cRes.data?.data || []);
      setSections(sRes.data?.data || []);
    }).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!fromYearId || !sourceClassId) {
      setStudents([]);
      setSelectedIds([]);
      return;
    }
    api.get(`/students?academicYearId=${fromYearId}&classId=${sourceClassId}&status=ACTIVE&size=200`)
      .then((res) => {
        const list = res.data?.data?.content || [];
        setStudents(list);
        setSelectedIds(list.map((s) => s.id));
      });
  }, [fromYearId, sourceClassId]);

  const targetSections = sections.filter(
    (s) => (!targetClassId || String(s.classId) === String(targetClassId)) && (!toYearId || String(s.academicYearId) === String(toYearId))
  );

  const toggle = (id) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const handlePromote = async () => {
    if (!fromYearId || !toYearId || !targetClassId || !targetSectionId || selectedIds.length === 0) {
      toast.warning('Select source year, target year, target class/section, and at least one student.');
      return;
    }
    setPromoting(true);
    try {
      const res = await api.post('/students/promote', {
        fromAcademicYearId: Number(fromYearId),
        toAcademicYearId: Number(toYearId),
        targetClassId: Number(targetClassId),
        targetSectionId: Number(targetSectionId),
        studentIds: selectedIds,
        remarks: 'Annual Academic Promotion',
      });
      toast.success(`Promoted ${(res.data?.data || []).length} student(s) while preserving previous academic records.`);
      setSelectedIds([]);
      setStudents([]);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setPromoting(false);
    }
  };

  if (loading) return <LoadingSkeleton rows={4} />;

  return (
    <div>
      <PageHeader
        title="Student Promotion Workflow"
        subtitle="Promote eligible students to the next academic year & class while preserving historical attendance and results."
      />

      <div className="tgs-card p-4 mb-4">
        <div className="row g-3">
          <div className="col-md-3">
            <label className="form-label small fw-semibold">From Academic Year</label>
            <select className="form-select form-select-sm" value={fromYearId} onChange={(e) => setFromYearId(e.target.value)}>
              <option value="">Select Year</option>
              {years.map((y) => <option key={y.id} value={y.id}>{y.name} {y.current ? '(Current)' : ''}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-semibold">Source Class</label>
            <select className="form-select form-select-sm" value={sourceClassId} onChange={(e) => setSourceClassId(e.target.value)}>
              <option value="">Select Class</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-semibold">To Academic Year</label>
            <select className="form-select form-select-sm" value={toYearId} onChange={(e) => setToYearId(e.target.value)}>
              <option value="">Select Target Year</option>
              {years.map((y) => <option key={y.id} value={y.id}>{y.name}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-semibold">Target Class</label>
            <select className="form-select form-select-sm" value={targetClassId} onChange={(e) => { setTargetClassId(e.target.value); setTargetSectionId(''); }}>
              <option value="">Select Class</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-semibold">Target Section</label>
            <select className="form-select form-select-sm" value={targetSectionId} onChange={(e) => setTargetSectionId(e.target.value)}>
              <option value="">Select Section</option>
              {targetSections.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.academicYearName})</option>)}
            </select>
          </div>
        </div>
      </div>

      <div className="tgs-card p-4">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h6 className="fw-bold mb-0">Eligible Students ({selectedIds.length} selected)</h6>
          <button type="button" className="btn tgs-btn-navy btn-sm" disabled={promoting || selectedIds.length === 0} onClick={handlePromote}>
            <ArrowUpRight size={15} className="me-1" /> {promoting ? 'Promoting...' : 'Promote Selected Students'}
          </button>
        </div>
        {students.length === 0 ? (
          <p className="text-muted small mb-0">Select a source academic year and class to load students.</p>
        ) : (
          <table className="table tgs-table table-sm mb-0">
            <thead><tr><th style={{ width: 40 }}></th><th>Adm. No</th><th>Name</th><th>Current Section</th><th>Status</th></tr></thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td><input type="checkbox" checked={selectedIds.includes(s.id)} onChange={() => toggle(s.id)} /></td>
                  <td>{s.admissionNumber}</td>
                  <td className="fw-semibold">{s.fullName}</td>
                  <td>{s.className} - {s.sectionName}</td>
                  <td><StatusBadge status={s.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
