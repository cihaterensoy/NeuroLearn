# NeuroLearn - PDF’ten Soru Üreten Yapay Zeka Uygulaması

NeuroLearn, eğitim materyallerinizi (PDF) otomatik olarak interaktif testlere dönüştüren, yapay zeka destekli bir öğrenme platformudur. Kullanıcılar PDF dosyalarını yükler ve sistem, içeriği analiz ederek çoktan seçmeli ve boşluk doldurma soruları hazırlar.

## 🚀 Özellikler

- **Yapay Zeka ile Soru Üretimi:** Google Gemini API kullanarak metinden anlamlı ve zorluk seviyesi dengeli sorular üretir.
- **Gelişmiş PDF İşleme:** Metin tabanlı PDF'lerden doğrudan veri çekimi ve taranmış (resim) PDF'ler için OCR (Optik Karakter Tanıma) desteği.
- **Gerçek Zamanlı Takip:** WebSocket teknolojisi ile PDF işleme aşamalarını (Yükleme -> Metin Çıkarma -> Soru Oluşturma) anlık olarak izleme.
- **Mobil Deneyim:** Android platformu üzerinden kolay PDF yükleme, geçmiş testleri görüntüleme ve başarı istatistiklerini takip etme.
- **İstatistik ve Takip:** Toplam soru çözme sayısı, başarı oranı ve "streak" (seri) takibi ile motivasyon desteği.

## 🛠️ Teknoloji Yığını

### Backend (API)
- **Framework:** FastAPI (Python)
- **AI/LLM:** Google Gemini API (genai)
- **PDF & OCR:** PyPDF2, Pytesseract, PDF2Image
- **Asenkron İşlemler:** `asyncio`, `aiofiles`
- **Haberleşme:** WebSocket & REST API

### Frontend (Mobil)
- **Platform:** Android (Java)
- **Ağ Yönetimi:** Retrofit & OkHttp
- **Veritabanı:** SQLite (Local History)
- **UI:** Material Design, CardView, RecyclerView

## 📂 Proje Yapısı

```text
NeuroLearn/
├── API/                    # FastAPI Backend Sunucusu
│   ├── main.py             # API uç noktaları ve WebSocket yönetimi
│   ├── pdf_islemek.py      # PDF okuma ve OCR mantığı
│   ├── llme_gonder.py      # Gemini API entegrasyonu
│   └── uploads/            # Geçici yüklenen PDF dosyaları
├── app/                    # Android Uygulaması Kaynak Kodları
│   ├── src/main/java/...   # Java sınıfları (Activity, Manager, Service)
│   └── src/main/res/       # Layout ve kaynak dosyaları
└── README.md
```

## ⚙️ Kurulum ve Çalıştırma

### Backend Kurulumu
1. Gerekli kütüphaneleri yükleyin:
   ```bash
   pip install fastapi uvicorn google-genai PyPDF2 pytesseract pdf2image aiofiles
   ```
2. Tesseract OCR'ı sisteminize kurun.
3. `llme_gonder.py` dosyasındaki `api_key` alanına Google Gemini API anahtarınızı girin.
4. Sunucuyu başlatın:
   ```bash
   uvicorn main:app --reload
   ```

### Mobil Uygulama Kurulumu
1. Projeyi Android Studio ile açın.
2. `ApiService.java` veya ilgili bağlantı sınıfından `BASE_URL` adresini kendi sunucu adresinizle güncelleyin.
3. Projeyi derleyin ve bir Android cihazda çalıştırın.

## 📝 Kullanım

1. Uygulamayı açın ve ana ekrandaki **"PDF Yükle"** kartına dokunun.
2. Cihazınızdan bir ders notu veya döküman (PDF) seçin.
3. Arka planda yapay zekanın soruları oluşturmasını bekleyin (WebSocket ile ilerlemeyi görebilirsiniz).
4. Oluşturulan soruları çözmeye başlayın!
5. İstatistiklerim sayfasından gelişiminizi takip edin.

---
**Geliştirici:** Cihat Erensoy
