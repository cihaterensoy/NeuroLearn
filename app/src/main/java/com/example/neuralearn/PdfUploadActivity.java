package com.example.neuralearn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.neuralearn.adapter.WebSocketClient;
import org.json.JSONException;
import org.json.JSONObject;

public class PdfUploadActivity extends AppCompatActivity {
    private static final int PDF_REQUEST_CODE = 1001;
    private String id;
    private WebSocketClient wsClient;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_upload);

        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        id = id_al.getPhoneId(this);
        wsClient = new WebSocketClient();

        // WebSocket Listener
        wsClient.setListener(new WebSocketClient.OnMessageReceived() {
            @Override
            public void onMessage(String message) {
                Log.d("WS_DEBUG", "Sunucudan mesaj: " + message);
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(message);
                        String status = json.getString("status");

                        switch (status) {
                            case "uploaded":
                                tvStatus.setText("PDF yüklendi, işleniyor...");
                                break;
                            case "processing":
                                tvStatus.setText("PDF sunucuda işleniyor...");
                                break;
                            case "extracting":
                                tvStatus.setText("PDF'den metin çıkarılıyor...");
                                break;
                            case "generating":
                                tvStatus.setText("Yapay zeka soruları oluşturuyor...");
                                break;
                            case "completed":
                                tvStatus.setText("Sorular hazır!");
                                progressBar.setVisibility(View.GONE);

                                if (json.has("questions")) {
                                    String questionsJson = json.getJSONObject("questions").toString();
                                    String filename = json.getString("filename");

                                    Intent intent = new Intent(PdfUploadActivity.this, soruEkranı.class);
                                    intent.putExtra("QUESTIONS_JSON", questionsJson);
                                    intent.putExtra("FILENAME", filename);
                                    startActivity(intent);
                                    finish(); // PdfUploadActivity'yi kapat
                                } else {
                                    tvStatus.setText("Sorular alınamadı!");
                                }
                                break;
                            case "error":
                                progressBar.setVisibility(View.GONE);
                                String errorMsg = json.getString("message");
                                tvStatus.setText("Hata: " + errorMsg);
                                break;
                        }
                    } catch (JSONException e) {
                        Log.e("WS_JSON", "JSON parse hatası: " + e.getMessage());
                        tvStatus.setText("Mesaj işleme hatası");
                    }
                });
            }
        });

        // WebSocket bağlantısını başlat
        wsClient.connect(id);

        // Test mesajı gönder
        new android.os.Handler().postDelayed(
                () -> wsClient.send("{\"type\": \"hello\", \"userId\": \"" + id + "\"}"),
                1000
        );

        // PDF seçme işlemini başlat
        openPdfPicker();
    }

    private void openPdfPicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PDF_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PDF_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri pdfUri = data.getData();
            Log.d("PDF_UPLOAD", "Seçilen PDF URI: " + pdfUri);

            tvStatus.setText("PDF sunucuya yükleniyor...");
            progressBar.setVisibility(View.VISIBLE);

            UploadManager uploadManager = new UploadManager();
            uploadManager.uploadPdf(this, pdfUri, id, new UploadManager.UploadCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d("PDF_UPLOAD", "Başarılı: " + response);
                    // WebSocket üzerinden zaten cevap gelecek, burada bir şey yapmıyoruz.
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("PDF_UPLOAD", "Hata: " + errorMessage);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("Yükleme hatası: " + errorMessage);
                    });
                }
            });
        } else {
            // Eğer kullanıcı PDF seçmediyse, aktiviteyi kapat
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}