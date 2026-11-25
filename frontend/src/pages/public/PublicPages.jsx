import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  ArrowRight, Award, BookOpen, Calendar, CheckCircle2, Compass, GraduationCap,
  MapPin, Megaphone, Phone, Mail, ShieldCheck, Sparkles, Building2, Image as ImageIcon, Send
} from 'lucide-react';
import api, { extractErrorMessage } from '../../services/api';
import { usePublicOverview } from '../../hooks/usePublicOverview';
import { LoadingSkeleton, SchoolLogo, EmptyState } from '../../components/CommonUI';
import { formatDate } from '../../utils/formatters';

function PublicSectionBanner({ title, subtitle, badge = 'Telangana Grammar School' }) {
  return (
    <section className="tgs-hero py-5">
      <div className="container py-2">
        <span className="badge bg-warning text-dark fw-semibold mb-2">{badge}</span>
        <h1 className="fw-bold mb-2">{title}</h1>
        {subtitle && <p className="text-white-50 mb-0 col-lg-8">{subtitle}</p>}
      </div>
    </section>
  );
}

function CmsRichPage({ sectionKey, fallbackTitle, fallbackSubtitle, badge, icon: Icon }) {
  const { loading, getSection, parseMeta } = usePublicOverview();
  const sec = getSection(sectionKey);
  const meta = parseMeta(sec);

  if (loading) {
    return (
      <div className="container py-5">
        <LoadingSkeleton rows={6} />
      </div>
    );
  }

  return (
    <>
      <PublicSectionBanner title={sec?.title || fallbackTitle} subtitle={sec?.subtitle || fallbackSubtitle} badge={badge} />
      <section className="container py-5">
        <div className="row g-4">
          <div className="col-lg-8">
            <div className="tgs-card p-4 p-lg-5">
              <div className="d-flex align-items-center gap-2 text-primary fw-semibold small mb-3">
                {Icon && <Icon size={18} />}
                <span>Admin-Managed Institutional Information</span>
              </div>
              <div style={{ whiteSpace: 'pre-line', lineHeight: 1.75 }} className="text-secondary">
                {sec?.bodyContent || 'This section can be configured by school administration via the Website CMS module.'}
              </div>
              {Array.isArray(meta.items) && meta.items.length > 0 && (
                <div className="row g-3 mt-4">
                  {meta.items.map((item, idx) => (
                    <div className="col-md-6" key={idx}>
                      <div className="p-3 rounded-3 border bg-light h-100">
                        <div className="fw-bold text-dark mb-1">{item.name}</div>
                        <div className="small text-muted">{item.detail}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="col-lg-4">
            <div className="tgs-card p-4 bg-light">
              <SchoolLogo variant="main" height={54} className="mb-3" />
              <h6 className="fw-bold mb-2">Kalluru Campus</h6>
              <p className="small text-muted mb-3">
                Offering LKG to 10th Class education with unified academic tracking, transparent parent communication, and structured assessments.
              </p>
              <Link to="/admission-enquiry" className="btn tgs-btn-navy btn-sm w-100 mb-2">
                Submit Admission Enquiry
              </Link>
              <Link to="/login" className="btn btn-outline-primary btn-sm w-100">
                Portal Login
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}

export function HomePage() {
  const { loading, error, getSection, parseMeta, classes, announcements, events, gallery } = usePublicOverview();
  const hero = getSection('HOME_HERO');
  const about = getSection('ABOUT_US');
  const facilities = getSection('FACILITIES');
  const whyChoose = getSection('WHY_CHOOSE_US');
  const contact = getSection('CONTACT_INFO');
  const facilitiesMeta = parseMeta(facilities);
  const contactMeta = parseMeta(contact);

  return (
    <>
      {/* Hero */}
      <section className="tgs-hero py-5">
        <div className="container py-4">
          <div className="row align-items-center g-4">
            <div className="col-lg-7">
              <div className="d-inline-flex align-items-center gap-2 bg-white bg-opacity-10 px-3 py-1 rounded-pill small mb-3">
                <MapPin size={14} className="text-warning" />
                <span>Kalluru, Khammam District, Telangana, India</span>
              </div>
              <h1 className="display-5 fw-bold mb-3">
                {hero?.title || 'Nurturing Knowledge, Character & Future Leaders'}
              </h1>
              <p className="lead text-white-50 mb-4" style={{ fontSize: '1.08rem' }}>
                {hero?.bodyContent ||
                  'Providing structured, value-based school education from LKG to 10th Class in Kalluru, Khammam.'}
              </p>
              <div className="d-flex flex-wrap gap-3">
                <Link to="/admission-enquiry" className="btn tgs-btn-gold px-4 py-2 d-inline-flex align-items-center gap-2">
                  <span>Admission Enquiry</span>
                  <ArrowRight size={17} />
                </Link>
                <Link to="/classes" className="btn btn-outline-light px-4 py-2">
                  Explore Classes (LKG – 10th)
                </Link>
              </div>
            </div>
            <div className="col-lg-5">
              <div className="bg-white text-dark rounded-4 p-4 shadow-lg">
                <SchoolLogo variant="main" height={58} className="mb-3" />
                <div className="row g-3 text-center pt-2 border-top">
                  <div className="col-4">
                    <div className="fw-bold text-primary fs-5">LKG – X</div>
                    <div className="text-muted small">Grade Levels</div>
                  </div>
                  <div className="col-4">
                    <div className="fw-bold text-success fs-5">ERP</div>
                    <div className="text-muted small">Digital Portal</div>
                  </div>
                  <div className="col-4">
                    <div className="fw-bold text-warning fs-5">CMS</div>
                    <div className="text-muted small">Live Updates</div>
                  </div>
                </div>
                <div className="mt-3 pt-3 border-top d-flex justify-content-between align-items-center small">
                  <span className="text-muted">Student, Parent &amp; Faculty Access</span>
                  <Link to="/login" className="fw-semibold text-decoration-none">Sign In &rarr;</Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {error && (
        <div className="container mt-4">
          <div className="alert alert-warning small mb-0">{error}</div>
        </div>
      )}

      {/* About + Why Choose Us */}
      <section className="container py-5">
        {loading ? (
          <LoadingSkeleton rows={4} />
        ) : (
          <div className="row g-4 align-items-stretch">
            <div className="col-lg-6">
              <div className="tgs-card p-4 h-100">
                <span className="badge bg-primary-subtle text-primary mb-2">About Our School</span>
                <h3 className="fw-bold mb-3">{about?.title || 'About Telangana Grammar School'}</h3>
                <p className="text-secondary mb-4" style={{ lineHeight: 1.7 }}>
                  {about?.bodyContent}
                </p>
                <div className="d-flex gap-2">
                  <Link to="/about" className="btn btn-outline-primary btn-sm">Read More</Link>
                  <Link to="/principal-message" className="btn btn-outline-secondary btn-sm">Principal&apos;s Message</Link>
                </div>
              </div>
            </div>
            <div className="col-lg-6">
              <div className="tgs-card p-4 h-100" style={{ backgroundColor: '#f8fafc' }}>
                <span className="badge bg-warning-subtle text-dark mb-2">Why Choose TGS</span>
                <h3 className="fw-bold mb-3">{whyChoose?.title || 'Why Choose Telangana Grammar School'}</h3>
                <p className="text-secondary mb-3" style={{ lineHeight: 1.7 }}>{whyChoose?.bodyContent}</p>
                <div className="row g-2 small">
                  {[
                    'Activity-based LKG & UKG learning',
                    'Structured 1st to 10th curriculum',
                    'Real-time parent attendance alerts',
                    'Transparent report cards & fee receipts',
                  ].map((pt, i) => (
                    <div className="col-sm-6 d-flex align-items-center gap-2" key={i}>
                      <CheckCircle2 size={16} className="text-success flex-shrink-0" />
                      <span>{pt}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Academic Classes Grid */}
      <section className="py-5 bg-light border-top border-bottom">
        <div className="container">
          <div className="d-flex flex-wrap justify-content-between align-items-end mb-4 gap-2">
            <div>
              <span className="badge bg-primary-subtle text-primary mb-1">Academic Wings</span>
              <h3 className="fw-bold mb-0">Classes from LKG to 10th Class</h3>
            </div>
            <Link to="/classes" className="btn btn-outline-primary btn-sm">View Full Curriculum</Link>
          </div>
          <div className="row g-3">
            {(classes.length > 0
              ? classes
              : [
                  'LKG', 'UKG', '1st Class', '2nd Class', '3rd Class', '4th Class',
                  '5th Class', '6th Class', '7th Class', '8th Class', '9th Class', '10th Class',
                ].map((name, idx) => ({ id: idx, name, category: idx < 2 ? 'PRE_PRIMARY' : idx < 7 ? 'PRIMARY' : 'HIGH_SCHOOL' }))
            ).map((cls) => (
              <div className="col-6 col-md-4 col-lg-2" key={cls.id}>
                <div className="tgs-card tgs-card-hover p-3 text-center h-100">
                  <GraduationCap size={22} className="text-primary mb-2" />
                  <div className="fw-bold text-dark">{cls.name}</div>
                  <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                    {String(cls.category || '').replace(/_/g, ' ')}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Facilities */}
      <section className="container py-5">
        <div className="d-flex flex-wrap justify-content-between align-items-end mb-4 gap-2">
          <div>
            <span className="badge bg-success-subtle text-success mb-1">Campus Infrastructure</span>
            <h3 className="fw-bold mb-0">{facilities?.title || 'Campus Facilities'}</h3>
          </div>
          <Link to="/facilities" className="btn btn-outline-secondary btn-sm">All Facilities</Link>
        </div>
        <div className="row g-3">
          {(facilitiesMeta.items || []).map((item, idx) => (
            <div className="col-md-6 col-lg-3" key={idx}>
              <div className="tgs-card p-4 h-100">
                <Building2 size={22} className="text-primary mb-2" />
                <h6 className="fw-bold mb-1">{item.name}</h6>
                <p className="small text-muted mb-0">{item.detail}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Announcements, Events & Gallery Preview */}
      <section className="py-5 bg-light border-top">
        <div className="container">
          <div className="row g-4">
            <div className="col-lg-6">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h4 className="fw-bold mb-0 d-flex align-items-center gap-2">
                  <Megaphone size={20} className="text-warning" /> Recent Notices
                </h4>
                <Link to="/notices" className="small fw-semibold text-decoration-none">View All &rarr;</Link>
              </div>
              {announcements.length === 0 ? (
                <EmptyState title="No public notices yet" description="School announcements published for public view will appear here." />
              ) : (
                <div className="d-flex flex-column gap-2">
                  {announcements.slice(0, 4).map((a) => (
                    <div key={a.id} className="tgs-card p-3">
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className="badge bg-primary-subtle text-primary">{a.category}</span>
                        <small className="text-muted">{formatDate(a.publishedAt)}</small>
                      </div>
                      <h6 className="fw-bold mb-1">{a.title}</h6>
                      <p className="small text-muted mb-0">{a.content}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="col-lg-6">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h4 className="fw-bold mb-0 d-flex align-items-center gap-2">
                  <Calendar size={20} className="text-primary" /> Upcoming Events &amp; Gallery
                </h4>
                <Link to="/events" className="small fw-semibold text-decoration-none">All Events &rarr;</Link>
              </div>
              <div className="d-flex flex-column gap-2 mb-3">
                {events.slice(0, 3).map((ev) => (
                  <div key={ev.id} className="tgs-card p-3">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="badge bg-warning-subtle text-dark">{ev.category}</span>
                      <small className="text-muted">{formatDate(ev.eventDate)}</small>
                    </div>
                    <h6 className="fw-bold mb-1">{ev.title}</h6>
                    <p className="small text-muted mb-0">{ev.venue ? `Venue: ${ev.venue} • ` : ''}{ev.description}</p>
                  </div>
                ))}
              </div>
              {gallery.length > 0 && (
                <div className="tgs-card p-3 d-flex align-items-center justify-content-between">
                  <div className="d-flex align-items-center gap-2">
                    <ImageIcon size={18} className="text-success" />
                    <span className="small fw-semibold">{gallery.length} campus gallery item(s) published</span>
                  </div>
                  <Link to="/gallery" className="btn btn-sm btn-outline-primary">Open Gallery</Link>
                </div>
              )}
            </div>
          </div>

          {/* Contact Strip */}
          <div className="tgs-card p-4 mt-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <h5 className="fw-bold mb-1">{contact?.title || 'Contact Telangana Grammar School'}</h5>
              <div className="small text-muted d-flex flex-wrap gap-3">
                <span><MapPin size={14} className="me-1" />{contactMeta.address || 'Kalluru, Khammam, Telangana'}</span>
                <span><Phone size={14} className="me-1" />{contactMeta.phone || '[Official Phone - Admin CMS]'}</span>
                <span><Mail size={14} className="me-1" />{contactMeta.email || '[Official Email - Admin CMS]'}</span>
              </div>
            </div>
            <Link to="/admission-enquiry" className="btn tgs-btn-navy">
              Apply / Enquire Now
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}

export function AboutPage() {
  return <CmsRichPage sectionKey="ABOUT_US" fallbackTitle="About Telangana Grammar School" fallbackSubtitle="Kalluru, Khammam, Telangana" badge="About Us" icon={BookOpen} />;
}

export function PrincipalMessagePage() {
  const { loading, getSection, parseMeta } = usePublicOverview();
  const sec = getSection('PRINCIPAL_MESSAGE');
  const meta = parseMeta(sec);
  if (loading) return <div className="container py-5"><LoadingSkeleton rows={5} /></div>;
  return (
    <>
      <PublicSectionBanner title={sec?.title || "Principal's Desk"} subtitle={sec?.subtitle || 'Message from the School Principal'} badge="Leadership" />
      <div className="container py-5">
        <div className="tgs-card p-4 p-lg-5">
          <div className="row g-4 align-items-center">
            <div className="col-md-3 text-center">
              <div className="p-4 rounded-4 bg-light border">
                <SchoolLogo variant="document" height={96} />
                <div className="fw-bold mt-3">{meta.principalName || '[To be updated by Admin]'}</div>
                <div className="small text-muted">{meta.designation || 'Principal, Telangana Grammar School'}</div>
              </div>
            </div>
            <div className="col-md-9">
              <h4 className="fw-bold mb-3">A Note to Parents &amp; Students</h4>
              <p className="text-secondary" style={{ whiteSpace: 'pre-line', lineHeight: 1.8 }}>
                {sec?.bodyContent}
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export function VisionMissionPage() {
  return <CmsRichPage sectionKey="VISION_MISSION" fallbackTitle="Vision & Mission" fallbackSubtitle="Guiding Principles of Telangana Grammar School" badge="Purpose" icon={Compass} />;
}

export function AcademicsPage() {
  const { classes } = usePublicOverview();
  return (
    <>
      <PublicSectionBanner title="Academics & Curriculum" subtitle="Structured educational progression from LKG to 10th Class in Kalluru, Khammam." badge="Academics" />
      <div className="container py-5">
        <div className="row g-4">
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100 border-top border-4 border-warning">
              <h5 className="fw-bold">Pre-Primary (LKG &amp; UKG)</h5>
              <p className="small text-muted mb-0">
                Activity-based assessment focusing on early literacy, rhymes, number play, motor skills, and descriptive teacher feedback rather than high-pressure numeric marks.
              </p>
            </div>
          </div>
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100 border-top border-4 border-primary">
              <h5 className="fw-bold">Primary Wing (1st – 5th Class)</h5>
              <p className="small text-muted mb-0">
                Core emphasis on English, Telugu, Mathematics, and Environmental Studies with continuous homework tracking and formative evaluations.
              </p>
            </div>
          </div>
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100 border-top border-4 border-success">
              <h5 className="fw-bold">High School (6th – 10th Class)</h5>
              <p className="small text-muted mb-0">
                Comprehensive languages, Mathematics, General Science, Social Studies, and Computing with periodic unit tests, half-yearly, and annual board-oriented assessments.
              </p>
            </div>
          </div>
        </div>
        <div className="tgs-card p-4 mt-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <h6 className="fw-bold mb-1">Configured Grade Levels ({classes.length || 12})</h6>
            <p className="small text-muted mb-0">Browse class structures and wings offered at Telangana Grammar School.</p>
          </div>
          <div className="d-flex gap-2">
            <Link to="/classes" className="btn tgs-btn-navy btn-sm">View Classes (LKG–10th)</Link>
            <Link to="/methodology" className="btn btn-outline-primary btn-sm">Teaching Methodology</Link>
          </div>
        </div>
      </div>
    </>
  );
}

export function ClassesPage() {
  const { classes, loading } = usePublicOverview();
  return (
    <>
      <PublicSectionBanner title="Classes (LKG to 10th Class)" subtitle="Complete K-10 academic structure at Telangana Grammar School, Kalluru." badge="Grade Levels" />
      <div className="container py-5">
        {loading ? <LoadingSkeleton rows={5} /> : (
          <div className="row g-3">
            {classes.map((cls) => (
              <div className="col-sm-6 col-md-4 col-lg-3" key={cls.id}>
                <div className="tgs-card tgs-card-hover p-4 h-100">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <GraduationCap size={24} className="text-primary" />
                    <span className="badge bg-light text-dark border">Level {cls.levelOrder}</span>
                  </div>
                  <h5 className="fw-bold mb-1">{cls.name}</h5>
                  <div className="small text-muted mb-2">{cls.category.replace(/_/g, ' ')}</div>
                  <span className={`badge ${cls.activityGraded ? 'bg-warning-subtle text-dark' : 'bg-primary-subtle text-primary'}`}>
                    {cls.activityGraded ? 'Activity-Based Grading' : 'Marks & Grade Scale'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

export function MethodologyPage() {
  return <CmsRichPage sectionKey="TEACHING_METHODOLOGY" fallbackTitle="Teaching Methodology" fallbackSubtitle="Pedagogy tailored for every developmental stage" badge="Pedagogy" icon={Sparkles} />;
}

export function FacilitiesPage() {
  return <CmsRichPage sectionKey="FACILITIES" fallbackTitle="Campus Facilities" fallbackSubtitle="Safe, organized learning environment in Kalluru" badge="Infrastructure" icon={Building2} />;
}

export function AchievementsPage() {
  return <CmsRichPage sectionKey="ACHIEVEMENTS" fallbackTitle="School Achievements & Milestones" fallbackSubtitle="Verified student & institutional recognitions managed by administration" badge="Recognition" icon={Award} />;
}

export function GalleryPage() {
  const { gallery, loading } = usePublicOverview();
  return (
    <>
      <PublicSectionBanner title="Campus Gallery" subtitle="Photographs and highlights published by school administration." badge="Gallery" />
      <div className="container py-5">
        {loading ? <LoadingSkeleton rows={4} /> : gallery.length === 0 ? (
          <EmptyState title="No gallery items published" description="Administrators can add campus photos from the Website CMS manager." />
        ) : (
          <div className="row g-4">
            {gallery.map((item) => (
              <div className="col-md-6 col-lg-4" key={item.id}>
                <div className="tgs-card overflow-hidden h-100">
                  <div className="bg-light d-flex align-items-center justify-content-center p-4" style={{ height: 190 }}>
                    <img
                      src={item.imageUrl}
                      alt={item.title}
                      style={{ maxHeight: '100%', maxWidth: '100%', objectFit: 'contain' }}
                    />
                  </div>
                  <div className="p-3">
                    <span className="badge bg-primary-subtle text-primary mb-2">{item.category}</span>
                    <h6 className="fw-bold mb-1">{item.title}</h6>
                    <p className="small text-muted mb-0">{item.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

export function EventsPage() {
  const { events, loading } = usePublicOverview();
  return (
    <>
      <PublicSectionBanner title="Events & Activities" subtitle="Academic calendar, cultural programs, and campus activities." badge="Calendar" />
      <div className="container py-5">
        {loading ? <LoadingSkeleton rows={4} /> : events.length === 0 ? (
          <EmptyState title="No upcoming public events" description="Published school events will appear here." />
        ) : (
          <div className="row g-3">
            {events.map((ev) => (
              <div className="col-md-6" key={ev.id}>
                <div className="tgs-card p-4 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="badge bg-warning-subtle text-dark">{ev.category}</span>
                    <span className="small fw-semibold text-primary">{formatDate(ev.eventDate)}</span>
                  </div>
                  <h5 className="fw-bold mb-2">{ev.title}</h5>
                  <p className="small text-secondary mb-2">{ev.description}</p>
                  {ev.venue && <div className="small text-muted"><MapPin size={14} className="me-1" />{ev.venue}</div>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

export function NoticesPage() {
  const { announcements, loading } = usePublicOverview();
  return (
    <>
      <PublicSectionBanner title="Notices & Announcements" subtitle="Official circulars and public announcements from Telangana Grammar School." badge="Bulletin" />
      <div className="container py-5">
        {loading ? <LoadingSkeleton rows={4} /> : announcements.length === 0 ? (
          <EmptyState title="No public notices available" description="Check back later for official school announcements." />
        ) : (
          <div className="d-flex flex-column gap-3">
            {announcements.map((a) => (
              <div className="tgs-card p-4" key={a.id}>
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                  <span className="badge bg-primary-subtle text-primary">{a.category}</span>
                  <small className="text-muted">Published: {formatDate(a.publishedAt)}</small>
                </div>
                <h5 className="fw-bold mb-2">{a.title}</h5>
                <p className="text-secondary mb-0" style={{ whiteSpace: 'pre-line' }}>{a.content}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

export function AdmissionEnquiryPage() {
  const { classes, getSection } = usePublicOverview();
  const admissionNotice = getSection('ADMISSION_NOTICE');
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState('');
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm();

  const onSubmit = async (values) => {
    setServerError('');
    try {
      await api.post('/public/enquiries', values);
      setSubmitted(true);
      reset();
    } catch (err) {
      setServerError(extractErrorMessage(err));
    }
  };

  return (
    <>
      <PublicSectionBanner
        title={admissionNotice?.title || 'Admission Enquiry (LKG – 10th Class)'}
        subtitle={admissionNotice?.subtitle || 'Telangana Grammar School • Kalluru, Khammam'}
        badge="Admissions"
      />
      <div className="container py-5">
        <div className="row g-4">
          <div className="col-lg-7">
            <div className="tgs-card p-4 p-lg-5">
              <h4 className="fw-bold mb-3">Submit Admission Enquiry</h4>
              {submitted && (
                <div className="alert alert-success d-flex align-items-center gap-2">
                  <CheckCircle2 size={18} />
                  <span>Thank you! Your admission enquiry has been received by our school office.</span>
                </div>
              )}
              {serverError && <div className="alert alert-danger small">{serverError}</div>}
              <form onSubmit={handleSubmit(onSubmit)} className="row g-3" noValidate>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Student Full Name *</label>
                  <input
                    className={`form-control ${errors.studentName ? 'is-invalid' : ''}`}
                    {...register('studentName', { required: 'Student name is required' })}
                    placeholder="Enter student name"
                  />
                  {errors.studentName && <div className="invalid-feedback">{errors.studentName.message}</div>}
                </div>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Parent / Guardian Name *</label>
                  <input
                    className={`form-control ${errors.parentName ? 'is-invalid' : ''}`}
                    {...register('parentName', { required: 'Parent name is required' })}
                    placeholder="Enter parent name"
                  />
                  {errors.parentName && <div className="invalid-feedback">{errors.parentName.message}</div>}
                </div>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Contact Phone *</label>
                  <input
                    className={`form-control ${errors.phone ? 'is-invalid' : ''}`}
                    {...register('phone', { required: 'Phone number is required' })}
                    placeholder="10-digit mobile number"
                  />
                  {errors.phone && <div className="invalid-feedback">{errors.phone.message}</div>}
                </div>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Email Address</label>
                  <input type="email" className="form-control" {...register('email')} placeholder="optional@example.com" />
                </div>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Applying for Class *</label>
                  <select
                    className={`form-select ${errors.applyingClassName ? 'is-invalid' : ''}`}
                    {...register('applyingClassName', { required: 'Please select a class' })}
                  >
                    <option value="">Select Class</option>
                    {(classes.length > 0
                      ? classes.map((c) => c.name)
                      : ['LKG', 'UKG', '1st Class', '2nd Class', '3rd Class', '4th Class', '5th Class', '6th Class', '7th Class', '8th Class', '9th Class', '10th Class']
                    ).map((name) => (
                      <option key={name} value={name}>{name}</option>
                    ))}
                  </select>
                  {errors.applyingClassName && <div className="invalid-feedback">{errors.applyingClassName.message}</div>}
                </div>
                <div className="col-md-6">
                  <label className="form-label small fw-semibold">Previous School (if any)</label>
                  <input className="form-control" {...register('previousSchool')} placeholder="Previous school name" />
                </div>
                <div className="col-12">
                  <label className="form-label small fw-semibold">Questions / Message</label>
                  <textarea rows={3} className="form-control" {...register('message')} placeholder="Any notes or transport requirements" />
                </div>
                <div className="col-12">
                  <button type="submit" className="btn tgs-btn-navy px-4 d-inline-flex align-items-center gap-2" disabled={isSubmitting}>
                    <Send size={16} />
                    <span>{isSubmitting ? 'Submitting...' : 'Submit Enquiry'}</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
          <div className="col-lg-5">
            <div className="tgs-card p-4 bg-light h-100">
              <h5 className="fw-bold mb-3">Admission Process</h5>
              <p className="small text-secondary">{admissionNotice?.bodyContent}</p>
              <ul className="small text-secondary d-flex flex-column gap-2 ps-3">
                <li>Submit the online enquiry or visit the Kalluru school office.</li>
                <li>Bring birth certificate, transfer certificate (for 1st–10th), and passport-size photographs.</li>
                <li>Upon admission, parent and student ERP login credentials are generated immediately.</li>
              </ul>
              <div className="mt-4 p-3 rounded-3 bg-white border small">
                <ShieldCheck size={18} className="text-success me-1" />
                Student personal information submitted here is visible only to authorized school administrators.
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export function ContactPage() {
  const { getSection, parseMeta, loading } = usePublicOverview();
  const sec = getSection('CONTACT_INFO');
  const meta = parseMeta(sec);
  if (loading) return <div className="container py-5"><LoadingSkeleton rows={4} /></div>;
  return (
    <>
      <PublicSectionBanner title={sec?.title || 'Contact Telangana Grammar School'} subtitle={sec?.subtitle || 'Kalluru, Khammam District, Telangana'} badge="Contact" />
      <div className="container py-5">
        <div className="row g-4">
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100">
              <MapPin size={24} className="text-primary mb-2" />
              <h6 className="fw-bold">School Address</h6>
              <p className="small text-muted mb-0">{meta.address || 'Kalluru, Khammam District, Telangana, India'}</p>
            </div>
          </div>
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100">
              <Phone size={24} className="text-primary mb-2" />
              <h6 className="fw-bold">Office Phone</h6>
              <p className="small text-muted mb-0">{meta.phone || '[Official Phone - Update in Admin CMS]'}</p>
            </div>
          </div>
          <div className="col-md-4">
            <div className="tgs-card p-4 h-100">
              <Mail size={24} className="text-primary mb-2" />
              <h6 className="fw-bold">Official Email &amp; Hours</h6>
              <p className="small text-muted mb-1">{meta.email || '[Official Email - Update in Admin CMS]'}</p>
              <p className="small text-muted mb-0">{meta.officeHours || 'Monday – Saturday, 9:00 AM – 4:30 PM'}</p>
            </div>
          </div>
        </div>
        <div className="tgs-card p-4 mt-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div className="small text-secondary">{sec?.bodyContent}</div>
          <Link to="/admission-enquiry" className="btn tgs-btn-navy btn-sm">Admission Enquiry Form</Link>
        </div>
      </div>
    </>
  );
}
