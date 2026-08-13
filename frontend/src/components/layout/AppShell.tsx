import {
  CircleUserRound,
  Compass,
  GalleryHorizontalEnd,
  Globe2,
  LayoutDashboard,
  Map,
  LogOut,
  Plus,
  Settings,
  Sparkles,
} from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../features/auth/context/AuthContext';

const primaryNavigation = [
  { label: 'Overview', to: '/', icon: LayoutDashboard, end: true },
  { label: 'My trips', to: '/trips', icon: Compass },
  { label: 'World map', to: '/map', icon: Map },
  { label: 'Memories', to: '/memories', icon: GalleryHorizontalEnd },
];

const mobileNavigation = primaryNavigation.slice(0, 4);

function Brand() {
  return (
    <NavLink to="/" className="brand" aria-label="Travel Memory Map home">
      <span className="brand__mark" aria-hidden="true">
        <Globe2 size={22} strokeWidth={1.8} />
      </span>
      <span>
        <strong>Travel</strong>
        <small>memory map</small>
      </span>
    </NavLink>
  );
}

export function AppShell() {
  const { demoMode, session, signOut } = useAuth();
  const displayEmail = session?.user.email ?? 'Demo traveller';

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Brand />

        <nav className="sidebar__nav" aria-label="Main navigation">
          <span className="sidebar__label">EXPLORE</span>
          {primaryNavigation.map(({ label, to, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`}
            >
              <Icon size={19} strokeWidth={1.8} />
              <span>{label}</span>
            </NavLink>
          ))}

          <span className="sidebar__label sidebar__label--second">ACCOUNT</span>
          <NavLink
            to="/profile"
            className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`}
          >
            <CircleUserRound size={19} strokeWidth={1.8} />
            <span>Travel profile</span>
          </NavLink>
          <button className="nav-link nav-link--button" type="button" disabled>
            <Settings size={19} strokeWidth={1.8} />
            <span>Settings</span>
          </button>
        </nav>

        <div className="sidebar__footer">
          <NavLink to="/?create=1" className="button button--dark button--full">
            <Plus size={18} />
            New trip
          </NavLink>
          <div className="profile-chip">
            <span className="avatar avatar--ilia">IP</span>
            <span>
              <strong>{demoMode ? 'Iliya Petrov' : displayEmail}</strong>
              <small>{demoMode ? 'Urban explorer' : 'Signed in traveller'}</small>
            </span>
            {demoMode ? (
              <Sparkles size={15} aria-label="Travel profile active" />
            ) : (
              <button className="profile-chip__logout" type="button" onClick={() => void signOut()} aria-label="Sign out">
                <LogOut size={15} />
              </button>
            )}
          </div>
        </div>
      </aside>

      <div className="mobile-header">
        <Brand />
        <span className="avatar avatar--ilia">IP</span>
      </div>

      <main className="main-content">
        <Outlet />
      </main>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {mobileNavigation.map(({ label, to, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) => `mobile-nav__link${isActive ? ' is-active' : ''}`}
          >
            <Icon size={20} />
            <span>{label.replace('My ', '')}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
