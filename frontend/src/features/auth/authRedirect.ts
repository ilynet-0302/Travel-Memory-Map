export function buildAuthRedirectUrl(
  destination: string,
  origin = window.location.origin,
  basePath = import.meta.env.BASE_URL,
) {
  const safeDestination = destination.startsWith('/') && !destination.startsWith('//') ? destination : '/';
  const normalizedBase = `/${basePath.replace(/^\/+|\/+$/g, '')}`.replace(/^\/$/, '');
  const appPath = safeDestination === '/'
    ? `${normalizedBase}/`
    : `${normalizedBase}/${safeDestination.replace(/^\/+/, '')}`;
  return new URL(appPath.replace(/\/{2,}/g, '/'), origin).toString();
}
