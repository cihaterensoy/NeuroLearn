package com.example.neuralearn.adapter;
// Bu dosyanın proje içindeki konumunu (paketini) belirtir.
// Kodun düzenli olmasını sağlar.

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
// Gson: JSON verilerini Java nesnelerine dönüştürmek için kullanılır.
// GsonBuilder: Gson'u özelleştirmek (örneğin esnek hale getirmek) için kullanılır.

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
// Retrofit: Sunucu (API) ile haberleşmeyi kolaylaştıran bir kütüphane.
// GsonConverterFactory: Retrofit’in Gson ile çalışmasını sağlar (JSON dönüşümü yapar).
public class ApiClient {
    // Bu sınıf (ApiClient), Retrofit bağlantısını oluşturmak ve yönetmek için kullanılır.
    private static final String BASE_URL = "http://10.0.2.2:5168";
    // Sunucunun (API'nin) temel adresi (base URL).
    // "10.0.2.2" Android emülatöründe bilgisayarını temsil eder (localhost gibi çalışır)
    public static Retrofit retrofit;
    // Retrofit nesnesini saklamak için oluşturulan değişken.
    // "static" olduğu için programın her yerinden ulaşılabilir.
    public static Retrofit getRetrofitInstance() {
        // Bu metod, Retrofit nesnesini oluşturur veya var olanı döndürür.
        // Böylece uygulama içinde her yerden aynı bağlantı kullanılabilir.
        if (retrofit == null) {
            // Eğer Retrofit daha önce oluşturulmadıysa (ilk defa çalışıyorsa),
            // yeni bir Retrofit nesnesi oluşturulur.
            Gson gson = new GsonBuilder().setLenient().create();
            // Gson nesnesi oluşturuluyor.
            // setLenient(): JSON verisinde küçük hatalar olsa bile tolere etmesini sağlar.
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // Retrofit'e hangi sunucuya bağlanacağını söylüyoruz.
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    // Retrofit'e JSON verilerini Gson kullanarak dönüştürmesini söylüyoruz.
                    .build();
            // Yukarıdaki ayarlarla Retrofit nesnesini oluşturur.
        }
        return retrofit;
        // Oluşturulan (veya önceden var olan) Retrofit nesnesini döndürür.
    }
}
