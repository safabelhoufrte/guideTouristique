// Relative URLs: nginx (Docker) and proxy.conf.cjs (ng serve) route them to each service,
// whatever its host port, and the auth interceptor only adds the token to /api/ URLs.
export const API = {
  guides: '/api/guides',
  circuits: '/api/circuits',
  reservations: '/api/reservations',
};
