package com.example.neuralearn;

import android.content.Context;
import android.net.Uri;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class FileUtil {

    public static RequestBody getRequestBodyFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("InputStream null");

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        inputStream.close();

        // OkHttp 3.x için doğru kullanım
        return RequestBody.create(MediaType.parse("application/pdf"), byteBuffer.toByteArray());
    }
}