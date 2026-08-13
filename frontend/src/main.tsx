import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import 'maplibre-gl/dist/maplibre-gl.css';
import './styles/index.css';
import { AppProviders } from './app/providers/AppProviders';
import { AppRouter } from './app/router/AppRouter';

const redirectedPath = window.sessionStorage.getItem('spa-redirect');
if (redirectedPath) {
  window.sessionStorage.removeItem('spa-redirect');
  window.history.replaceState(null, '', `${import.meta.env.BASE_URL}${redirectedPath.replace(/^\//, '')}`);
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppProviders>
      <AppRouter />
    </AppProviders>
  </StrictMode>,
);
