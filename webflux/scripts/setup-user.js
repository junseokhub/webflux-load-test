import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    iterations: 1000, // 1000명 순차 생성
};

export default function () {
    const username = `user_${__ITER}`;
    const res = http.post(
        'http://localhost:8080/api/auth/register',
        JSON.stringify({ username, password: 'test1234' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'ok': (r) => r.status === 201 || r.status === 409 });
}