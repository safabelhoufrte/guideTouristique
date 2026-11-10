// Override these URLs when running the teammates' APIs on different host ports.
module.exports = {
  '/api/circuits': {
    target: process.env.CIRCUIT_API_URL || 'http://localhost:8082',
    secure: false,
  },
  '/api/guides': { target: process.env.GUIDE_API_URL || 'http://localhost:8081', secure: false },
  '/api/reservations': {
    target: process.env.RESERVATION_API_URL || 'http://localhost:8083',
    secure: false,
  },
  '/api/avis': { target: process.env.AVIS_API_URL || 'http://localhost:8084', secure: false },
};
