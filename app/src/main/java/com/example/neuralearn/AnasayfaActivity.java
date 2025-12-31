package com.example.neuralearn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnasayfaActivity extends AppCompatActivity implements
        RecentTopicsAdapter.OnQuizItemClickListener,
        DialogManager.OnOptionSelectedListener,
        DialogManager.OnDetailActionListener {

    // Manager sınıfları
    private QuizManager quizManager;
    private DialogManager dialogManager;

    // UI bileşenleri
    private RecyclerView rvRecentTopics;
    private View emptyState;
    private TextView tvTotalQuestions, tvSuccessRate, tvStreak;
    private RecentTopicsAdapter recentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anasayfa);

        // Manager sınıflarını başlat
        quizManager = new QuizManager(this);
        dialogManager = new DialogManager(this, quizManager);

        // UI bileşenlerini başlat
        initViews();
        setupStats();
        loadRecentQuizzes();

        CardView cardUploadPdf = findViewById(R.id.cardUploadPdf);
        CardView cardEnterTopic = findViewById(R.id.cardEnterTopic);

        // PDF Yükle butonu - Yeni aktiviteye yönlendir
        cardUploadPdf.setOnClickListener(v -> {
            Intent intent = new Intent(AnasayfaActivity.this, PdfUploadActivity.class);
            startActivity(intent);
        });

        // Konu Gir butonu
        cardEnterTopic.setOnClickListener(v -> {
            showToast("Yakında eklenecek!");
        });
    }

    // UI bileşenlerini başlat
    private void initViews() {
        rvRecentTopics = findViewById(R.id.rvRecentTopics);
        emptyState = findViewById(R.id.emptyState);
        tvTotalQuestions = findViewById(R.id.tvTotalQuestions);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvStreak = findViewById(R.id.tvStreak);

        // RecyclerView ayarla
        rvRecentTopics.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentTopicsAdapter(this);
        rvRecentTopics.setAdapter(recentAdapter);
    }

    // İstatistikleri ayarla
    private void setupStats() {
        QuizManager.Stats stats = quizManager.calculateStats();
        tvTotalQuestions.setText(String.valueOf(stats.totalQuestions));
        tvSuccessRate.setText(stats.successRate + "%");
        tvStreak.setText(String.valueOf(stats.streak));
    }

    // Geçmiş quizleri yükle
    private void loadRecentQuizzes() {
        try {
            List<QuizSession> recentSessions = quizManager.getRecentQuizzes();
            Log.d("RECENT_QUIZZES", "Yüklenen quiz sayısı: " + recentSessions.size());

            if (recentSessions.isEmpty()) {
                rvRecentTopics.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                rvRecentTopics.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                recentAdapter.updateData(recentSessions);
            }

            setupStats();

        } catch (Exception e) {
            Log.e("RECENT_QUIZZES", "Geçmiş quizleri yükleme hatası: " + e.getMessage());
            rvRecentTopics.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    // RecentTopicsAdapter.OnQuizItemClickListener implementasyonu
    @Override
    public void onQuizClick(QuizSession session) {
        quizManager.restartQuiz(session);
    }

    @Override
    public void onQuizLongClick(QuizSession session) {
        dialogManager.showQuizOptionsDialog(session, this);
    }

    // DialogManager.OnOptionSelectedListener implementasyonu
    @Override
    public void onOptionSelected(int optionIndex, QuizSession session) {
        switch (optionIndex) {
            case 0: // Tekrar Çöz
                quizManager.restartQuiz(session);
                break;
            case 1: // Detayları Gör
                dialogManager.showQuizDetailsDialog(session, this);
                break;
            case 2: // Quiz'i Sil
                showDeleteConfirmation(session);
                break;
        }
    }

    // DialogManager.OnDetailActionListener implementasyonu
    @Override
    public void onRestartQuiz(QuizSession session) {
        quizManager.restartQuiz(session);
    }

    @Override
    public void onDeleteQuiz(QuizSession session) {
        showDeleteConfirmation(session);
    }

    // Silme onayı göster
    private void showDeleteConfirmation(QuizSession session) {
        dialogManager.showDeleteQuizDialog(session, new DialogManager.DialogCallback() {
            @Override
            public void onConfirm() {
                boolean success = quizManager.deleteQuiz(session);
                if (success) {
                    showToast("Quiz silindi");
                    loadRecentQuizzes();
                } else {
                    showToast("Quiz silinirken hata oluştu");
                }
            }

            @Override
            public void onCancel() {
                // İptal edildi, bir şey yapma
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentQuizzes();
    }

    // Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_history) {
            showClearAllHistoryConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Tüm geçmişi temizleme onayı
    private void showClearAllHistoryConfirmation() {
        List<QuizSession> recentSessions = quizManager.getRecentQuizzes();
        if (recentSessions.isEmpty()) {
            showToast("Silinecek quiz bulunamadı");
            return;
        }

        dialogManager.showClearAllHistoryDialog(new DialogManager.DialogCallback() {
            @Override
            public void onConfirm() {
                boolean success = quizManager.clearAllHistory();
                if (success) {
                    showToast("Tüm geçmiş temizlendi");
                    loadRecentQuizzes();
                } else {
                    showToast("Geçmiş temizlenirken hata oluştu");
                }
            }

            @Override
            public void onCancel() {
                // İptal edildi
            }
        });
    }

    // Toast göstermek için yardımcı metod
    private void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(AnasayfaActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizManager != null) {
            quizManager.close();
        }
    }
}