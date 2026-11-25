import React, { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X, MapPin, Phone, Mail, LogIn, GraduationCap, ChevronDown } from 'lucide-react';
import { SchoolLogo } from '../components/CommonUI';
import { useAuth } from '../context/AuthContext';
import { usePublicOverview } from '../hooks/usePublicOverview';

export default function PublicLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { isAuthenticated, defaultRoute } = useAuth();
  const { getSection, parseMeta } = usePublicOverview();
  const contactSection = getSection('CONTACT_INFO');
  const contactMeta = parseMeta(contactSection);

  const navLinkClass = ({ isActive }) =>
    `nav-link px-2 py-2 fw-medium small ${isActive ? 'text-warning fw-semibold' : 'text-white'}`;

  return (
    <div className="d-flex flex-column min-vh-100 bg-white">
      {/* Top Info Strip */}
      <div className="py-1 px-3 text-white-50 small" style={{ backgroundColor: '#091a32' }}>
        <div className="container d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div className="d-flex align-items-center gap-3 flex-wrap">
            <span className="d-inline-flex align-items-center gap-1">
              <MapPin size={13} className="text-warning" /> Kalluru, Khammam, Telangana, India
            </span>
            <span className="d-inline-flex align-items-center gap-1">
              <GraduationCap size={13} className="text-warning" /> LKG to 10th Class
            </span>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span>{contactMeta.officeHours || 'Mon – Sat, 9:00 AM – 4:30 PM'}</span>
            <Link to="/admission-enquiry" className="text-warning text-decoration-none fw-semibold">
              Admissions Open
            </Link>
          </div>
        </div>
      </div>

      {/* Main Navigation */}
      <header className="sticky-top shadow-sm" style={{ backgroundColor: '#0f294a', zIndex: 1030 }}>
        <div className="container py-2 d-flex align-items-center justify-content-between gap-3">
          <Link to="/" className="d-flex align-items-center gap-2 text-decoration-none">
            <SchoolLogo variant="compact" height={44} />
            <div>
              <div className="text-white fw-bold tgs-serif" style={{ fontSize: '1.05rem', lineHeight: 1.15 }}>
                Telangana Grammar School
              </div>
              <div className="text-warning" style={{ fontSize: '0.7rem', letterSpacing: '0.06em' }}>
                KALLURU, KHAMMAM
              </div>
            </div>
          </Link>

          <nav className="d-none d-xl-flex align-items-center gap-1">
            <NavLink to="/" end className={navLinkClass}>Home</NavLink>
            <div className="dropdown">
              <button
                className="btn btn-link nav-link px-2 py-2 text-white small fw-medium dropdown-toggle text-decoration-none"
                type="button"
                data-bs-toggle="dropdown"
              >
                About <ChevronDown size={13} />
              </button>
              <ul className="dropdown-menu shadow border-0">
                <li><Link className="dropdown-item small" to="/about">About Us</Link></li>
                <li><Link className="dropdown-item small" to="/principal-message">Principal&apos;s Message</Link></li>
                <li><Link className="dropdown-item small" to="/vision-mission">Vision &amp; Mission</Link></li>
                <li><Link className="dropdown-item small" to="/achievements">Achievements</Link></li>
              </ul>
            </div>
            <div className="dropdown">
              <button
                className="btn btn-link nav-link px-2 py-2 text-white small fw-medium dropdown-toggle text-decoration-none"
                type="button"
                data-bs-toggle="dropdown"
              >
                Academics <ChevronDown size={13} />
              </button>
              <ul className="dropdown-menu shadow border-0">
                <li><Link className="dropdown-item small" to="/academics">Academics Overview</Link></li>
                <li><Link className="dropdown-item small" to="/classes">Classes (LKG – 10th)</Link></li>
                <li><Link className="dropdown-item small" to="/methodology">Teaching Methodology</Link></li>
                <li><Link className="dropdown-item small" to="/facilities">Facilities</Link></li>
              </ul>
            </div>
            <NavLink to="/events" className={navLinkClass}>Events</NavLink>
            <NavLink to="/gallery" className={navLinkClass}>Gallery</NavLink>
            <NavLink to="/notices" className={navLinkClass}>Notices</NavLink>
            <NavLink to="/admission-enquiry" className={navLinkClass}>Admissions</NavLink>
            <NavLink to="/contact" className={navLinkClass}>Contact</NavLink>
          </nav>

          <div className="d-flex align-items-center gap-2">
            <Link
              to={isAuthenticated ? defaultRoute : '/login'}
              className="btn tgs-btn-gold btn-sm px-3 d-inline-flex align-items-center gap-1"
            >
              <LogIn size={15} />
              <span>{isAuthenticated ? 'Go to Portal' : 'ERP Login'}</span>
            </Link>
            <button
              type="button"
              className="btn btn-outline-light btn-sm d-xl-none"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-label="Toggle Menu"
            >
              {mobileOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileOpen && (
          <div className="d-xl-none border-top border-secondary px-3 py-3" style={{ backgroundColor: '#0c213c' }}>
            <div className="row g-2">
              {[
                ['/', 'Home'],
                ['/about', 'About Us'],
                ['/principal-message', "Principal's Message"],
                ['/vision-mission', 'Vision & Mission'],
                ['/academics', 'Academics'],
                ['/classes', 'Classes (LKG–10th)'],
                ['/methodology', 'Methodology'],
                ['/facilities', 'Facilities'],
                ['/events', 'Events & Activities'],
                ['/gallery', 'Gallery'],
                ['/achievements', 'Achievements'],
                ['/notices', 'Notices'],
                ['/admission-enquiry', 'Admission Enquiry'],
                ['/contact', 'Contact Us'],
              ].map(([to, label]) => (
                <div className="col-6" key={to}>
                  <Link
                    to={to}
                    onClick={() => setMobileOpen(false)}
                    className="d-block text-white text-decoration-none small py-1 px-2 rounded"
                  >
                    {label}
                  </Link>
                </div>
              ))}
            </div>
          </div>
        )}
        <div className="tgs-hero-accent" />
      </header>

      <main className="flex-grow-1">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="text-white pt-5 pb-4 mt-auto" style={{ backgroundColor: '#091a32' }}>
        <div className="container">
          <div className="row g-4 pb-4 border-bottom border-secondary border-opacity-25">
            <div className="col-lg-4">
              <div className="d-flex align-items-center gap-2 mb-3">
                <SchoolLogo variant="compact" height={42} />
                <div>
                  <h6 className="mb-0 fw-bold tgs-serif text-white">Telangana Grammar School</h6>
                  <small className="text-warning">Kalluru, Khammam, Telangana</small>
                </div>
              </div>
              <p className="small text-white-50 mb-0">
                Digitized school campus &amp; learning portal serving students from LKG to 10th Class. All campus notices
                and institutional details are managed by school administration.
              </p>
            </div>
            <div className="col-sm-6 col-lg-2">
              <h6 className="fw-bold text-warning small text-uppercase mb-3">Explore</h6>
              <ul className="list-unstyled small d-flex flex-column gap-2">
                <li><Link to="/about" className="text-white-50 text-decoration-none">About Us</Link></li>
                <li><Link to="/principal-message" className="text-white-50 text-decoration-none">Principal&apos;s Message</Link></li>
                <li><Link to="/vision-mission" className="text-white-50 text-decoration-none">Vision &amp; Mission</Link></li>
                <li><Link to="/achievements" className="text-white-50 text-decoration-none">Achievements</Link></li>
              </ul>
            </div>
            <div className="col-sm-6 col-lg-3">
              <h6 className="fw-bold text-warning small text-uppercase mb-3">Academics</h6>
              <ul className="list-unstyled small d-flex flex-column gap-2">
                <li><Link to="/classes" className="text-white-50 text-decoration-none">Classes (LKG – 10th)</Link></li>
                <li><Link to="/methodology" className="text-white-50 text-decoration-none">Teaching Methodology</Link></li>
                <li><Link to="/facilities" className="text-white-50 text-decoration-none">Campus Facilities</Link></li>
                <li><Link to="/admission-enquiry" className="text-white-50 text-decoration-none">Admission Enquiry</Link></li>
              </ul>
            </div>
            <div className="col-lg-3">
              <h6 className="fw-bold text-warning small text-uppercase mb-3">Campus Office</h6>
              <ul className="list-unstyled small text-white-50 d-flex flex-column gap-2 mb-0">
                <li className="d-flex gap-2">
                  <MapPin size={15} className="text-warning flex-shrink-0 mt-1" />
                  <span>{contactMeta.address || 'Kalluru, Khammam District, Telangana, India'}</span>
                </li>
                <li className="d-flex gap-2">
                  <Phone size={15} className="text-warning flex-shrink-0 mt-1" />
                  <span>{contactMeta.phone || '[Official Phone - Update in Admin CMS]'}</span>
                </li>
                <li className="d-flex gap-2">
                  <Mail size={15} className="text-warning flex-shrink-0 mt-1" />
                  <span>{contactMeta.email || '[Official Email - Update in Admin CMS]'}</span>
                </li>
              </ul>
            </div>
          </div>
          <div className="pt-3 d-flex flex-wrap justify-content-between align-items-center small text-white-50 gap-2">
            <span>&copy; {new Date().getFullYear()} Telangana Grammar School, Kalluru.</span>
            <div className="d-flex gap-3">
              <Link to="/notices" className="text-white-50 text-decoration-none">Notices</Link>
              <Link to="/contact" className="text-white-50 text-decoration-none">Contact</Link>
              <Link to="/login" className="text-warning text-decoration-none fw-semibold">ERP Login</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
