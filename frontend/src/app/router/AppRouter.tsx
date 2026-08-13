import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '../../components/layout/AppShell';
import { RequireAuth } from '../../features/auth/components/RequireAuth';
import { DashboardPage } from '../../pages/DashboardPage';
import { PlaceholderPage } from '../../pages/PlaceholderPage';
import { TripsPage } from '../../pages/TripsPage';

const LoginPage = lazy(() => import('../../pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const TripDetailPage = lazy(() => import('../../pages/TripDetailPage').then((module) => ({ default: module.TripDetailPage })));
const JoinTripPage = lazy(() => import('../../pages/JoinTripPage').then((module) => ({ default: module.JoinTripPage })));

function PageLoader() {
  return <div className="auth-loading"><span className="auth-loading__mark">◎</span><span>Unfolding the map…</span></div>;
}

export function AppRouter() {
  return (
    <BrowserRouter basename={import.meta.env.BASE_URL}>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="login" element={<LoginPage />} />
          <Route path="join/:inviteToken" element={<JoinTripPage />} />
          <Route element={<RequireAuth />}>
            <Route element={<AppShell />}>
              <Route index element={<DashboardPage />} />
              <Route path="trips" element={<TripsPage />} />
              <Route path="trips/:tripId" element={<TripDetailPage />} />
              <Route
                path="map"
                element={
                  <PlaceholderPage
                    eyebrow="World map"
                    title="Your travels, at a glance"
                    description="The complete scratch map arrives in Phase 5. Your visited countries and trip routes will live here."
                  />
                }
              />
              <Route
                path="memories"
                element={
                  <PlaceholderPage
                    eyebrow="Memories"
                    title="Every photo has a place"
                    description="Photo upload, EXIF detection and the memory gallery are part of the next product slice."
                  />
                }
              />
              <Route
                path="profile"
                element={
                  <PlaceholderPage
                    eyebrow="Travel profile"
                    title="Your travel DNA is taking shape"
                    description="Statistics and a deterministic travel personality will be calculated from completed trips."
                  />
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
