package com.example.neuralearn;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class QuizManager {
    private DatabaseHelper dbHelper;
    private Context context;

    public QuizManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    // Quiz'i tekrar başlatma metodu
    public void restartQuiz(QuizSession session) {
        try {
            Log.d("QUIZ_MANAGER", "Quiz tekrar başlatılıyor: " + session.getSessionId());

            // DEĞİŞİKLİK: Yeni session oluşturma, mevcut session'ı kullan
            long existingSessionId = session.getSessionId();

            // Bu session'a ait soruları veritabanından al
            List<Question> questions = dbHelper.getQuestionsForSession(existingSessionId);

            if (questions == null || questions.isEmpty()) {
                showToast("Bu quiz için sorular bulunamadı!");
                return;
            }

            // Soruları JSON formatına çevir
            JSONObject questionsJson = new JSONObject();
            JSONArray multipleChoiceArray = new JSONArray();
            JSONArray fillBlankArray = new JSONArray();

            for (Question question : questions) {
                JSONObject questionObj = new JSONObject();
                questionObj.put("question", question.getQuestionText());
                questionObj.put("correct_answer", question.getCorrectAnswer());

                JSONArray optionsArray = new JSONArray();
                if (question.getOptions() != null) {
                    for (String option : question.getOptions()) {
                        optionsArray.put(option);
                    }
                }
                questionObj.put("options", optionsArray);

                if ("multiple_choice".equals(question.getQuestionType())) {
                    multipleChoiceArray.put(questionObj);
                } else if ("word_bank".equals(question.getQuestionType()) || "fill_blank".equals(question.getQuestionType())) {
                    fillBlankArray.put(questionObj);
                }
            }

            questionsJson.put("multiple_choice", multipleChoiceArray);
            questionsJson.put("fill_blank", fillBlankArray);

            // DEĞİŞİKLİK: Mevcut session ID'yi gönder
            Intent intent = new Intent(context, soruEkranı.class);
            intent.putExtra("QUESTIONS_JSON", questionsJson.toString());
            intent.putExtra("SESSION_ID", existingSessionId); // ← BURASI ÖNEMLİ
            intent.putExtra("FILENAME", session.getFilename());
            context.startActivity(intent);

        } catch (Exception e) {
            Log.e("QUIZ_MANAGER", "Quiz'i tekrar başlatma hatası: " + e.getMessage(), e);
            showToast("Quiz başlatılamadı: " + e.getMessage());
        }
    }

    // Quiz silme metodu
    public boolean deleteQuiz(QuizSession session) {
        boolean success = dbHelper.deleteQuizSession(session.getSessionId());
        if (success) {
            Log.d("QUIZ_MANAGER", "Quiz silindi: " + session.getSessionId());
        } else {
            Log.e("QUIZ_MANAGER", "Quiz silinemedi: " + session.getSessionId());
        }
        return success;
    }

    // Tüm geçmişi temizleme metodu
    public boolean clearAllHistory() {
        boolean success = dbHelper.clearAllHistory();
        if (success) {
            Log.d("QUIZ_MANAGER", "Tüm geçmiş temizlendi");
        } else {
            Log.e("QUIZ_MANAGER", "Geçmiş temizlenemedi");
        }
        return success;
    }

    // İstatistikleri hesapla
    public Stats calculateStats() {
        try {
            List<QuizSession> allSessions = dbHelper.getQuizSessions();

            int totalQuestions = 0;
            int totalCorrect = 0;
            int totalSessions = allSessions.size();

            for (QuizSession session : allSessions) {
                totalQuestions += session.getTotalQuestions();
                totalCorrect += session.getCorrectAnswers();
            }

            int successRate = 0;
            if (totalQuestions > 0) {
                successRate = (int) ((totalCorrect * 100.0) / totalQuestions);
            }

            return new Stats(totalQuestions, successRate, totalSessions);

        } catch (Exception e) {
            Log.e("QUIZ_MANAGER", "İstatistik hesaplama hatası: " + e.getMessage());
            return new Stats(0, 0, 0);
        }
    }

    // Geçmiş quizleri getir
    public List<QuizSession> getRecentQuizzes() {
        return dbHelper.getQuizSessions();
    }

    private void showToast(String message) {
        // Bu metod AnasayfaActivity'de implemente edilecek
        // Burada sadece log atıyoruz
        Log.d("QUIZ_MANAGER", "Toast: " + message);
    }

    // İstatistik veri sınıfı
    public static class Stats {
        public int totalQuestions;
        public int successRate;
        public int streak;

        public Stats(int totalQuestions, int successRate, int streak) {
            this.totalQuestions = totalQuestions;
            this.successRate = successRate;
            this.streak = streak;
        }
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}