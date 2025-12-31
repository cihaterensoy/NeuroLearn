package com.example.neuralearn;

import android.content.Context;
import android.provider.Settings;

public class id_al {

    public static String getPhoneId(Context context) {
        // Sadece telefonun Android ID'sini al
        String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        return androidId;
    }
}