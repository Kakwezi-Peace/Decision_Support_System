import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { ROLE_LABELS } from "./auth/permissions";

function initials(fullName: string | undefined) {
  if (!fullName) return "?";
  const parts = fullName.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? "" : "";
  return (first + last).toUpperCase();
}

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  function goToProfile() {
    setMenuOpen(false);
    navigate("/profile");
  }

  return (
    <>
      <header className="app-header">
        <div className="brand">
          <img src="/images/img1.png" alt="RwandAir" />
          <div>
            <h1>RwandAir OCC</h1>
            <div className="subtitle">Cost-Effective Aircraft Delay Recovery System</div>
          </div>
        </div>
        <nav className="app-nav">
          <NavLink to="/delay-events" className={({ isActive }) => (isActive ? "active" : "")}>
            Delay Events
          </NavLink>
          <NavLink to="/operations" className={({ isActive }) => (isActive ? "active" : "")}>
            Operational Data
          </NavLink>
          <NavLink to="/analytics" className={({ isActive }) => (isActive ? "active" : "")}>
            Analytics
          </NavLink>
          <NavLink to="/history" className={({ isActive }) => (isActive ? "active" : "")}>
            History
          </NavLink>
          {user?.role === "ADMIN" && (
            <NavLink to="/users" className={({ isActive }) => (isActive ? "active" : "")}>
              Users
            </NavLink>
          )}
          <div className="user-menu" ref={menuRef}>
            <button
              type="button"
              className="user-avatar-btn"
              aria-label="Account menu"
              onClick={() => setMenuOpen((open) => !open)}
            >
              <span className="user-avatar">{initials(user?.fullName)}</span>
            </button>
            {menuOpen && (
              <div className="user-menu-dropdown">
                <div className="user-menu-header">
                  <span className="user-avatar user-avatar--lg">{initials(user?.fullName)}</span>
                  <div>
                    <div className="user-menu-name">{user?.fullName}</div>
                    <div className="user-menu-role">{user ? ROLE_LABELS[user.role] : ""}</div>
                    <div className="user-menu-username">@{user?.username}</div>
                  </div>
                </div>
                <button type="button" className="user-menu-item" onClick={goToProfile}>
                  My Profile
                </button>
                <button type="button" className="user-menu-item user-menu-item--danger" onClick={handleLogout}>
                  Log out
                </button>
              </div>
            )}
          </div>
        </nav>
      </header>
      <main className="app-body">
        <Outlet />
      </main>
    </>
  );
}
