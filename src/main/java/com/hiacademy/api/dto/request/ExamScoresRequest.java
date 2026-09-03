package com.hiacademy.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamScoresRequest {
    @NotNull
    @Valid
    private List<Item> records;
    /** true면 해당 반(또는 전체)을 입력 완료로 표시 */
    private Boolean complete;
    /** 지정하면 해당 반만 DRAFT/COMPLETE 상태를 갱신 */
    private Long classroomId;

    @Data
    public static class Item {
        @NotNull private Long studentId;
        /** null = 미응시 */
        private Double score;
        private String comment;
        @Valid
        private List<SectionScore> sectionScores;

        @Data
        public static class SectionScore {
            @NotNull private Long sectionId;
            private Double score;
            private Double percent;
        }
    }
}
