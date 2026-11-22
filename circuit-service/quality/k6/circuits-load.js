// Load test of the Circuit service public API (k6).
// Run: docker run --rm -i -e BASE_URL=http://host.docker.internal:8082 grafana/k6 run - < circuits-load.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

export const options = {
  scenarios: {
    visitors: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 }, // ramp up to 50 simultaneous visitors
        { duration: '1m', target: 50 }, // steady load
        { duration: '15s', target: 0 }, // ramp down
      ],
    },
  },
  // Pass criteria of the quality test.
  thresholds: {
    http_req_failed: ['rate<0.01'], // less than 1% errors
    http_req_duration: ['p(95)<500'], // 95% of requests under 500 ms
    'http_req_duration{route:list}': ['p(95)<500'],
    'http_req_duration{route:detail}': ['p(95)<300'],
    'http_req_duration{route:image}': ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const res = http.get(`${BASE_URL}/api/circuits`);
  const ids = res.json().map((c) => c.id);
  if (ids.length === 0) throw new Error('No circuit to test: load the demo data first.');
  return { ids };
}

export default function ({ ids }) {
  // A visitor opens the catalogue, then one circuit and its photo.
  const list = http.get(`${BASE_URL}/api/circuits`, { tags: { route: 'list' } });
  check(list, {
    'list 200': (r) => r.status === 200,
    'list is JSON without embedded photos': (r) => !r.body.includes('data:image'),
  });

  const id = ids[Math.floor(Math.random() * ids.length)];
  const detail = http.get(`${BASE_URL}/api/circuits/${id}`, { tags: { route: 'detail' } });
  check(detail, { 'detail 200': (r) => r.status === 200 });

  // A circuit without a photo answers 404 by design: not an error for this test.
  const image = http.get(`${BASE_URL}/api/circuits/${id}/image`, {
    tags: { route: 'image' },
    responseCallback: http.expectedStatuses(200, 404),
  });
  check(image, { 'image 200 or 404': (r) => r.status === 200 || r.status === 404 });

  sleep(1); // think time between two visitors' actions
}
