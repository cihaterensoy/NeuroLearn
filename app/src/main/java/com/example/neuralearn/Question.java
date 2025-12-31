package com.example.neuralearn;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private String id;
    private String questionText;
    private String questionType;
    private List<String> options;
    private String correctAnswer;
    private String questionTitle;
    private String imageUrl;
    private List<String> selectedWords;
    private String userAnswer;

    // PARAMETRESİZ CONSTRUCTOR - EKLENDİ
    public Question() {
        this.selectedWords = new ArrayList<>();
        this.options = new ArrayList<>();
    }

    // Mevcut parametreli constructor
    public Question(String id, String questionText, String questionType,
                    List<String> options, String correctAnswer) {
        this.id = id;
        this.questionText = questionText;
        this.questionType = questionType;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.selectedWords = new ArrayList<>();
    }

    // Getter ve Setter metodları (mevcut)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getSelectedWords() { return selectedWords; }
    public void setSelectedWords(List<String> selectedWords) { this.selectedWords = selectedWords; }
    public void addSelectedWord(String word) {
        if (this.selectedWords == null) {
            this.selectedWords = new ArrayList<>();
        }
        this.selectedWords.add(word);
    }
    public void clearSelectedWords() {
        if (this.selectedWords != null) {
            this.selectedWords.clear();
        }
    }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    // Sorunun doğru cevaplanıp cevaplanmadığını kontrol etme
    public boolean isCorrect() {
        if (selectedWords == null || selectedWords.isEmpty()) {
            return false;
        }

        if ("multiple_choice".equals(questionType)) {
            return selectedWords.get(0).equals(correctAnswer);
        } else if ("word_bank".equals(questionType) || "fill_blank".equals(questionType)) {
            return selectedWords.get(0).equals(correctAnswer);
        }
        return false;
    }
}