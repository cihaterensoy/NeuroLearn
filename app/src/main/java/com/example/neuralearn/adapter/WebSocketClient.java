package com.example.neuralearn.adapter;
import android.util.Log; //loglama için

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient; //http/websocket bağlantısı kurmak için temel sınıf
import okhttp3.Request; //websocket veya http isteği oluşturmak için
import okhttp3.Response;  // sunucudan gelen cevabı temsil eder
import okhttp3.WebSocket; //gerçek webSocket bağlantısı
import okhttp3.WebSocketListener; //websocket olaylarını dinlemeye yarar (açılma mesaj kapanma hata)

public class WebSocketClient {
    private WebSocket webSocket;
    // bu sınıf, websocket ile sunucuya bağlanmayı ve mesaj alamayı kolaylaştırır
    //websocket bağlantısını saklamak için değişken
    //doğrudan erişim yok bu sınıfın içersinde kullanılacak

    // ----  EKLENDİ: Activity’den mesaj almak için arayüz ----
    public interface OnMessageReceived {
        void onMessage(String message);
    }

    private OnMessageReceived listener; // Dışarıdan gelecek dinleyici

    // ----  EKLENDİ: Activity'den listener bağlamak için ----
    public void setListener(OnMessageReceived listener) {
        this.listener = listener;
    }

    public void connect(String userId){
        //connect() metodu sunucuya bağlanamyı başlatır
        //userId kullanıcıyı sunucuda tanıtmak için kullanılır
        //her kullanıcı için ayrı bir websocket bağlantısı açılabilir
        OkHttpClient client = new OkHttpClient();
        //websocket veya http bağlantılarını yapacak client nesnesi oluşturuyoruz
        //bu nesne bağlantıyı yönetir ve mesajları gönderip alır
        Request request = new Request.Builder()
                .url("ws://10.0.2.2:5168/ws/"+userId)
                .build();
        //websocket isteği için bir request oluşturuyoruz
        //ws://10.0.2.2:8000/ws/ -> websocket protokolu+suncuu adresi
        //userId url'ye ekleniyor böylece sunucu hangi kullanıcı bağlandığını biliyor
        // build() request nesnesi tamamlanıyor
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            //client.newWebSocket(...) → WebSocket bağlantısını başlatıyor
            //İkinci parametre olarak WebSocketListener veriyoruz → olayları dinleyecek
            @Override
            public void onOpen(WebSocket webSocket,Response response){
                Log.d("WEBSOCKET","Bağlantı açıldı");
            }
            //onOpen() → WebSocket bağlantısı açıldığında çağrılır
            //Log’a yazıyoruz: bağlantı açıldı

            @Override
            public void onMessage(WebSocket webSocket,String text){
                Log.d("WEBSOCKET", "Sunucudan mesaj: " + text);
                // ----  EKLENDİ: Activity’ye mesaj ilet ----
                if (listener != null) {
                    listener.onMessage(text);
                }
            }
            //onMessage() → Sunucudan mesaj geldiğinde çağrılır
            //text → Sunucudan gelen mesajın içeriği (örneğin: “Sorular hazır! file_uuid: …”)
            //Eğer UI’ı güncelleyeceksen runOnUiThread() içinde yapmalısın
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("WEBSOCKET", "Kapanıyor: " + reason);
            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WEBSOCKET", "Bağlantı kapandı: " + reason);
            }
            //onClosed() → WebSocket bağlantısı kapandığında çağrılır
            //code ve reason → kapanma kodu ve sebebi
            //Log ile bildiriyoruz

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WEBSOCKET", "Hata: " + t.getMessage());
            }
            //onFailure() → Bağlantı sırasında hata oluşursa çağrılır
            //Throwable t → hata bilgisi
            //Hata Logcat’e yazdırılır

        });

    }
    public void send(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        } else {
            Log.e("WEBSOCKET", "WebSocket bağlı değil, mesaj gönderilemedi");
        }
    }


    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Kullanıcı çıkışı");
        }
    }
    //close() → WebSocket’i kapatmak için kullanılacak
    //1000 → normal kapanış kodu
    //"Kullanıcı çıkışı" → sebep açıklaması
    //Eğer webSocket null değilse, düzgün şekilde kapatılır
}
