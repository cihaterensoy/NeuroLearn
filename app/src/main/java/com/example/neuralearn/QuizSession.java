package com.example.neuralearn;

public class QuizSession {
    private long sessionId;
    private String filename;
    private String createdAt;
    private int totalQuestions;
    private int correctAnswers;

    // PARAMETRESİZ CONSTRUCTOR
    public QuizSession() {
    }

    // Parametreli constructor
    public QuizSession(long sessionId, String filename, String createdAt, int totalQuestions, int correctAnswers) {
        this.sessionId = sessionId;
        this.filename = filename;
        this.createdAt = createdAt;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
    }

    // Getter ve Setter metodları
    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public double getSuccessRate() {
        if (totalQuestions == 0) return 0;
        return (correctAnswers * 100.0) / totalQuestions;
    }

    @Override
    public String toString() {
        return filename + " - " + String.format("%.1f%% Başarı", getSuccessRate());
    }
}