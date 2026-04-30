import http from 'k6/http';
import { check } from 'k6';

export const options = {
    setupTimeout: '300s',
    stages: [
        { duration: '30s', target: 100 },
        { duration: '60s', target: 500 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        // 에러율 99% 이하 → 409는 에러 아니니까 여유있게
        http_req_failed: ['rate<0.99'],
        http_req_duration: ['p(95)<2000'],
    },
};

export function setup() {
    const tokens = [];

    for (let i = 0; i < 1000; i++) {
        const loginRes = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({ username: `user_${i}`, password: 'test1234' }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (loginRes.status === 200) {
            tokens.push(loginRes.json('accessToken'));
        } else {
            console.warn(`로그인 실패 - user_${i}: ${loginRes.status}`);
        }
    }

    console.log(`준비된 토큰 수: ${tokens.length}`);
    return { tokens };
}

export default function (data) {
    // VU마다 다른 토큰 사용
    const token = data.tokens[__VU % data.tokens.length];

    const res = http.post(
        'http://localhost:8080/api/coupons/1/issue',
        null,
        {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }
    );

    // 201 → 발급 성공
    // 409 → 중복 or 재고 소진 (정상 비즈니스 로직)
    check(res, {
        'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
    });
}