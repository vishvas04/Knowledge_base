import { check } from 'k6';
import http from 'k6/http';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up
    { duration: '1m', target: 100 },   // Sustained load
    { duration: '30s', target: 0 },    // Ramp-down
  ],
};

export default function () {
  const question = `What is the policy for ${uuidv4()}?`; // Unique query

  const payload = JSON.stringify({
    question: question
  });

  const headers = {
    'Content-Type': 'application/json',
  };

  const res = http.post(`${BASE_URL}/api/query`, payload, { headers });

  check(res, {
    'Query success': (r) => r.status === 200,
    'Latency < 1s': (r) => r.timings.duration < 1000,
  });
}