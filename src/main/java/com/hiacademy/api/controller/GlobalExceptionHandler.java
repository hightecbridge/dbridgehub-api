package com.hiacademy.api.controller;
import com.hiacademy.api.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 반드시 {@link ResponseEntity}로 HTTP 상태를 넘겨야 합니다.
     * 본문만 반환하면 로그인 실패(401)가 200으로 나가 axios가 성공 처리되어 프론트 오류 메시지가 깨집니다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException ex) {
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.fail(msg));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.fail(ex.getBindingResult().getFieldErrors().stream()
            .map(e->e.getField()+": "+e.getDefaultMessage()).collect(Collectors.joining(", ")));
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception ex) {
        return ApiResponse.fail("서버 오류: "+ex.getMessage());
    }
}
