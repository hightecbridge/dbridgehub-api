package com.hiacademy.api.security;
public record TokenDetails(Long subjectId, String role, String type, Long academyId) {}
