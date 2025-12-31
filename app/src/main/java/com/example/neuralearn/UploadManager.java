package com.example.neuralearn;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.neuralearn.adapter.ApiClient;
import com.example.neuralearn.service.ApiService;
import com.example.neuralearn.FileUtil;

import com.google.gson.JsonObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class UploadManager {

    private ApiService apiService;

    public UploadManager() {
        // Retrofit servisini başlat
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
    }

    // PDF yükleme fonksiyonu
    public void uploadPdf(Context context, Uri pdfUri, String userId, UploadCallback callback) {
        try {
            // PDF'i RequestBody'e çevir
            RequestBody requestBody = FileUtil.getRequestBodyFromUri(context, pdfUri);
            RequestBody userBody = RequestBody.create(MediaType.parse("text/plain"), userId);

            // MultipartBody.Part oluştur
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", "upload.pdf", requestBody);

            // Opsiyonel description
            RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "My PDF file");


            // API çağrısı
            Call<JsonObject> call = apiService.uploadPdf(filePart, userBody, description);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("UPLOAD_MANAGER", "Başarılı: " + response.body().toString());
                        callback.onSuccess(response.body().toString()); // İstersen string'e çevirebilirsin
                    } else {
                        Log.e("UPLOAD_MANAGER", "Sunucu hatası: " + response.code());
                        callback.onError("Sunucu hatası: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("UPLOAD_MANAGER", "Hata: " + t.getMessage());
                    callback.onError(t.getMessage());
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
            callback.onError(e.getMessage());
        }
    }

    // Callback arayüzü
    public interface UploadCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }
}