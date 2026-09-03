package com.hiacademy.api.entity;

public enum ExamKind {
    ALL,
    CLASS;

    public String label() {
        return this == ALL ? "정기고사/모의고사" : "일일테스트";
    }
}
