package com.example.neuralearn.service;


import org.json.JSONObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import com.google.gson.JsonObject;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("upload")
    Call<JsonObject> uploadPdf(
            @Part MultipartBody.Part file,
            @Part("user_id") RequestBody userId,
            @Part("description") RequestBody description
    );
}
