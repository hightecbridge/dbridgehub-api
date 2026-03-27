package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.util.List;
@Data @Builder public class ParentHomeResponse {
    private AcademyInfo academy;
    private List<StudentSummary> students;
    private List<NoticeResponse> recentNotices;
    private List<EventResponse>  upcomingEvents;
    @Data @Builder public static class StudentSummary {
        private Long id;
        private String name, className, grade, status;
        private List<FeeResponse> fees;
        private int hwPendingCount;
    }
}
