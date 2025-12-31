package com.example.neuralearn;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

public class DialogManager {
    private Context context;
    private QuizManager quizManager;

    public interface DialogCallback {
        void onConfirm();
        void onCancel();
    }

    public DialogManager(Context context, QuizManager quizManager) {
        this.context = context;
        this.quizManager = quizManager;
    }

    // Quiz silme dialog'u
    public void showDeleteQuizDialog(QuizSession session, DialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Quiz'i Sil")
                .setMessage("'" + session.getFilename() + "' quizini silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz!")
                .setPositiveButton("Sil", (dialog, which) -> {
                    if (callback != null) callback.onConfirm();
                })
                .setNegativeButton("İptal", (dialog, which) -> {
                    if (callback != null) callback.onCancel();
                })
                .show();
    }

    // Tüm geçmişi temizleme dialog'u
    public void showClearAllHistoryDialog(DialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tüm Geçmişi Temizle")
                .setMessage("Tüm quiz geçmişini silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz!")
                .setPositiveButton("Tümünü Sil", (dialog, which) -> {
                    if (callback != null) callback.onConfirm();
                })
                .setNegativeButton("İptal", (dialog, which) -> {
                    if (callback != null) callback.onCancel();
                })
                .show();
    }

    // Quiz seçenekleri menüsü
    public void showQuizOptionsDialog(QuizSession session,
                                      OnOptionSelectedListener listener) {
        String[] options = {"Tekrar Çöz", "Detayları Gör", "Quiz'i Sil"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(session.getFilename())
                .setItems(options, (dialog, which) -> {
                    if (listener != null) {
                        listener.onOptionSelected(which, session);
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    // Quiz detaylarını göster
    public void showQuizDetailsDialog(QuizSession session,
                                      OnDetailActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Quiz Detayları")
                .setMessage(String.format(
                        "Dosya: %s\n\n" +
                                "Tarih: %s\n" +
                                "Sonuç: %d/%d soru\n" +
                                "Başarı: %.1f%%",
                        session.getFilename(),
                        session.getCreatedAt(),
                        session.getCorrectAnswers(),
                        session.getTotalQuestions(),
                        session.getSuccessRate()
                ))
                .setPositiveButton("Tekrar Çöz", (dialog, which) -> {
                    if (listener != null) listener.onRestartQuiz(session);
                })
                .setNeutralButton("Sil", (dialog, which) -> {
                    if (listener != null) listener.onDeleteQuiz(session);
                })
                .setNegativeButton("Kapat", null)
                .show();
    }

    public interface OnOptionSelectedListener {
        void onOptionSelected(int optionIndex, QuizSession session);
    }

    public interface OnDetailActionListener {
        void onRestartQuiz(QuizSession session);
        void onDeleteQuiz(QuizSession session);
    }
}