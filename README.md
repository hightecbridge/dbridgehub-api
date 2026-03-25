# API

Spring Boot 3.2 + PostgreSQL + JWT 기반 REST API

## 멀티 서비스 구조

동일 JVM·동일 호스트에서 **공통 API 루트**와 **제품별 경로**를 나눈다.

| 구분 | 서블릿 컨텍스트 | 예시 |
|------|----------------|------|
| 공통 게이트웨이 | `/api` | — |
| 플랫폼 공통 | `/api/platform/**` | 헬스(Actuator): `GET /api/platform/health`, 서비스 목록: `GET /api/platform/services` (인증 불필요) |
| Academy | `/api/academy/**` | 어드민·학부모 API (경로는 이전과 동일) |

새 제품을 붙일 때는 `ServicePaths`에 접두사를 추가하고, `PlatformController`의 서비스 목록에 항목을 등록한다. 컨트롤러는 `@RequestMapping(ServicePaths.XXX + "/...")` 패턴으로 분리한다.

### 등록 서비스 조회

`GET /api/platform/services` — 응답 `data`에 노출 중인 서비스 `id`, `pathPrefix`, 이름 등이 배열로 온다.

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.3 |
| PostgreSQL | 16 |
| JWT (jjwt) | 0.12.5 |
| Lombok | latest |

---

## 실행 방법

### 1. Docker Compose (권장)
```bash
docker-compose up -d
# API 루트: http://localhost:8080/api  (학원 API: /api/academy/..., 헬스: /api/platform/health)
# DB:  localhost:5432
```

### 2. 로컬 직접 실행
```bash
# PostgreSQL 실행 후
export DB_USERNAME=hiacademy
export DB_PASSWORD=hiacademy123
export JWT_SECRET=HiAcademySecretKey2024VeryLongSecretKeyForJWT256bits

mvn spring-boot:run
```

---

## 인증 구조

| 사용자 | 엔드포인트 접두사 | 토큰 만료 |
|--------|-----------------|---------|
| 어드민 | `/api/academy/admin/**` | 24시간 |
| 학부모 | `/api/academy/parent/**` | 7일 |

모든 인증 요청에 헤더 추가:
```
Authorization: Bearer {token}
```

---

## API 엔드포인트 전체 목록

### 어드민 인증

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/academy/admin/auth/signup` | 어드민 회원가입 (학원 자동 생성) |
| POST | `/api/academy/admin/auth/login` | 어드민 로그인 |
| PUT  | `/api/academy/admin/auth/profile` | 회원정보 수정 (학원 로고 포함) |
| PATCH | `/api/academy/admin/auth/password` | 비밀번호 변경 |

### 반 관리

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/classrooms` | 반 목록 |
| POST   | `/api/academy/admin/classrooms` | 반 생성 |
| PUT    | `/api/academy/admin/classrooms/{id}` | 반 수정 |
| DELETE | `/api/academy/admin/classrooms/{id}` | 반 삭제 |

### 학부모/학생 관리

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/parents` | 학부모 목록 |
| POST   | `/api/academy/admin/parents` | 학부모 등록 (앱 로그인 계정 포함) |
| POST   | `/api/academy/admin/parents/{parentId}/students` | 학생 추가 |
| PATCH  | `/api/academy/admin/parents/students/{studentId}/fees` | 수납 상태 변경 |

### 출석 관리

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/classrooms/{cid}/attend` | 출석 시트 목록 |
| POST   | `/api/academy/admin/classrooms/{cid}/attend` | 출석 저장/수정 |

### 숙제 관리

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/classrooms/{cid}/homework` | 숙제 시트 목록 |
| POST   | `/api/academy/admin/classrooms/{cid}/homework` | 숙제 시트 저장 |
| PATCH  | `/api/academy/admin/classrooms/{cid}/homework/{sheetId}/students/{studentId}` | 완료 체크 |

### 공지사항

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/notices` | 공지 목록 |
| POST   | `/api/academy/admin/notices` | 공지 생성 |
| DELETE | `/api/academy/admin/notices/{id}` | 공지 삭제 |

### 일정

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/events` | 일정 목록 |
| POST   | `/api/academy/admin/events` | 일정 생성 |
| PUT    | `/api/academy/admin/events/{id}` | 일정 수정 |
| DELETE | `/api/academy/admin/events/{id}` | 일정 삭제 |

### 상담

| Method | URL | 설명 |
|--------|-----|------|
| GET    | `/api/academy/admin/consultations` | 상담 목록 |
| PATCH  | `/api/academy/admin/consultations/{id}/status?status=확정` | 상담 상태 변경 |

---

### 학부모 앱 API

| Method | URL | 설명 |
|--------|-----|------|
| POST   | `/api/academy/parent/auth/login` | 학부모 로그인 (전화번호+비밀번호) |
| GET    | `/api/academy/parent/home` | 홈 요약 (학원정보+자녀+공지+일정) |
| GET    | `/api/academy/parent/students/{id}/attend` | 자녀 출석 기록 |
| GET    | `/api/academy/parent/students/{id}/homework` | 자녀 숙제 기록 |
| GET    | `/api/academy/parent/consultations` | 상담 목록 |
| POST   | `/api/academy/parent/consultations` | 상담 신청 |
| GET    | `/api/academy/parent/academy/{academyId}` | 학원 정보 (비인증) |

---

## 요청/응답 예시

### 어드민 회원가입
```json
POST /api/academy/admin/auth/signup
{
  "email": "admin@example.com",
  "password": "1234",
  "name": "김원장",
  "phone": "010-1234-5678",
  "academyName": "데모 학원",
  "academyAddress": "서울시 강남구 테헤란로 123"
}
```

### 출석 저장
```json
POST /api/academy/admin/classrooms/1/attend
{
  "date": "2024-03-15",
  "records": [
    { "studentId": 1, "status": "출석" },
    { "studentId": 2, "status": "결석", "note": "병결" }
  ]
}
```

### 숙제 완료 체크
```json
PATCH /api/academy/admin/classrooms/1/homework/5/students/1
{
  "done": true,
  "comment": "꼼꼼히 잘 해왔음 👍"
}
```

### 학부모 로그인
```json
POST /api/academy/parent/auth/login
{
  "phone": "010-1234-5678",
  "password": "1234"
}
```

### 응답 공통 형식
```json
{
  "success": true,
  "message": "success",
  "data": { ... }
}
```

---

## 어드민 웹 연동 방법

`academy-admin` 프로젝트의 `authStore.ts`에서 실제 API 호출로 교체:

```typescript
// src/store/authStore.ts — login 함수
login: async (email, password) => {
  const res = await axios.post('/api/academy/admin/auth/login', { email, password })
  const { token, ...user } = res.data.data
  localStorage.setItem('token', token)
  set({ user, isLoading: false })
  return true
}
```

`vite.config.ts` 프록시 설정(로컬에서 `/api` 전체를 백엔드로 넘김):
```typescript
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true }
}
```

## 학부모 앱 연동 방법

학부모 앱 `App.js`의 store에서:
```javascript
const BASE = 'http://10.0.2.2:8080/api/academy'  // 에뮬레이터에서 localhost

login: async (phone, password) => {
  const res = await fetch(`${BASE}/parent/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone, password })
  })
  const { data } = await res.json()
  // data.token 저장 후 AsyncStorage에 보관
}
```

---

## DB 테이블 구조

```
academies           — 학원
users               — 어드민 계정
classrooms          — 반
parents             — 학부모 (학부모 앱 로그인 계정 포함)
students            — 학생
fee_records         — 수납 기록
attend_sheets       — 출석 시트 (날짜×반)
attend_records      — 출석 기록 (학생×시트)
homework_sheets     — 숙제 시트 (날짜×반)
homework_records    — 숙제 기록 (학생×시트)
notices             — 공지사항
notice_targets      — 공지 대상 반
calendar_events     — 일정
event_targets       — 일정 대상 반
consultations       — 상담
```
