package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.ExamRequest;
import com.hiacademy.api.dto.request.ExamScoresRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GradeService {
    private final ExamRepository examRepo;
    private final ExamScoreRepository scoreRepo;
    private final ClassRoomRepository classRepo;
    private final StudentRepository studentRepo;
    private final AcademyRepository academyRepo;

    public GradeService(
        ExamRepository examRepo,
        ExamScoreRepository scoreRepo,
        ClassRoomRepository classRepo,
        StudentRepository studentRepo,
        AcademyRepository academyRepo
    ) {
        this.examRepo = examRepo;
        this.scoreRepo = scoreRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.academyRepo = academyRepo;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listExams(AdminAccessService.Scope scope) {
        List<Exam> exams = examRepo.findAllByAcademy_IdOrderByExamDateDescIdDesc(scope.academyId());
        exams.forEach(this::initExamCollections);
        return exams.stream()
            .filter(e -> canAccessExam(scope, e))
            .map(e -> toExamSummary(e, targetStudents(scope, e, !scope.director())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listExams(AdminAccessService.Scope scope, Long classroomId) {
        requireClass(scope, classroomId);
        return listExams(scope).stream()
            .filter(e -> e.getClassrooms() != null && e.getClassrooms().stream().anyMatch(c -> classroomId.equals(c.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public ExamPageResponse listRegularExams(
        AdminAccessService.Scope scope,
        String grade,
        String from,
        String to,
        String status,
        String q,
        int page,
        int size,
        boolean dateDesc
    ) {
        Comparator<ExamResponse> byDate = Comparator.comparing(
            ExamResponse::getDate,
            Comparator.nullsLast(String::compareTo)
        ).thenComparing(ExamResponse::getId, Comparator.nullsLast(Long::compareTo));
        if (dateDesc) byDate = byDate.reversed();
        List<ExamResponse> filtered = listExamsByKind(scope, ExamKind.ALL).stream()
            .filter(e -> matchGrade(e, grade))
            .filter(e -> matchDateRange(e, from, to))
            .filter(e -> matchStatus(e, status))
            .filter(e -> matchQuery(e, q))
            .sorted(byDate)
            .toList();
        int sz = size < 1 ? 10 : Math.min(size, 50);
        int pg = Math.max(page, 0);
        int total = filtered.size();
        int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) sz);
        int fromIdx = Math.min(pg * sz, total);
        int toIdx = Math.min(fromIdx + sz, total);
        return ExamPageResponse.builder()
            .content(filtered.subList(fromIdx, toIdx))
            .totalElements(total)
            .totalPages(pages)
            .page(pg)
            .size(sz)
            .build();
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listDailyExams(AdminAccessService.Scope scope, Long classroomId, String from, String to) {
        return listExamsByKind(scope, ExamKind.CLASS).stream()
            .filter(e -> classroomId == null || (e.getClassrooms() != null && e.getClassrooms().stream().anyMatch(c -> classroomId.equals(c.getId()))))
            .filter(e -> matchDateRange(e, from, to))
            .toList();
    }

    @Transactional(readOnly = true)
    public DailyExamCalendarResponse dailyCalendar(AdminAccessService.Scope scope, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<ExamResponse> rows = listDailyExams(scope, null, start.toString(), end.toString());
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ExamResponse e : rows) {
            counts.merge(e.getDate(), 1, Integer::sum);
        }
        List<DailyExamCalendarResponse.DayCount> days = counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(en -> DailyExamCalendarResponse.DayCount.builder().date(en.getKey()).count(en.getValue()).build())
            .toList();
        return DailyExamCalendarResponse.builder().year(year).month(month).days(days).build();
    }

    public ExamResponse createExam(AdminAccessService.Scope scope, ExamRequest req, ExamKind forcedKind) {
        req.setKind(forcedKind.name());
        if (forcedKind == ExamKind.CLASS) {
            autoDailyTitle(req);
        }
        return createExam(scope, req);
    }

    public ExamResponse getExam(AdminAccessService.Scope scope, Long examId, ExamKind expected) {
        ExamResponse exam = getExam(scope, examId);
        if (expected != null && !expected.name().equals(exam.getKind())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다.");
        }
        return exam;
    }

    public ExamResponse updateExam(AdminAccessService.Scope scope, Long examId, ExamRequest req, ExamKind expected) {
        req.setKind(expected.name());
        Exam exam = loadExam(scope, examId);
        requireKind(exam, expected);
        return updateExam(scope, examId, req);
    }

    public void deleteExam(AdminAccessService.Scope scope, Long examId, ExamKind expected) {
        requireKind(loadExam(scope, examId), expected);
        deleteExam(scope, examId);
    }

    public ExamResponse saveScores(AdminAccessService.Scope scope, Long examId, ExamScoresRequest req, ExamKind expected) {
        requireKind(loadExam(scope, examId), expected);
        return saveScores(scope, examId, req);
    }

    public ExamResponse closeExam(AdminAccessService.Scope scope, Long examId, ExamKind expected) {
        Exam exam = loadExam(scope, examId);
        requireKind(exam, expected);
        exam.setClosed(true);
        initExamCollections(exam);
        return toExamSummary(exam, targetStudents(scope, exam, !scope.director()));
    }

    public ExamResponse createExam(AdminAccessService.Scope scope, ExamRequest req) {
        ExamKind kind = parseKind(req.getKind());
        Exam exam = Exam.builder()
            .title(req.getTitle().trim())
            .examDate(parseDate(req.getDate()))
            .subject(blankToNull(req.getSubject()))
            .maxScore(req.getMaxScore() == null ? 100 : req.getMaxScore())
            .kind(kind)
            .targetGrade(kind == ExamKind.ALL ? blankToNull(req.getTargetGrade()) : null)
            .build();
        applyTargets(scope, exam, req.getClassroomIds(), kind);
        if (exam.getSubject() == null && exam.getClassroom() != null) {
            exam.setSubject(exam.getClassroom().getSubject());
        }
        applySections(exam, req.getSections());
        Exam saved = examRepo.save(exam);
        initExamCollections(saved);
        List<Student> roster = targetStudents(scope, saved, true);
        return toExamDetail(saved, roster, Map.of());
    }

    public ExamResponse createExam(AdminAccessService.Scope scope, Long classroomId, ExamRequest req) {
        List<Long> ids = new ArrayList<>(req.getClassroomIds() == null ? List.of() : req.getClassroomIds());
        if (classroomId != null && !ids.contains(classroomId)) ids.add(0, classroomId);
        req.setClassroomIds(ids);
        return createExam(scope, req);
    }

    public ExamResponse updateExam(AdminAccessService.Scope scope, Long examId, ExamRequest req) {
        Exam exam = loadExam(scope, examId);
        exam.setTitle(req.getTitle().trim());
        exam.setExamDate(parseDate(req.getDate()));
        if (req.getSubject() != null && !req.getSubject().isBlank()) {
            exam.setSubject(req.getSubject().trim());
        }
        if (req.getMaxScore() != null) {
            exam.setMaxScore(req.getMaxScore());
        }
        ExamKind kind = req.getKind() == null || req.getKind().isBlank()
            ? (exam.getKind() == null ? ExamKind.CLASS : exam.getKind())
            : parseKind(req.getKind());
        exam.setKind(kind);
        exam.setTargetGrade(kind == ExamKind.ALL ? blankToNull(req.getTargetGrade()) : null);
        initExamCollections(exam);
        exam.getScores().forEach(s -> Hibernate.initialize(s.getSectionScores()));
        if (kind == ExamKind.ALL || req.getClassroomIds() != null) {
            applyTargets(scope, exam, req.getClassroomIds(), kind);
        }
        applySections(exam, req.getSections());
        return toExamDetail(exam, targetStudents(scope, exam, true), ranks(exam.getScores()));
    }

    public ExamResponse updateExam(AdminAccessService.Scope scope, Long classroomId, Long examId, ExamRequest req) {
        requireClass(scope, classroomId);
        return updateExam(scope, examId, req);
    }

    public void deleteExam(AdminAccessService.Scope scope, Long examId) {
        examRepo.delete(loadExam(scope, examId));
    }

    public void deleteExam(AdminAccessService.Scope scope, Long classroomId, Long examId) {
        requireClass(scope, classroomId);
        deleteExam(scope, examId);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(AdminAccessService.Scope scope, Long examId) {
        Exam exam = examRepo.findDetailByIdAndAcademyId(examId, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다."));
        initExamCollections(exam);
        if (!canAccessExam(scope, exam)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        exam.getScores().forEach(s -> {
            Hibernate.initialize(s.getSectionScores());
            if (s.getStudent() != null) Hibernate.initialize(s.getStudent().getClassroom());
        });
        return toExamDetail(exam, targetStudents(scope, exam, true), ranks(exam.getScores()));
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(AdminAccessService.Scope scope, Long classroomId, Long examId) {
        requireClass(scope, classroomId);
        return getExam(scope, examId);
    }

    public ExamResponse saveScores(AdminAccessService.Scope scope, Long examId, ExamScoresRequest req) {
        Exam exam = loadExam(scope, examId);
        initExamCollections(exam);
        exam.getScores().forEach(s -> Hibernate.initialize(s.getSectionScores()));
        Map<Long, ExamScore> existing = exam.getScores().stream()
            .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));
        Set<Long> allowedIds = targetStudents(scope, exam, true).stream().map(Student::getId).collect(Collectors.toSet());
        Map<Long, ExamSection> sectionMap = exam.getSections().stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(ExamSection::getId, s -> s, (a, b) -> a));

        for (ExamScoresRequest.Item item : req.getRecords() == null ? List.<ExamScoresRequest.Item>of() : req.getRecords()) {
            Student stu = studentRepo.findById(item.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
            boolean allowed = allowedIds.contains(stu.getId());
            if (!allowed && !existing.containsKey(stu.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "대상 반 학생만 점수를 입력할 수 있습니다.");
            }
            if (allowed) {
                scope.requireStudent(stu);
            }
            validateScore(item.getScore(), exam.getMaxScore());
            Double score = deriveTotalScore(item.getScore(), item.getSectionScores(), exam);
            validateScore(score, exam.getMaxScore());
            ExamScore row = existing.get(stu.getId());
            if (row == null) {
                row = ExamScore.builder().exam(exam).student(stu).build();
                exam.getScores().add(row);
                existing.put(stu.getId(), row);
            }
            row.setScore(score);
            row.setComment(blankToNull(item.getComment()));
            applySectionScores(row, item.getSectionScores(), sectionMap);
        }
        applyInputStatus(exam, req);
        Exam saved = examRepo.save(exam);
        initExamCollections(saved);
        saved.getScores().forEach(s -> {
            Hibernate.initialize(s.getStudent());
            if (s.getStudent() != null) Hibernate.initialize(s.getStudent().getClassroom());
            Hibernate.initialize(s.getSectionScores());
        });
        return toExamDetail(saved, targetStudents(scope, saved, true), ranks(saved.getScores()));
    }

    public ExamResponse saveScores(AdminAccessService.Scope scope, Long classroomId, Long examId, ExamScoresRequest req) {
        requireClass(scope, classroomId);
        return saveScores(scope, examId, req);
    }

    @Transactional(readOnly = true)
    public ClassGradeStatsResponse classStats(AdminAccessService.Scope scope, Long classroomId) {
        requireClass(scope, classroomId);
        ClassRoom cls = classRepo.findByIdAndAcademy_Id(classroomId, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Exam> exams = examRepo.findAllByClassroom_IdOrderByExamDateDescIdDesc(classroomId);
        exams.forEach(e -> {
            Hibernate.initialize(e.getScores());
            e.getScores().forEach(s -> {
                Hibernate.initialize(s.getStudent());
                Hibernate.initialize(s.getSectionScores());
            });
        });
        List<Student> students = classStudents(classroomId);

        List<ClassGradeStatsResponse.ExamTrendPoint> trend = new ArrayList<>();
        Map<Long, List<Double>> byStudent = new HashMap<>();
        Map<Long, Double> latestScore = new HashMap<>();
        Map<Long, Double> latestDelta = new HashMap<>();

        List<Exam> chrono = new ArrayList<>(exams);
        chrono.sort(Comparator.comparing(Exam::getExamDate).thenComparing(Exam::getId));

        for (Exam exam : chrono) {
            List<Double> taken = exam.getScores().stream()
                .map(GradeService::effectiveScore).filter(Objects::nonNull).toList();
            Stats st = statsOf(taken);
            trend.add(ClassGradeStatsResponse.ExamTrendPoint.builder()
                .examId(exam.getId()).title(exam.getTitle())
                .date(exam.getExamDate().toString())
                .average(st.avg).participantCount(taken.size())
                .high(st.high).low(st.low)
                .build());
            Map<Long, Double> thisScores = new HashMap<>();
            for (ExamScore s : exam.getScores()) {
                Double sc = effectiveScore(s);
                if (sc == null) continue;
                byStudent.computeIfAbsent(s.getStudent().getId(), k -> new ArrayList<>()).add(sc);
                thisScores.put(s.getStudent().getId(), sc);
            }
            for (Map.Entry<Long, Double> e : thisScores.entrySet()) {
                Double prev = latestScore.get(e.getKey());
                latestDelta.put(e.getKey(), prev == null ? null : round1(e.getValue() - prev));
                latestScore.put(e.getKey(), e.getValue());
            }
        }

        List<ClassGradeStatsResponse.StudentRankRow> ranking = new ArrayList<>();
        for (Student stu : students) {
            List<Double> sc = byStudent.getOrDefault(stu.getId(), List.of());
            Double avg = sc.isEmpty() ? null : round1(sc.stream().mapToDouble(d -> d).average().orElse(0));
            ranking.add(ClassGradeStatsResponse.StudentRankRow.builder()
                .studentId(stu.getId()).studentName(stu.getName())
                .examCount(sc.size()).average(avg)
                .latestScore(latestScore.get(stu.getId()))
                .latestDelta(latestDelta.get(stu.getId()))
                .build());
        }
        ranking.sort(Comparator
            .comparing((ClassGradeStatsResponse.StudentRankRow r) -> r.getAverage() == null ? -1 : 0)
            .thenComparing((ClassGradeStatsResponse.StudentRankRow r) -> r.getAverage() == null ? 0.0 : r.getAverage(), Comparator.reverseOrder())
            .thenComparing(ClassGradeStatsResponse.StudentRankRow::getStudentName, Comparator.nullsLast(String::compareTo)));
        int i = 0, prevRank = 0;
        Double prevAvg = null;
        for (ClassGradeStatsResponse.StudentRankRow row : ranking) {
            i++;
            if (row.getAverage() == null) {
                row.setRank(null);
                continue;
            }
            if (prevAvg != null && Double.compare(prevAvg, row.getAverage()) == 0) {
                row.setRank(prevRank);
            } else {
                row.setRank(i);
                prevRank = i;
                prevAvg = row.getAverage();
            }
        }

        List<Double> allAvgs = ranking.stream().map(ClassGradeStatsResponse.StudentRankRow::getAverage)
            .filter(Objects::nonNull).toList();
        Double overall = allAvgs.isEmpty() ? null : round1(allAvgs.stream().mapToDouble(d -> d).average().orElse(0));

        // 시험 추이는 최신순으로 보여 주기
        Collections.reverse(trend);

        return ClassGradeStatsResponse.builder()
            .classroomId(cls.getId()).classroomName(cls.getName())
            .examCount(exams.size()).overallAverage(overall)
            .examTrend(trend).ranking(ranking)
            .build();
    }

    @Transactional(readOnly = true)
    public ExamStatsResponse examStats(AdminAccessService.Scope scope, Long examId) {
        Exam exam = loadExam(scope, examId);
        initExamCollections(exam);
        exam.getScores().forEach(s -> {
            Hibernate.initialize(s.getStudent());
            if (s.getStudent() != null) Hibernate.initialize(s.getStudent().getClassroom());
            Hibernate.initialize(s.getSectionScores());
        });
        List<ClassRoom> classes = examClassrooms(exam);
        List<Student> allRoster = targetStudents(scope, exam, false);
        if (classes.isEmpty()) {
            classes = allRoster.stream()
                .map(Student::getClassroom)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ClassRoom::getId, c -> c, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(ClassRoom::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        }
        Map<Long, ExamScore> byStudent = exam.getScores() == null ? Map.of()
            : exam.getScores().stream().collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s, (a, b) -> a));
        Map<Long, Integer> rankMap = ranks(exam.getScores() == null ? List.of() : exam.getScores());
        List<Double> taken = exam.getScores() == null ? List.of()
            : exam.getScores().stream().map(GradeService::effectiveScore).filter(Objects::nonNull).toList();
        Stats overall = statsOf(taken);

        List<ExamStatsResponse.ClassBreakdown> breakdown = new ArrayList<>();
        for (ClassRoom cls : classes) {
            List<Student> inClass = allRoster.stream()
                .filter(s -> s.getClassroom() != null && cls.getId().equals(s.getClassroom().getId()))
                .toList();
            List<Double> classTaken = inClass.stream()
                .map(s -> byStudent.get(s.getId()))
                .map(GradeService::effectiveScore)
                .filter(Objects::nonNull)
                .toList();
            Stats st = statsOf(classTaken);
            breakdown.add(ExamStatsResponse.ClassBreakdown.builder()
                .classroomId(cls.getId()).classroomName(cls.getName())
                .participantCount(st.count).totalStudents(inClass.size())
                .average(st.avg).high(st.high).low(st.low)
                .build());
        }

        int rankedCount = overall.count;
        List<ExamStatsResponse.StudentRankRow> ranking = new ArrayList<>();
        for (Student stu : allRoster) {
            ExamScore row = byStudent.get(stu.getId());
            Double score = effectiveScore(row);
            ranking.add(ExamStatsResponse.StudentRankRow.builder()
                .studentId(stu.getId())
                .studentName(stu.getName())
                .classroomName(stu.getClassroom() != null ? stu.getClassroom().getName() : null)
                .score(score)
                .rank(score == null ? null : rankMap.get(stu.getId()))
                .rankedCount(rankedCount)
                .build());
        }
        ranking.sort(Comparator
            .comparing((ExamStatsResponse.StudentRankRow r) -> r.getScore() == null ? 1 : 0)
            .thenComparing((ExamStatsResponse.StudentRankRow r) -> r.getScore() == null ? 0.0 : r.getScore(), Comparator.reverseOrder())
            .thenComparing(ExamStatsResponse.StudentRankRow::getStudentName, Comparator.nullsLast(String::compareTo)));

        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        return ExamStatsResponse.builder()
            .examId(exam.getId()).title(exam.getTitle())
            .date(exam.getExamDate().toString())
            .subject(exam.getSubject())
            .kind(kind.name())
            .kindLabel(kind.label())
            .maxScore(exam.getMaxScore())
            .participantCount(overall.count)
            .totalStudents(allRoster.size())
            .average(overall.avg).high(overall.high).low(overall.low).median(overall.median)
            .classrooms(breakdown)
            .distribution(distributionOf(taken, exam.getMaxScore()))
            .ranking(ranking)
            .build();
    }

    @Transactional(readOnly = true)
    public GradeHistoryResponse studentHistory(AdminAccessService.Scope scope, Long studentId) {
        Student stu = studentRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
        if (stu.resolveAcademy() == null || !stu.resolveAcademy().getId().equals(scope.academyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        scope.requireStudent(stu);
        return buildHistory(stu);
    }

    @Transactional(readOnly = true)
    public GradeHistoryResponse parentHistory(Student child) {
        return parentHistory(child, true, true);
    }

    @Transactional(readOnly = true)
    public GradeHistoryResponse parentHistory(Student child, boolean showRegular, boolean showDaily) {
        GradeHistoryResponse history = buildHistory(child);
        if (history.getItems() != null) {
            history.getItems().forEach(it -> {
                it.setRank(null);
                it.setRankedCount(null);
            });
        }
        return restrictKinds(history, showRegular, showDaily);
    }

    public static GradeHistoryResponse restrictKinds(GradeHistoryResponse history, boolean showRegular, boolean showDaily) {
        if (history == null) return null;
        if (showRegular && showDaily) return history;
        List<GradeItemResponse> items = history.getItems() == null ? List.of() : history.getItems();
        List<GradeItemResponse> kept = items.stream()
            .filter(it -> "ALL".equals(it.getKind()) ? showRegular : showDaily)
            .toList();
        List<GradeItemResponse> chrono = new ArrayList<>(kept);
        Collections.reverse(chrono);
        Double prev = null;
        for (GradeItemResponse it : chrono) {
            Double score = it.getScore();
            it.setDelta(score != null && prev != null ? Math.round((score - prev) * 10.0) / 10.0 : null);
            if (score != null) prev = score;
        }
        List<Double> taken = kept.stream().map(GradeItemResponse::getScore).filter(Objects::nonNull).toList();
        Double avg = taken.isEmpty() ? null : Math.round(taken.stream().mapToDouble(d -> d).average().orElse(0) * 10.0) / 10.0;
        Double latestDelta = kept.stream().map(GradeItemResponse::getDelta).filter(Objects::nonNull).findFirst().orElse(null);
        history.setItems(kept);
        history.setExamCount(taken.size());
        history.setAverage(avg);
        history.setLatestDelta(latestDelta);
        return history;
    }

    private GradeHistoryResponse buildHistory(Student stu) {
        List<ExamScore> mine = scoreRepo.findAllByStudentIdWithExam(stu.getId());
        mine.forEach(s -> {
            Hibernate.initialize(s.getExam().getSections());
            Hibernate.initialize(s.getSectionScores());
            s.getSectionScores().forEach(ss -> Hibernate.initialize(ss.getSection()));
        });
        List<Long> examIds = mine.stream().map(s -> s.getExam().getId()).toList();
        Map<Long, List<ExamScore>> byExam = examIds.isEmpty()
            ? Map.of()
            : scoreRepo.findAllByExam_IdIn(examIds).stream().collect(Collectors.groupingBy(s -> s.getExam().getId()));

        List<ExamScore> chrono = new ArrayList<>(mine);
        chrono.sort(Comparator.comparing((ExamScore s) -> s.getExam().getExamDate()).thenComparing(s -> s.getExam().getId()));

        Double prevScore = null;
        List<GradeItemResponse> chronoItems = new ArrayList<>();
        for (ExamScore row : chrono) {
            Exam exam = row.getExam();
            List<ExamScore> all = byExam.getOrDefault(exam.getId(), List.of());
            all.forEach(s -> Hibernate.initialize(s.getSectionScores()));
            Map<Long, Integer> rankMap = ranks(all);
            List<Double> taken = all.stream().map(GradeService::effectiveScore).filter(Objects::nonNull).toList();
            Stats st = statsOf(taken);
            Double mineScore = effectiveScore(row);
            Double delta = (mineScore != null && prevScore != null) ? round1(mineScore - prevScore) : null;
            Double vsAvg = (mineScore != null && st.avg != null) ? round1(mineScore - st.avg) : null;
            ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
            String className = stu.getClassroom() != null ? stu.getClassroom().getName()
                : (exam.getClassroom() != null ? exam.getClassroom().getName() : null);
            chronoItems.add(GradeItemResponse.builder()
                .examId(exam.getId()).title(exam.getTitle())
                .date(exam.getExamDate().toString())
                .subject(exam.getSubject())
                .kind(kind.name())
                .kindLabel(kind.label())
                .classroomName(className)
                .score(mineScore).maxScore(exam.getMaxScore())
                .classAvg(st.avg)
                .rank(mineScore == null ? null : rankMap.get(stu.getId()))
                .rankedCount(taken.size())
                .delta(delta)
                .vsClassAvg(vsAvg)
                .comment(row.getComment())
                .sectionScores(toSectionScoreResponses(exam, row))
                .build());
            if (mineScore != null) prevScore = mineScore;
        }

        List<GradeItemResponse> newestFirst = new ArrayList<>(chronoItems);
        Collections.reverse(newestFirst);

        List<Double> takenMine = chrono.stream().map(GradeService::effectiveScore).filter(Objects::nonNull).toList();
        Double avg = takenMine.isEmpty() ? null : round1(takenMine.stream().mapToDouble(d -> d).average().orElse(0));
        Double latestDelta = newestFirst.stream().map(GradeItemResponse::getDelta).filter(Objects::nonNull).findFirst().orElse(null);

        return GradeHistoryResponse.builder()
            .studentId(stu.getId()).studentName(stu.getName())
            .classroomName(stu.getClassroom() != null ? stu.getClassroom().getName() : null)
            .examCount(takenMine.size()).average(avg).latestDelta(latestDelta)
            .items(newestFirst)
            .build();
    }

    private ClassRoom requireClass(AdminAccessService.Scope scope, Long classroomId) {
        scope.requireClassroom(classroomId);
        ClassRoom cls = classRepo.findByIdAndAcademy_Id(classroomId, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "반을 찾을 수 없습니다."));
        return cls;
    }

    private Exam loadExam(AdminAccessService.Scope scope, Long examId) {
        Exam exam = examRepo.findByIdAndAcademy_Id(examId, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다."));
        Hibernate.initialize(exam.getClassrooms());
        if (!canAccessExam(scope, exam)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반 시험만 관리할 수 있습니다.");
        }
        return exam;
    }

    private Exam loadExam(Long classroomId, Long examId) {
        Exam exam = examRepo.findByIdAndClassroom_Id(examId, classroomId)
            .orElseGet(() -> examRepo.findById(examId)
                .filter(e -> examClassrooms(e).stream().anyMatch(c -> c.getId().equals(classroomId)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다.")));
        return exam;
    }

    private void initExamCollections(Exam exam) {
        Hibernate.initialize(exam.getScores());
        Hibernate.initialize(exam.getSections());
        Hibernate.initialize(exam.getClassrooms());
        Hibernate.initialize(exam.getInputStatuses());
        if (exam.getClassroom() != null) Hibernate.initialize(exam.getClassroom());
        if (exam.getScores() != null) {
            exam.getScores().forEach(s -> {
                Hibernate.initialize(s.getSectionScores());
                if (s.getSectionScores() != null) {
                    s.getSectionScores().forEach(ss -> Hibernate.initialize(ss.getSection()));
                }
            });
        }
        if (exam.getInputStatuses() != null) {
            exam.getInputStatuses().forEach(s -> {
                if (s.getClassroom() != null) Hibernate.initialize(s.getClassroom());
            });
        }
    }

    private List<ClassRoom> examClassrooms(Exam exam) {
        Hibernate.initialize(exam.getClassrooms());
        if (exam.getClassrooms() != null && !exam.getClassrooms().isEmpty()) {
            return exam.getClassrooms().stream()
                .sorted(Comparator.comparing(ClassRoom::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        }
        if (exam.getClassroom() != null) return List.of(exam.getClassroom());
        return List.of();
    }

    private boolean canAccessExam(AdminAccessService.Scope scope, Exam exam) {
        if (scope.director()) return true;
        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        if (kind == ExamKind.ALL) return true;
        return examClassrooms(exam).stream().anyMatch(c -> scope.allowsClassroom(c.getId()));
    }

    private void applyTargets(AdminAccessService.Scope scope, Exam exam, List<Long> classroomIds, ExamKind kind) {
        Academy academy = academyRepo.findById(scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학원을 찾을 수 없습니다."));
        exam.setAcademy(academy);
        if (classroomIds == null || classroomIds.isEmpty()) {
            if (kind == ExamKind.ALL) {
                if (exam.getClassrooms() == null) {
                    exam.setClassrooms(new ArrayList<>());
                }
                exam.getClassrooms().clear();
                exam.setClassroom(null);
                return;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 반을 선택하세요.");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>(classroomIds);
        List<ClassRoom> resolved = new ArrayList<>();
        for (Long id : unique) {
            resolved.add(requireClass(scope, id));
        }
        if (exam.getClassrooms() == null) {
            exam.setClassrooms(new ArrayList<>());
        }
        exam.getClassrooms().clear();
        exam.getClassrooms().addAll(resolved);
        exam.setClassroom(resolved.get(0));
    }

    /** editable=true 이면 교사는 담당 반 학생만 명단에 포함 */
    private List<Student> targetStudents(AdminAccessService.Scope scope, Exam exam, boolean editableOnly) {
        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        String grade = kind == ExamKind.ALL ? blankToNull(exam.getTargetGrade()) : null;
        List<ClassRoom> bound = examClassrooms(exam);
        List<Student> base;
        if (!bound.isEmpty()) {
            List<Long> ids = bound.stream()
                .filter(c -> !editableOnly || scope.allowsClassroom(c.getId()))
                .map(ClassRoom::getId)
                .toList();
            if (ids.isEmpty()) return List.of();
            base = classStudents(ids);
        } else if (kind == ExamKind.ALL) {
            base = studentRepo.findAllByAcademyIdWithClassroom(scope.academyId()).stream()
                .filter(s -> s.getStatus() != StudentStatus.퇴원)
                .filter(s -> !editableOnly || scope.allowsStudent(s))
                .peek(s -> {
                    if (s.getClassroom() != null) Hibernate.initialize(s.getClassroom());
                })
                .sorted(Comparator
                    .comparing((Student s) -> s.getClassroom() == null ? "" : s.getClassroom().getName(), Comparator.nullsLast(String::compareTo))
                    .thenComparing(Student::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        } else {
            return List.of();
        }
        if (grade == null) return base;
        return base.stream().filter(s -> grade.equals(s.getGrade())).toList();
    }

    private List<Student> classStudents(Long classroomId) {
        return classStudents(List.of(classroomId));
    }

    private List<Student> classStudents(Collection<Long> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) return List.of();
        return studentRepo.findAllByClassroom_IdIn(classroomIds).stream()
            .filter(s -> s.getStatus() != StudentStatus.퇴원)
            .peek(s -> Hibernate.initialize(s.getClassroom()))
            .sorted(Comparator
                .comparing((Student s) -> s.getClassroom() == null ? "" : s.getClassroom().getName(), Comparator.nullsLast(String::compareTo))
                .thenComparing(Student::getName, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    private static void validateScore(Double score, int maxScore) {
        if (score == null) return;
        if (score < 0 || score > maxScore) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "점수는 0부터 " + maxScore + "점까지 입력할 수 있습니다.");
        }
    }

    private static void validatePercent(Double percent) {
        if (percent == null) return;
        if (percent < 0 || percent > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "백분율 점수는 0부터 100까지 입력할 수 있습니다.");
        }
    }

    private static Double resolvePercent(Double score, Double percent, int maxScore) {
        if (percent != null) return round1(percent);
        if (score == null || maxScore <= 0) return null;
        return round1(score * 100.0 / maxScore);
    }

    private void applySections(Exam exam, List<ExamRequest.Section> incoming) {
        List<ExamRequest.Section> items = incoming == null ? List.of() : incoming.stream()
            .filter(item -> blankToNull(item.getName()) != null)
            .toList();
        if (items.size() > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과목은 최대 12개까지 등록할 수 있습니다.");
        }
        Set<Long> keepIds = items.stream()
            .map(ExamRequest.Section::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (exam.getScores() != null) {
            for (ExamScore score : exam.getScores()) {
                if (score.getSectionScores() == null) continue;
                Hibernate.initialize(score.getSectionScores());
                score.getSectionScores().removeIf(ss ->
                    ss.getSection() != null && ss.getSection().getId() != null && !keepIds.contains(ss.getSection().getId()));
            }
        }
        if (exam.getSections() == null) {
            exam.setSections(new ArrayList<>());
        }
        Map<Long, ExamSection> existing = exam.getSections().stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(ExamSection::getId, s -> s, (a, b) -> a));
        exam.getSections().removeIf(s -> s.getId() != null && !keepIds.contains(s.getId()));

        int order = 0;
        for (ExamRequest.Section item : items) {
            String name = blankToNull(item.getName());
            int max = item.getMaxScore() == null ? 100 : item.getMaxScore();
            if (max < 1 || max > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과목 만점은 1~1000점입니다.");
            }
            int weight = item.getWeight() == null ? 0 : item.getWeight();
            if (weight < 0 || weight > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과목 배점은 0~1000점입니다.");
            }
            ExamSection row = item.getId() != null ? existing.get(item.getId()) : null;
            if (row == null) {
                row = ExamSection.builder().exam(exam).build();
                exam.getSections().add(row);
            }
            row.setName(name);
            row.setMaxScore(max);
            row.setWeight(weight);
            row.setSortOrder(order++);
        }
    }

    private void applySectionScores(
        ExamScore row,
        List<ExamScoresRequest.Item.SectionScore> incoming,
        Map<Long, ExamSection> sectionMap
    ) {
        if (incoming == null) return;
        if (row.getSectionScores() == null) {
            row.setSectionScores(new ArrayList<>());
        }
        Hibernate.initialize(row.getSectionScores());
        Map<Long, ExamScoreSection> existing = row.getSectionScores().stream()
            .filter(ss -> ss.getSection() != null && ss.getSection().getId() != null)
            .collect(Collectors.toMap(ss -> ss.getSection().getId(), ss -> ss, (a, b) -> a));
        Set<Long> seen = new HashSet<>();
        for (ExamScoresRequest.Item.SectionScore item : incoming) {
            ExamSection section = sectionMap.get(item.getSectionId());
            if (section == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 과목입니다.");
            }
            seen.add(section.getId());
            validateScore(item.getScore(), section.getMaxScore());
            validatePercent(item.getPercent());
            Double percent = resolvePercent(item.getScore(), item.getPercent(), section.getMaxScore());
            ExamScoreSection ss = existing.get(section.getId());
            if (item.getScore() == null && percent == null) {
                if (ss != null) {
                    row.getSectionScores().remove(ss);
                }
                continue;
            }
            if (ss == null) {
                ss = ExamScoreSection.builder().examScore(row).section(section).build();
                row.getSectionScores().add(ss);
            }
            ss.setScore(item.getScore());
            ss.setPercent(percent);
        }
        row.getSectionScores().removeIf(ss ->
            ss.getSection() == null || ss.getSection().getId() == null || !seen.contains(ss.getSection().getId()));
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시험 날짜 형식이 올바르지 않습니다.");
        }
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

    private static Double effectiveScore(ExamScore row) {
        if (row == null) return null;
        if (row.getScore() != null) return row.getScore();
        return weightedSectionTotal(row);
    }

    private static int sectionWeight(ExamSection section) {
        if (section == null) return 0;
        return section.getWeight() > 0 ? section.getWeight() : section.getMaxScore();
    }

    private static Double weightedSectionTotal(ExamScore row) {
        if (row.getSectionScores() == null || row.getSectionScores().isEmpty()) return null;
        double sum = 0;
        boolean any = false;
        for (ExamScoreSection ss : row.getSectionScores()) {
            if (ss.getScore() == null) continue;
            ExamSection sec = ss.getSection();
            int max = sec == null ? 0 : sec.getMaxScore();
            int weight = sectionWeight(sec);
            if (max > 0) sum += ss.getScore() * weight / (double) max;
            else sum += ss.getScore();
            any = true;
        }
        return any ? round1(sum) : null;
    }

    private static Double deriveTotalScore(Double total, List<ExamScoresRequest.Item.SectionScore> sections, Exam exam) {
        if (total != null) return total;
        if (sections == null || exam == null) return null;
        Map<Long, ExamSection> byId = exam.getSections() == null ? Map.of()
            : exam.getSections().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(ExamSection::getId, s -> s, (a, b) -> a));
        double sum = 0;
        boolean any = false;
        for (ExamScoresRequest.Item.SectionScore s : sections) {
            if (s.getScore() == null) continue;
            ExamSection sec = byId.get(s.getSectionId());
            int max = sec == null ? 0 : sec.getMaxScore();
            int weight = sectionWeight(sec);
            if (max > 0) sum += s.getScore() * weight / (double) max;
            else sum += s.getScore();
            any = true;
        }
        if (!any) return null;
        int cap = exam.getMaxScore() > 0 ? exam.getMaxScore() : Integer.MAX_VALUE;
        return round1(Math.min(sum, cap));
    }

    private static Map<Long, Integer> ranks(List<ExamScore> scores) {
        List<ExamScore> taken = scores.stream()
            .filter(s -> effectiveScore(s) != null)
            .sorted(Comparator.comparing((ExamScore s) -> effectiveScore(s)).reversed()
                .thenComparing(s -> s.getStudent().getId()))
            .toList();
        Map<Long, Integer> map = new LinkedHashMap<>();
        int i = 0, prevRank = 0;
        Double prev = null;
        for (ExamScore s : taken) {
            i++;
            Double sc = effectiveScore(s);
            if (prev != null && Double.compare(prev, sc) == 0) {
                map.put(s.getStudent().getId(), prevRank);
            } else {
                map.put(s.getStudent().getId(), i);
                prevRank = i;
                prev = sc;
            }
        }
        return map;
    }

    private record Stats(Double avg, Double high, Double low, Double median, int count) {}

    private static Stats statsOf(List<Double> taken) {
        if (taken == null || taken.isEmpty()) return new Stats(null, null, null, null, 0);
        List<Double> sorted = taken.stream().sorted().toList();
        double sum = 0;
        for (Double d : sorted) sum += d;
        double avg = round1(sum / sorted.size());
        double high = sorted.get(sorted.size() - 1);
        double low = sorted.get(0);
        double median;
        int n = sorted.size();
        if (n % 2 == 1) median = sorted.get(n / 2);
        else median = round1((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);
        return new Stats(avg, high, low, median, n);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private ExamResponse toExamSummary(Exam exam, List<Student> roster) {
        int classSize = roster == null ? 0 : roster.size();
        List<Double> taken = exam.getScores() == null ? List.of()
            : exam.getScores().stream().map(GradeService::effectiveScore).filter(Objects::nonNull).toList();
        int recorded = exam.getScores() == null ? 0 : exam.getScores().size();
        Stats st = statsOf(taken);
        List<ExamClassroomResponse> rooms = examClassrooms(exam).stream()
            .map(c -> ExamClassroomResponse.builder().id(c.getId()).name(c.getName()).build())
            .toList();
        ClassRoom first = rooms.isEmpty() ? exam.getClassroom() : examClassrooms(exam).get(0);
        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        ExamResponse res = ExamResponse.builder()
            .id(exam.getId())
            .classroomId(first != null ? first.getId() : null)
            .classroomName(first != null ? first.getName() : null)
            .classrooms(rooms)
            .kind(kind.name())
            .kindLabel(kind.label())
            .targetGrade(exam.getTargetGrade())
            .targetLabel(targetLabel(exam, rooms))
            .title(exam.getTitle())
            .date(exam.getExamDate().toString())
            .subject(exam.getSubject())
            .maxScore(exam.getMaxScore())
            .participantCount(st.count)
            .absentCount(Math.max(0, recorded - st.count))
            .totalStudents(classSize)
            .average(st.avg).high(st.high).low(st.low).median(st.median)
            .sections(toSectionResponses(exam))
            .createdAt(exam.getCreatedAt())
            .build();
        attachProgress(res, exam, roster == null ? List.of() : roster);
        attachLifecycle(res, exam);
        return res;
    }

    private ExamResponse toExamDetail(Exam exam, List<Student> classStudents, Map<Long, Integer> rankMap) {
        Map<Long, ExamScore> byId = exam.getScores() == null ? Map.of()
            : exam.getScores().stream().collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s, (a, b) -> a));
        List<Student> roster = new ArrayList<>(classStudents);
        for (ExamScore s : exam.getScores() == null ? List.<ExamScore>of() : exam.getScores()) {
            if (roster.stream().noneMatch(st -> st.getId().equals(s.getStudent().getId()))) {
                roster.add(s.getStudent());
            }
        }
        int rankedCount = (int) exam.getScores().stream().filter(s -> effectiveScore(s) != null).count();
        List<ExamScoreResponse> rows = new ArrayList<>();
        for (Student stu : roster) {
            ExamScore row = byId.get(stu.getId());
            ClassRoom cls = stu.getClassroom();
            Double score = effectiveScore(row);
            rows.add(ExamScoreResponse.builder()
                .studentId(stu.getId()).studentName(stu.getName())
                .classroomId(cls == null ? null : cls.getId())
                .classroomName(cls == null ? null : cls.getName())
                .score(score)
                .comment(row == null ? null : row.getComment())
                .rank(score == null ? null : rankMap.get(stu.getId()))
                .rankedCount(rankedCount)
                .sectionScores(toSectionScoreResponses(exam, row))
                .build());
        }
        ExamResponse summary = toExamSummary(exam, classStudents);
        summary.setScores(rows);
        return summary;
    }

    private static List<ExamSection> sortedSections(Exam exam) {
        if (exam.getSections() == null) return List.of();
        return exam.getSections().stream()
            .sorted(Comparator.comparingInt(ExamSection::getSortOrder).thenComparing(s -> s.getId() == null ? 0L : s.getId()))
            .toList();
    }

    private static List<ExamSectionResponse> toSectionResponses(Exam exam) {
        return sortedSections(exam).stream()
            .map(s -> ExamSectionResponse.builder()
                .id(s.getId()).name(s.getName()).maxScore(s.getMaxScore()).weight(s.getWeight()).sortOrder(s.getSortOrder())
                .build())
            .toList();
    }

    private static List<ExamScoreSectionResponse> toSectionScoreResponses(Exam exam, ExamScore row) {
        Map<Long, ExamScoreSection> bySection = row == null || row.getSectionScores() == null
            ? Map.of()
            : row.getSectionScores().stream()
                .filter(ss -> ss.getSection() != null && ss.getSection().getId() != null)
                .collect(Collectors.toMap(ss -> ss.getSection().getId(), ss -> ss, (a, b) -> a));
        List<ExamScoreSectionResponse> out = new ArrayList<>();
        for (ExamSection section : sortedSections(exam)) {
            ExamScoreSection ss = bySection.get(section.getId());
            out.add(ExamScoreSectionResponse.builder()
                .sectionId(section.getId())
                .name(section.getName())
                .maxScore(section.getMaxScore())
                .score(ss == null ? null : ss.getScore())
                .percent(ss == null ? null : ss.getPercent())
                .build());
        }
        return out;
    }

    private List<ExamResponse> listExamsByKind(AdminAccessService.Scope scope, ExamKind kind) {
        return listExams(scope).stream()
            .filter(e -> kind.name().equals(e.getKind()))
            .toList();
    }

    private static boolean matchGrade(ExamResponse e, String grade) {
        String g = blankToNull(grade);
        if (g == null) return true;
        String target = blankToNull(e.getTargetGrade());
        return target == null || g.equals(target);
    }

    private boolean matchDateRange(ExamResponse e, String from, String to) {
        LocalDate date = LocalDate.parse(e.getDate());
        if (blankToNull(from) != null && date.isBefore(parseDate(from))) return false;
        if (blankToNull(to) != null && date.isAfter(parseDate(to))) return false;
        return true;
    }

    private static boolean matchStatus(ExamResponse e, String status) {
        String s = blankToNull(status);
        if (s == null) return true;
        return s.equalsIgnoreCase(e.getStatus());
    }

    private static boolean matchQuery(ExamResponse e, String q) {
        String s = blankToNull(q);
        if (s == null) return true;
        String title = e.getTitle() == null ? "" : e.getTitle();
        return title.toLowerCase().contains(s.toLowerCase());
    }

    private static void autoDailyTitle(ExamRequest req) {
        if (req.getTitle() != null && !req.getTitle().isBlank()) return;
        String date = req.getDate() == null ? "" : req.getDate();
        req.setTitle(date + " 일일테스트");
    }

    private static void requireKind(Exam exam, ExamKind expected) {
        if (expected == null) return;
        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        if (kind != expected) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "시험을 찾을 수 없습니다.");
        }
    }

    private static String subjectSummary(Exam exam) {
        List<ExamSection> secs = sortedSections(exam);
        if (!secs.isEmpty()) {
            String names = secs.stream().map(ExamSection::getName).limit(3).collect(Collectors.joining("/"));
            return names + " " + secs.size() + "과목";
        }
        if (blankToNull(exam.getSubject()) != null) {
            return exam.getSubject().trim() + " 1과목";
        }
        return "0과목";
    }

    private static void attachLifecycle(ExamResponse res, Exam exam) {
        res.setClosed(exam.isClosed());
        res.setEnteredCount(res.getParticipantCount() == null ? 0 : res.getParticipantCount());
        List<ExamSection> secs = sortedSections(exam);
        int subjectCount = secs.isEmpty() ? (blankToNull(exam.getSubject()) == null ? 0 : 1) : secs.size();
        res.setSubjectCount(subjectCount);
        res.setSubjectSummary(subjectSummary(exam));
        String status;
        String label;
        if (exam.isClosed()) {
            status = "CLOSED";
            label = "마감";
        } else {
            int completed = res.getCompletedClasses() == null ? 0 : res.getCompletedClasses();
            int total = res.getTotalClasses() == null ? 0 : res.getTotalClasses();
            int entered = res.getParticipantCount() == null ? 0 : res.getParticipantCount();
            int students = res.getTotalStudents() == null ? 0 : res.getTotalStudents();
            boolean done = (total > 0 && completed >= total) || (total == 0 && students > 0 && entered >= students);
            if (done) {
                status = "COMPLETE";
                label = "입력완료";
            } else if (exam.getExamDate() != null && exam.getExamDate().isAfter(LocalDate.now()) && entered == 0) {
                status = "SCHEDULED";
                label = "예정";
            } else {
                status = "IN_PROGRESS";
                label = "진행중";
            }
        }
        res.setStatus(status);
        res.setStatusLabel(label);
    }

    private static ExamKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) return ExamKind.CLASS;
        String v = raw.trim();
        if ("ALL".equalsIgnoreCase(v) || "전체".equals(v) || "전체시험".equals(v)) return ExamKind.ALL;
        return ExamKind.CLASS;
    }

    private static String targetLabel(Exam exam, List<ExamClassroomResponse> rooms) {
        ExamKind kind = exam.getKind() == null ? ExamKind.CLASS : exam.getKind();
        String classPart = null;
        if (rooms != null && !rooms.isEmpty()) {
            if (rooms.size() <= 3) {
                classPart = rooms.stream().map(ExamClassroomResponse::getName).filter(Objects::nonNull).collect(Collectors.joining(" · "));
            } else {
                classPart = rooms.size() + "개 반";
            }
        } else if (exam.getClassroom() != null) {
            classPart = exam.getClassroom().getName();
        }
        if (kind == ExamKind.ALL) {
            String grade = exam.getTargetGrade();
            boolean hasGrade = grade != null && !grade.isBlank();
            if (classPart != null && !classPart.isBlank()) {
                return hasGrade ? grade + " · " + classPart : classPart;
            }
            return hasGrade ? grade + " 전체" : "전체";
        }
        return classPart == null ? "" : classPart;
    }

    private void attachProgress(ExamResponse res, Exam exam, List<Student> roster) {
        Map<Long, ExamScore> byStudent = exam.getScores() == null ? Map.of()
            : exam.getScores().stream()
                .filter(s -> s.getStudent() != null)
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s, (a, b) -> a));
        Map<Long, List<Student>> byClass = new LinkedHashMap<>();
        for (Student stu : roster) {
            if (stu.getClassroom() == null) continue;
            byClass.computeIfAbsent(stu.getClassroom().getId(), k -> new ArrayList<>()).add(stu);
        }
        List<ClassRoom> classes = examClassrooms(exam);
        if (classes.isEmpty()) {
            classes = byClass.keySet().stream()
                .map(id -> byClass.get(id).get(0).getClassroom())
                .sorted(Comparator.comparing(ClassRoom::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        }
        Map<Long, ExamInputStatus> statusMap = new HashMap<>();
        if (exam.getInputStatuses() != null) {
            for (ExamClassroomInput row : exam.getInputStatuses()) {
                if (row.getClassroom() != null) {
                    statusMap.put(row.getClassroom().getId(), row.getStatus());
                }
            }
        }
        List<ExamClassProgressResponse> progress = new ArrayList<>();
        int completed = 0;
        String firstCompleteName = null;
        for (ClassRoom cls : classes) {
            List<Student> inClass = byClass.getOrDefault(cls.getId(), List.of());
            int entered = (int) inClass.stream()
                .map(s -> byStudent.get(s.getId()))
                .map(GradeService::effectiveScore)
                .filter(Objects::nonNull)
                .count();
            ExamInputStatus st = statusMap.get(cls.getId());
            boolean complete = st == ExamInputStatus.COMPLETE
                || (st == null && !inClass.isEmpty() && entered >= inClass.size());
            if (complete) {
                completed++;
                if (firstCompleteName == null) firstCompleteName = cls.getName();
            }
            progress.add(ExamClassProgressResponse.builder()
                .classroomId(cls.getId())
                .classroomName(cls.getName())
                .entered(entered)
                .total(inClass.size())
                .complete(complete)
                .status(st == null ? (complete ? "COMPLETE" : "NONE") : st.name())
                .build());
        }
        int totalClasses = progress.size();
        String label;
        if (totalClasses == 0) {
            int enteredAll = (int) roster.stream()
                .map(s -> byStudent.get(s.getId()))
                .map(GradeService::effectiveScore)
                .filter(Objects::nonNull)
                .count();
            label = enteredAll + "/" + roster.size() + "명 입력";
        } else if (firstCompleteName != null) {
            label = firstCompleteName + " 완료 " + completed + "/" + totalClasses;
        } else {
            label = completed + "/" + totalClasses + "반 완료";
        }
        res.setClassProgress(progress);
        res.setCompletedClasses(completed);
        res.setTotalClasses(totalClasses);
        res.setProgressLabel(label);
    }

    private void applyInputStatus(Exam exam, ExamScoresRequest req) {
        if (exam.getInputStatuses() == null) {
            exam.setInputStatuses(new ArrayList<>());
        }
        Set<Long> classIds = new LinkedHashSet<>();
        if (req.getClassroomId() != null) {
            classIds.add(req.getClassroomId());
        } else {
            Map<Long, ExamScore> byStudent = exam.getScores() == null ? Map.of()
                : exam.getScores().stream()
                    .filter(s -> s.getStudent() != null)
                    .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s, (a, b) -> a));
            for (ExamScoresRequest.Item item : req.getRecords() == null ? List.<ExamScoresRequest.Item>of() : req.getRecords()) {
                ExamScore row = byStudent.get(item.getStudentId());
                if (row != null && row.getStudent() != null && row.getStudent().getClassroom() != null) {
                    classIds.add(row.getStudent().getClassroom().getId());
                }
            }
        }
        ExamInputStatus status = Boolean.TRUE.equals(req.getComplete())
            ? ExamInputStatus.COMPLETE
            : ExamInputStatus.DRAFT;
        for (Long cid : classIds) {
            ClassRoom cls = classRepo.findById(cid).orElse(null);
            if (cls == null) continue;
            ExamClassroomInput row = exam.getInputStatuses().stream()
                .filter(i -> i.getClassroom() != null && cid.equals(i.getClassroom().getId()))
                .findFirst()
                .orElse(null);
            if (row == null) {
                row = ExamClassroomInput.builder().exam(exam).classroom(cls).build();
                exam.getInputStatuses().add(row);
            }
            row.setStatus(status);
        }
    }

    private static List<ExamStatsResponse.DistributionBucket> distributionOf(List<Double> taken, int maxScore) {
        int[] buckets = new int[5];
        String[] labels = { "0–20%", "20–40%", "40–60%", "60–80%", "80–100%" };
        int max = Math.max(1, maxScore);
        for (Double score : taken == null ? List.<Double>of() : taken) {
            if (score == null) continue;
            double pct = score * 100.0 / max;
            int idx = pct >= 100 ? 4 : Math.min(4, Math.max(0, (int) (pct / 20.0)));
            buckets[idx]++;
        }
        List<ExamStatsResponse.DistributionBucket> out = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            out.add(ExamStatsResponse.DistributionBucket.builder().label(labels[i]).count(buckets[i]).build());
        }
        return out;
    }
}
