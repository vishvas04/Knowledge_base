import { check } from 'k6';
import http from 'k6/http';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';


const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PDF_FILE = open('./test-data/test.pdf', 'b');

export const options = {
  scenarios: {
    upload: {
      executor: 'constant-vus',
      vus: 10,
      duration: '1m',
    },
  },
};

export default function () {
  const formData = new FormData();
  formData.append('file', http.file(PDF_FILE, 'test.pdf', 'application/pdf'));

  const headers = {
    'Content-Type': `multipart/form-data; boundary=${formData.boundary}`,
  };

  const res = http.post(`${BASE_URL}/api/documents`, formData.body(), { headers });

  check(res, {
    'Upload success': (r) => r.status === 200,
    'Response time < 2s': (r) => r.timings.duration < 2000,
  });
}