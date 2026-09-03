-- 학생 등록 현황 진단 쿼리 (PostgreSQL)
-- 사용: DBeaver / psql 등에서 academy_id 를 본인 학원 ID 로 바꿔 실행

-- 0) 학원 목록
SELECT id, name, billing_plan_id, created_at
FROM academies
ORDER BY id;

-- 1) 학원별 학생 수 (API 가 보는 방식과 동일하게 비교)
--    :academy_id 를 실제 학원 ID 로 교체 (예: 1)
\set academy_id 1

-- 1-a) 신규 방식만 (students.academy_id 직접 매칭) — 현재 API 1차 조회
SELECT COUNT(*) AS cnt_new_api_path
FROM students s
WHERE s.academy_id = :academy_id;

-- 1-b) 레거시 방식만 (parent 경유) — academy_id 가 비어 있을 때만 API 가 2차로 사용
SELECT COUNT(*) AS cnt_legacy_api_path
FROM students s
JOIN parents p ON p.id = s.parent_id
WHERE p.academy_id = :academy_id;

-- 1-c) 통합 (권장) — academy_id 직접 또는 parent 경유
SELECT COUNT(*) AS cnt_combined
FROM students s
LEFT JOIN parents p ON p.id = s.parent_id
WHERE COALESCE(s.academy_id, p.academy_id) = :academy_id;

-- 2) 학생 등록 현황 상세 (검색·누락 원인 확인)
SELECT
    s.id,
    s.name,
    s.grade,
    s.status,
    s.parent_name,
    s.parent_phone,
    s.academy_id          AS student_academy_id,
    s.parent_id,
    p.academy_id          AS parent_academy_id,
    COALESCE(s.academy_id, p.academy_id) AS resolved_academy_id,
    s.classroom_id,
    c.name                AS classroom_name,
    s.created_at,
    CASE
        WHEN s.academy_id IS NOT NULL THEN 'direct'
        WHEN p.academy_id IS NOT NULL THEN 'via_parent'
        ELSE 'orphan'
    END AS linkage
FROM students s
LEFT JOIN parents p ON p.id = s.parent_id
LEFT JOIN classrooms c ON c.id = s.classroom_id
WHERE COALESCE(s.academy_id, p.academy_id) = :academy_id
   OR (:academy_id IS NULL)  -- 전체 조회 시 academy_id 조건 제거
ORDER BY s.created_at DESC NULLS LAST, s.id DESC;

-- 3) API 에서 누락될 수 있는 학생 (academy_id 없고 parent 만 있는 경우)
SELECT
    s.id, s.name, s.parent_id, p.academy_id AS parent_academy_id
FROM students s
JOIN parents p ON p.id = s.parent_id
WHERE s.academy_id IS NULL
  AND p.academy_id = :academy_id;

-- 4) 고아 학생 (학원 연결 없음 — 화면에 안 나옴)
SELECT s.id, s.name, s.parent_id, s.academy_id, s.classroom_id, s.created_at
FROM students s
LEFT JOIN parents p ON p.id = s.parent_id
WHERE s.academy_id IS NULL
  AND (s.parent_id IS NULL OR p.academy_id IS NULL);

-- 5) 반별 학생 수
SELECT
    c.id AS classroom_id,
    c.name AS classroom_name,
    COUNT(s.id) AS student_count
FROM classrooms c
LEFT JOIN students s ON s.classroom_id = c.id
WHERE c.academy_id = :academy_id
GROUP BY c.id, c.name
ORDER BY c.name;

-- 6) 반 미배정 학생
SELECT s.id, s.name, s.grade, s.academy_id, s.parent_id
FROM students s
LEFT JOIN parents p ON p.id = s.parent_id
WHERE COALESCE(s.academy_id, p.academy_id) = :academy_id
  AND s.classroom_id IS NULL;

-- 7) 이름·전화번호로 검색 (예: '김' / '010')
-- SELECT ... WHERE s.name ILIKE '%김%' OR s.parent_name ILIKE '%김%' ...

-- 8) academy_id 백필 (누락 학생이 3번에 있을 때만, 실행 전 백업 권장)
-- UPDATE students s
-- SET academy_id = p.academy_id
-- FROM parents p
-- WHERE s.parent_id = p.id
--   AND s.academy_id IS NULL
--   AND p.academy_id IS NOT NULL;
