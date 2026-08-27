import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const SERVICES = [
  { to: "/delay-events", title: "Delay Event Management" },
  { to: "/operations", title: "Operational Data" },
  { to: "/delay-events", title: "Recovery Planning" },
  { to: "/analytics", title: "Analytics & Reporting" },
  { to: "/history", title: "Recovery History" },
  { to: "/users", title: "User Management" },
];

export function HomePage() {
  const { user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setMenuOpen(false);
    }
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("keydown", handleKeyDown);
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <div className="home-page">
      <nav className="home-navbar">
        <div className="home-navbar-brand">
          <img src="/images/img1.png" alt="RwandAir" />
          <span>RwandAir OCC</span>
        </div>
        <div className="home-services-menu" ref={menuRef}>
          <button
            type="button"
            className="home-services-toggle"
            aria-label="View all services"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span />
            <span />
            <span />
          </button>

          {menuOpen && (
            <div className="home-services-dropdown" role="menu" aria-label="All services">
              <div className="home-services-dropdown-title">All Services</div>
              <ul className="home-services-modal-list">
                {SERVICES.map((service) => (
                  <li key={service.title}>
                    <Link to={service.to} onClick={() => setMenuOpen(false)}>
                      <span>{service.title}</span>
                      <span className="home-services-modal-arrow">&rarr;</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </nav>

      <section className="home-banner">
        <div className="home-banner-overlay" />
        <div className="home-banner-content">
          <span className="home-banner-eyebrow">Cost-Effective Aircraft Delay Recovery System</span>
          <h1>Every delay meets a plan, in seconds.</h1>
          <p>
            When a flight is disrupted, RwandAir's Operations Control Centre has minutes to decide, not hours.
            This system weighs every option, absorb, cancel, swap the aircraft, swap the crew, or reroute
            passengers, and costs and ranks each one before the decision has to be made.
          </p>
          <Link className="home-banner-cta" to={user ? "/delay-events" : "/login"}>
            {user ? "Open the Dashboard" : "Sign in to the OCC"}
          </Link>
        </div>
      </section>

      <section className="home-stats">
        <Link to="/delay-events">
          <strong>5</strong>
          <span>recovery strategies costed and ranked automatically</span>
        </Link>
        <Link to="/history">
          <strong>MILP + RL</strong>
          <span>hybrid engine that learns from every real outcome</span>
        </Link>
        <Link to="/analytics">
          <strong>Fuel · Crew · Pax · Slot · MRO</strong>
          <span>full cost breakdown behind every recommendation</span>
        </Link>
      </section>

      <div className="home-cabin-band">
        <div className="home-cabin-band-overlay">
          <p>Every seat on board depends on a fast, well-costed recovery decision on the ground.</p>
        </div>
      </div>

      <section className="home-promo">
        <img src="/images/hero-boarding.jpg" alt="Passengers boarding an aircraft" />
        <div className="home-promo-text">
          <span>Built for the OCC</span>
          <h2>Passengers back on their way, faster.</h2>
          <p>
            A hybrid engine pairs Mixed-Integer Linear Programming with Reinforcement Learning that keeps
            improving from every real outcome, so recommendations get sharper the more RwandAir flies. Built
            for the Operations Control Centre, Crew Scheduling, Maintenance, and Commercial Services teams who
            keep Rwanda's flag carrier moving.
          </p>
        </div>
      </section>

      <footer className="home-footer">
        <div className="home-footer-main">
          <div className="home-footer-brand">
            <strong>RwandAir OCC</strong>
            <p>
              Cost-optimal aircraft delay recovery for RwandAir. Connecting Africa, one decision at a time.
            </p>
          </div>

          <div className="home-footer-contact">
            <h4>Contact</h4>
            <a href="tel:+250788177000">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M6.62 10.79a15.05 15.05 0 006.59 6.59l2.2-2.2a1 1 0 011.02-.24 11.36 11.36 0 003.57.57 1 1 0 011 1V20a1 1 0 01-1 1C10.61 21 3 13.39 3 4a1 1 0 011-1h3.5a1 1 0 011 1 11.36 11.36 0 00.57 3.57 1 1 0 01-.25 1.02l-2.2 2.2z" />
              </svg>
              +250 788 177 000
            </a>
            <a href="mailto:info@rwandair.com">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2 6a2 2 0 012-2h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm2 0l8 6 8-6" stroke="currentColor" strokeWidth="1.6" fill="none" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              info@rwandair.com
            </a>
          </div>

          <div>
            <h4>Follow RwandAir</h4>
            <div className="home-footer-social">
              <a href="https://www.instagram.com/flyrwandair/" target="_blank" rel="noreferrer" aria-label="Instagram">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                  <rect x="3" y="3" width="18" height="18" rx="5" />
                  <circle cx="12" cy="12" r="4" />
                  <circle cx="17.2" cy="6.8" r="1" fill="currentColor" stroke="none" />
                </svg>
              </a>
              <a href="https://www.linkedin.com/company/flyrwandair/" target="_blank" rel="noreferrer" aria-label="LinkedIn">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M4.98 3.5a2.5 2.5 0 11-.02 5 2.5 2.5 0 01.02-5zM3 9h4v12H3V9zm7 0h3.8v1.7h.05c.53-1 1.83-2.05 3.77-2.05 4.03 0 4.78 2.65 4.78 6.1V21H18v-5.4c0-1.3-.02-2.96-1.81-2.96-1.82 0-2.1 1.42-2.1 2.87V21H10V9z" />
                </svg>
              </a>
              <a href="https://www.facebook.com/FlyRwandAir/" target="_blank" rel="noreferrer" aria-label="Facebook">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13.5 21v-7.5H16l.5-3.5h-3V8c0-1 .3-1.7 1.7-1.7H16.5V3.2C16.2 3.1 15.2 3 14 3c-2.5 0-4.2 1.5-4.2 4.3V10H7v3.5h2.8V21h3.7z" />
                </svg>
              </a>
              <a href="https://twitter.com/FlyRwandAir" target="_blank" rel="noreferrer" aria-label="X (Twitter)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.3 3H21l-6.7 7.6L22 21h-6.3l-5-6.5-5.7 6.5H2.3l7.2-8.2L2 3h6.4l4.5 6L18.3 3zm-1.1 16h1.7L7.9 5H6.1l11.1 14z" />
                </svg>
              </a>
            </div>
          </div>
        </div>
        <div className="home-footer-bottom">© {new Date().getFullYear()} RwandAir. All rights reserved.</div>
      </footer>
    </div>
  );
}
