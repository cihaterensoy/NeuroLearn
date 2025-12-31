package com.example.neuralearn;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.neuralearn.R;
import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class soruEkranı extends AppCompatActivity {

    // UI bileşenleri
    private ProgressBar progressBar;
    private TextView tvQuestionCounter, tvQuestionTitle, tvQuestion;
    private TextView tvFeedback, tvFeedbackIcon, tvCorrectAnswer;
    private LinearLayout multipleChoiceContainer;
    private CardView feedbackCard, answerCard, wordBankCard, imageCard;
    private FlexboxLayout answerArea, wordBankContainer;
    private Button btnCheck;
    private ImageView ivQuestionImage;
    private ImageButton btnClose;

    // Veri ve durum değişkenleri
    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private boolean isAnswerChecked = false;
    private Button selectedOptionButton = null;

    // Veritabanı değişkenleri - EKLENDİ
    private DatabaseHelper dbHelper;
    private long currentSessionId;
    private String currentFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.soruekrani);

        // DatabaseHelper'ı başlat - EKLENDİ
        dbHelper = new DatabaseHelper(this);

        initViews();
        loadQuestionsFromAPI();
        setupListeners();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
        tvQuestionTitle = findViewById(R.id.tvQuestionTitle);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvFeedbackIcon = findViewById(R.id.tvFeedbackIcon);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);

        multipleChoiceContainer = findViewById(R.id.multipleChoiceContainer);
        feedbackCard = findViewById(R.id.feedbackCard);
        answerCard = findViewById(R.id.answerCard);
        wordBankCard = findViewById(R.id.wordBankCard);
        imageCard = findViewById(R.id.imageCard);

        answerArea = findViewById(R.id.answerArea);
        wordBankContainer = findViewById(R.id.wordBankContainer);
        btnCheck = findViewById(R.id.btnCheck);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        btnClose = findViewById(R.id.btnClose);
    }

    private void setupListeners() {
        btnCheck.setOnClickListener(v -> {
            if (!isAnswerChecked) {
                checkAnswer();
            } else {
                moveToNextQuestion();
            }
        });

        btnClose.setOnClickListener(v -> showExitDialog());
    }

    // GÜNCELLENDİ: WebSocket'ten gelen soruları işle ve veritabanına kaydet
    private void loadQuestionsFromAPI() {
        try {
            String questionsJson = getIntent().getStringExtra("QUESTIONS_JSON");
            currentFilename = getIntent().getStringExtra("FILENAME");

            // DEĞİŞİKLİK: Intent'ten SESSION_ID geliyor mu kontrol et
            if (getIntent().hasExtra("SESSION_ID")) {
                // Tekrar çözme durumu - mevcut session'ı kullan
                currentSessionId = getIntent().getLongExtra("SESSION_ID", -1);
                Log.d("DB_DEBUG", "Mevcut oturum kullanılıyor: " + currentSessionId);

                // Session'ı resetle (eski cevapları temizle)
                dbHelper.resetQuizSession(currentSessionId);
            } else {
                // Yeni quiz başlatma durumu - yeni session oluştur
                currentSessionId = dbHelper.createQuizSession(currentFilename);
                Log.d("DB_DEBUG", "Yeni oturum oluşturuldu: " + currentSessionId);

                // Soruları veritabanına kaydet (sadece yeni session için)
                dbHelper.saveQuestions(currentSessionId, questionsJson);
            }

            parseQuestionsFromJson(questionsJson);

        } catch (Exception e) {
            Log.e("QUESTIONS_ERROR", "Soru yükleme hatası: " + e.getMessage());
            setupTestQuestions();
        }
    }

    // JSON'dan soruları parse et
    private void parseQuestionsFromJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            questions.clear();

            // Çoktan seçmeli soruları işle
            if (json.has("multiple_choice")) {
                JSONArray mcArray = json.getJSONArray("multiple_choice");
                for (int i = 0; i < mcArray.length(); i++) {
                    JSONObject q = mcArray.getJSONObject(i);
                    String questionText = q.getString("question");
                    String correctAnswer = q.getString("correct_answer");

                    // Seçenekleri al
                    List<String> options = new ArrayList<>();
                    JSONArray optionsArray = q.getJSONArray("options");
                    for (int j = 0; j < optionsArray.length(); j++) {
                        options.add(optionsArray.getString(j));
                    }

                    Question question = new Question(
                            String.valueOf(i + 1),
                            questionText,
                            "multiple_choice",
                            options,
                            correctAnswer
                    );
                    question.setQuestionTitle("Çoktan Seçmeli");
                    questions.add(question);
                    Log.d("QUESTION_ADD", "Çoktan seçmeli eklendi: " + questionText);
                }
            }

            // Boşluk doldurma sorularını işle
            if (json.has("fill_blank")) {
                JSONArray fbArray = json.getJSONArray("fill_blank");
                for (int i = 0; i < fbArray.length(); i++) {
                    JSONObject q = fbArray.getJSONObject(i);
                    String questionText = q.getString("question");
                    String correctAnswer = q.getString("correct_answer");

                    // Boşluk doldurma için seçenekleri oluştur
                    List<String> options = new ArrayList<>();
                    options.add(correctAnswer); // Doğru cevap

                    // Yanlış seçenekler ekle (eğer varsa)
                    if (q.has("options")) {
                        JSONArray optionsArray = q.getJSONArray("options");
                        for (int j = 0; j < optionsArray.length(); j++) {
                            String option = optionsArray.getString(j);
                            if (!option.equals(correctAnswer)) {
                                options.add(option);
                            }
                        }
                    }

                    // Eğer yeterli seçenek yoksa, bazı ek seçenekler ekle
                    if (options.size() < 3) {
                        options.add("is");
                        options.add("are");
                        options.add("were");
                    }

                    Question question = new Question(
                            String.valueOf(questions.size() + 1),
                            questionText,
                            "word_bank", // Boşluk doldurma için word_bank kullan
                            options,
                            correctAnswer
                    );
                    question.setQuestionTitle("Boşluk Doldurma");
                    questions.add(question);
                    Log.d("QUESTION_ADD", "Boşluk doldurma eklendi: " + questionText);
                }
            }

            // Hiç soru yüklenemediyse test sorularını kullan
            if (questions.isEmpty()) {
                Log.w("QUESTIONS_WARN", "Hiç soru yüklenemedi, test moduna geçiliyor");
                setupTestQuestions();
            } else {
                Log.i("QUESTIONS_INFO", questions.size() + " soru başarıyla yüklendi");
                displayQuestion();
            }

        } catch (JSONException e) {
            Log.e("JSON_PARSE_ERROR", "JSON parse hatası: " + e.getMessage());
            setupTestQuestions();
        }
    }

    private void setupTestQuestions() {
        Log.d("QUESTIONS_TEST", "Test soruları yükleniyor");

        // Test için örnek sorular
        Question q1 = new Question("1", "I ___ a student.", "word_bank",
                Arrays.asList("am", "is", "are", "be", "was"), "am");
        q1.setQuestionTitle("Cümleyi tamamlayın");

        Question q2 = new Question("2", "What is the capital of Turkey?", "multiple_choice",
                Arrays.asList("Istanbul", "Ankara", "Izmir", "Bursa"), "Ankara");
        q2.setQuestionTitle("Doğru cevabı seçin");

        Question q3 = new Question("3", "She ___ to school every day.", "word_bank",
                Arrays.asList("goes", "go", "going", "went", "gone"), "goes");
        q3.setQuestionTitle("Boşluğu doldurun");

        questions.clear();
        questions.add(q1);
        questions.add(q2);
        questions.add(q3);

        // Test modunda da veritabanına kaydet - EKLENDİ
        currentSessionId = dbHelper.createQuizSession("test_session");
        Log.d("DB_DEBUG", "Test oturumu oluşturuldu: " + currentSessionId);

        displayQuestion();
    }

    // Mevcut soruyu ekranda gösterme
    private void displayQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            showResults();
            return;
        }

        Question question = questions.get(currentQuestionIndex);

        // Progress bar'ı güncelle
        progressBar.setMax(questions.size());
        progressBar.setProgress(currentQuestionIndex + 1);
        tvQuestionCounter.setText((currentQuestionIndex + 1) + "/" + questions.size());

        // Soru başlık ve metnini ayarla
        tvQuestionTitle.setText(question.getQuestionTitle());
        tvQuestion.setText(question.getQuestionText());

        // Durumu sıfırla (yeni soru için)
        feedbackCard.setVisibility(View.GONE);
        isAnswerChecked = false;
        btnCheck.setText("KONTROL ET");
        btnCheck.setEnabled(false);
        question.clearSelectedWords();
        selectedOptionButton = null;

        // Tüm soru container'larını başlangıçta gizle
        multipleChoiceContainer.setVisibility(View.GONE);
        answerCard.setVisibility(View.GONE);
        wordBankCard.setVisibility(View.GONE);
        imageCard.setVisibility(View.GONE);

        // Soru tipine göre ilgili container'ı göster
        if ("multiple_choice".equals(question.getQuestionType())) {
            showMultipleChoice(question);
        } else if ("word_bank".equals(question.getQuestionType())) {
            showWordBank(question);
        }

        // Eğer sorunun görseli varsa göster
        if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
            imageCard.setVisibility(View.VISIBLE);
            // Glide.with(this).load(question.getImageUrl()).into(ivQuestionImage);
        }
    }

    // Çoktan seçmeli soruyu ekranda gösterme
    private void showMultipleChoice(Question question) {
        multipleChoiceContainer.setVisibility(View.VISIBLE);
        multipleChoiceContainer.removeAllViews();

        // Her seçenek için buton oluştur
        for (String option : question.getOptions()) {
            Button optionButton = createModernOptionButton(option);

            // Buton tıklama dinleyicisi
            optionButton.setOnClickListener(v -> {
                if (!isAnswerChecked) {
                    selectOption(optionButton);
                    question.clearSelectedWords();
                    question.addSelectedWord(option);
                    btnCheck.setEnabled(true);
                }
            });

            multipleChoiceContainer.addView(optionButton);
        }
    }

    // Kelime bankası soruyu ekranda gösterme
    private void showWordBank(Question question) {
        answerCard.setVisibility(View.VISIBLE);
        wordBankCard.setVisibility(View.VISIBLE);

        answerArea.removeAllViews();
        wordBankContainer.removeAllViews();

        // Karıştırılmış seçenekler
        List<String> shuffledOptions = new ArrayList<>(question.getOptions());
        // Basit karıştırma
        java.util.Collections.shuffle(shuffledOptions);

        // Her kelime için "chip" (kart) oluştur
        for (String word : shuffledOptions) {
            TextView wordChip = createModernWordChip(word, false);

            // Chip tıklama dinleyicisi
            wordChip.setOnClickListener(v -> {
                if (!isAnswerChecked) {
                    onWordClicked(wordChip, word, question);
                }
            });
            wordBankContainer.addView(wordChip);
        }
    }

    // Kelime chip'ine tıklandığında yapılacak işlemler
    private void onWordClicked(TextView wordChip, String word, Question question) {
        // Eğer kelime daha önce seçilmediyse (tag kontrolü)
        if (wordChip.getTag() == null || !(boolean) wordChip.getTag()) {
            // Kelimeyi sorunun seçili kelimeler listesine ekle
            question.addSelectedWord(word);

            // Cevap alanı için yeni chip oluştur
            TextView answerChip = createModernWordChip(word, true);
            answerChip.setOnClickListener(v -> {
                if (!isAnswerChecked) {
                    onAnswerChipClicked(answerChip, word, question);
                }
            });
            answerArea.addView(answerChip);

            // Kelime bankasındaki chip'i görünmez yap ve seçildi olarak işaretle
            wordChip.setVisibility(View.INVISIBLE);
            wordChip.setTag(true);

            // Eğer en az bir kelime seçildiyse kontrol butonunu aktif et
            btnCheck.setEnabled(!question.getSelectedWords().isEmpty());
        }
    }

    // Cevap alanındaki chip'e tıklandığında (kelimeyi geri kaldırma)
    private void onAnswerChipClicked(TextView answerChip, String word, Question question) {
        // Cevap alanından chip'i kaldır
        answerArea.removeView(answerChip);
        // Sorunun seçili kelimeler listesinden kelimeyi çıkar
        question.getSelectedWords().remove(word);

        // Kelime bankasında ilgili kelimeyi tekrar görünür yap
        for (int i = 0; i < wordBankContainer.getChildCount(); i++) {
            View child = wordBankContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                // Doğru kelimeyi bul ve görünür yap
                if (chip.getText().equals(word) && child.getVisibility() == View.INVISIBLE) {
                    child.setVisibility(View.VISIBLE);
                    child.setTag(false); // Seçili olmadığını işaretle
                    break;
                }
            }
        }

        // Eğer hiç kelime kalmadıysa kontrol butonunu devre dışı bırak
        btnCheck.setEnabled(!question.getSelectedWords().isEmpty());
    }

    // Modern görünümlü çoktan seçmeli buton oluşturma
    private Button createModernOptionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setBackgroundColor(Color.parseColor("#f0f0f0"));

        // Padding ayarları
        button.setPadding(
                dpToPx(20),
                dpToPx(16),
                dpToPx(20),
                dpToPx(16)
        );

        // Layout parametreleri
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(12));
        button.setLayoutParams(params);

        return button;
    }

    // Modern görünümlü kelime chip'i (kart) oluşturma
    private TextView createModernWordChip(String text, boolean isInAnswer) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(16);
        chip.setPadding(
                dpToPx(16),
                dpToPx(10),
                dpToPx(16),
                dpToPx(10)
        );

        // Basit stil
        chip.setBackgroundColor(isInAnswer ? Color.parseColor("#E3F2FD") : Color.parseColor("#F5F5F5"));
        chip.setTextColor(Color.BLACK);
        chip.setClickable(true);

        // Kenarlık
        //chip.setBackgroundResource(R.drawable.chip_background); // Eğer drawable varsa

        // Flexbox layout parametreleri
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        chip.setLayoutParams(params);

        chip.setTag(false);
        return chip;
    }

    // Çoktan seçmeli seçenek seçildiğinde
    private void selectOption(Button selectedButton) {
        // Önceki seçimi temizle (varsa)
        if (selectedOptionButton != null) {
            selectedOptionButton.setBackgroundColor(Color.parseColor("#f0f0f0"));
        }

        // Yeni butonu seçili yap
        selectedButton.setBackgroundColor(Color.parseColor("#2196F3"));
        selectedButton.setTextColor(Color.WHITE);
        selectedOptionButton = selectedButton;
    }

    // GÜNCELLENDİ: Cevabı kontrol etme ve veritabanına kaydetme
    private void checkAnswer() {
        Question question = questions.get(currentQuestionIndex);
        boolean isCorrect = question.isCorrect();

        // Kullanıcı cevabını veritabanına kaydet - EKLENDİ
        String userAnswer = question.getSelectedWords().isEmpty() ? "" : question.getSelectedWords().get(0);
        long questionId = Long.parseLong(question.getId());
        dbHelper.saveUserAnswer(questionId, userAnswer, isCorrect);
        Log.d("DB_DEBUG", "Kullanıcı cevabı kaydedildi: " + userAnswer + " - Doğru: " + isCorrect);

        // Doğru cevap sayısını güncelle
        if (isCorrect) {
            correctAnswers++;
            showFeedback(true, null);
        } else {
            showFeedback(false, question.getCorrectAnswer());
        }

        // Durumu güncelle
        isAnswerChecked = true;
        btnCheck.setText("DEVAM ET");
        btnCheck.setEnabled(true);

        // Tüm etkileşimleri devre dışı bırak
        disableAllInteractions();
    }

    // Cevap kontrol edildikten sonra tüm etkileşimleri devre dışı bırakma
    private void disableAllInteractions() {
        // Kelime bankası chip'lerini devre dışı bırak
        for (int i = 0; i < wordBankContainer.getChildCount(); i++) {
            wordBankContainer.getChildAt(i).setEnabled(false);
        }
        // Cevap alanı chip'lerini devre dışı bırak
        for (int i = 0; i < answerArea.getChildCount(); i++) {
            answerArea.getChildAt(i).setEnabled(false);
        }

        // Çoktan seçmeli butonları devre dışı bırak
        for (int i = 0; i < multipleChoiceContainer.getChildCount(); i++) {
            multipleChoiceContainer.getChildAt(i).setEnabled(false);
        }
    }

    // Geri bildirim kartını gösterme
    private void showFeedback(boolean isCorrect, String correctAnswer) {
        feedbackCard.setVisibility(View.VISIBLE);

        if (isCorrect) {
            tvFeedback.setText("Doğru!");
            tvFeedback.setTextColor(Color.GREEN);
            tvFeedbackIcon.setText("✓");
            tvCorrectAnswer.setVisibility(View.GONE);
        } else {
            tvFeedback.setText("Yanlış!");
            tvFeedback.setTextColor(Color.RED);
            tvFeedbackIcon.setText("✗");
            tvCorrectAnswer.setVisibility(View.VISIBLE);
            tvCorrectAnswer.setText("Doğru cevap: " + correctAnswer);
        }
    }

    // Sonraki soruya geçme
    private void moveToNextQuestion() {
        currentQuestionIndex++;
        displayQuestion();
    }

    // GÜNCELLENDİ: Test sonuçlarını gösterme ve veritabanına kaydetme
    private void showResults() {
        double percentage = (double) correctAnswers / questions.size() * 100;

        // Quiz sonuçlarını veritabanına kaydet
        dbHelper.updateQuizResults(currentSessionId, questions.size(), correctAnswers);
        Log.d("DB_DEBUG", "Quiz sonuçları kaydedildi: " + correctAnswers + "/" + questions.size());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Tamamlandı")
                .setMessage(String.format(
                        "%d doğru cevap verdiniz!\nToplam soru: %d\nBaşarı: %.1f%%",
                        correctAnswers, questions.size(), percentage
                ))
                .setPositiveButton("Tamam", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    // Çıkış dialog'unu gösterme
    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Çıkış")
                .setMessage("Testten çıkmak istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> finish())
                .setNegativeButton("Hayır", null)
                .show();
    }

    // DP değerini PX'e çevirme
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Veritabanı bağlantısını kapat - EKLENDİ
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}