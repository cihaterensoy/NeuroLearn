import asyncio  # Asenkron işlemler için kullanılır (örneğin arka planda görev başlatmak)
from fastapi import FastAPI, File, UploadFile, Form, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse  # JSON formatında yanıt döndürmek için
import aiofiles  # Asenkron dosya işlemleri (dosya okuma/yazma) için
import os  # Dosya yolları ve dizin işlemleri için
import uuid  # Dosya isimlerini benzersiz hale getirmek için
import logging  # Loglama işlemleri (hata, bilgi vs.)
import json  # JSON verilerini oluşturmak veya okumak için
from typing import Dict  # WebSocket bağlantılarını user_id'ye göre saklamak için tip belirleme
from pdf_islemek import pdf_oku  # PDF’ten metin çıkarma fonksiyonu (senin yazdığın)
from llme_gonder import soru_olustur  # LLM’e metin gönderip soru oluşturan fonksiyon (senin yazdığın)

#FASTAPI UYGULAMASI VE LOG AYARLARI
app = FastAPI(title="Secure PDF Upload API")  # API başlığıyla FastAPI uygulaması oluşturuluyor

# Dosyaların yükleneceği dizin (uploads klasörü)
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)  # Klasör yoksa oluştur

# Log ayarları — hata ve bilgi mesajlarını upload_log.txt dosyasına kaydeder
logging.basicConfig(
    filename="upload_log.txt",
    level=logging.INFO,  # INFO, WARNING, ERROR gibi seviyeleri loglar
    format="%(asctime)s - %(levelname)s - %(message)s"
)

# Bağlı olan WebSocket istemcilerini (kullanıcıları) saklamak için sözlük
active_connections: Dict[str, WebSocket] = {}


# WEBSOCKET BAĞLANTISI (KULLANICI DİNLEME)
@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    """
    Her kullanıcı kendi user_id’siyle bağlanır.
    Bu fonksiyon bağlantıyı kabul eder ve bağlantıyı açık tutar.
    """
    await websocket.accept()  # WebSocket bağlantısını kabul et
    active_connections[user_id] = websocket  # Kullanıcı bağlantısını sakla
    logging.info(f"WebSocket bağlantısı açıldı: {user_id}")

    try:
        while True:
            # Bağlantıyı canlı tutmak için istemciden mesaj beklenir
            data = await websocket.receive_text()
            logging.info(f"Client'tan gelen mesaj ({user_id}): {data}")

    except WebSocketDisconnect:
        # Kullanıcı bağlantıyı kapatırsa listeden çıkar
        if user_id in active_connections:
            del active_connections[user_id]
        logging.info(f"WebSocket bağlantısı kapandı: {user_id}")


#WEBSOCKET MESAJ GÖNDERME FONKSİYONU
async def send_websocket_message(user_id: str, data: dict):
    """
    Belirli bir kullanıcıya WebSocket üzerinden JSON mesaj gönderir.
    """
    if user_id in active_connections:  # Eğer kullanıcı bağlıysa
        try:
            # JSON veriyi stringe çevir
            json_str = json.dumps(data, ensure_ascii=False)
            # Mesajı WebSocket bağlantısına gönder
            await active_connections[user_id].send_text(json_str)
            logging.info(f"WebSocket mesajı gönderildi: {user_id} - {data['status']}")
        except Exception as e:
            # Hata olursa logla ve bağlantıyı sil
            logging.error(f"WebSocket gönderim hatası ({user_id}): {str(e)}")
            if user_id in active_connections:
                del active_connections[user_id]
    else:
        # Eğer kullanıcı bağlı değilse uyarı logu
        logging.warning(f"WebSocket bağlantısı bulunamadı: {user_id}")


#YARDIMCI FONKSİYONLAR
def secure_filename(filename: str) -> str:

    base_name = os.path.basename(filename)
    unique_id = uuid.uuid4().hex  # Rastgele benzersiz ID
    safe_name = f"{unique_id}_{base_name}"
    return safe_name


async def is_pdf(file: UploadFile) -> bool:
    """
    Dosyanın PDF olup olmadığını dosya başlığından (magic number) kontrol eder.
    PDF dosyaları '%PDF' ile başlar.
    """
    header = await file.read(4)  # İlk 4 baytı oku
    await file.seek(0)  # Okuma pozisyonunu sıfırla (dosya başına dön)
    return header.startswith(b"%PDF")  # PDF imzası kontrolü


# ANA ENDPOINT — PDF YÜKLEME
@app.post("/upload")
async def upload_pdf(
        file: UploadFile = File(...),      # Kullanıcının gönderdiği dosya
        description: str = Form(...),      # PDF açıklaması
        user_id: str = Form(...)           # Kullanıcı kimliği
):


    # İçerik tipi kontrolü (yalnızca PDF izin verilir)
    if file.content_type != "application/pdf":
        raise HTTPException(status_code=400, detail="Sadece PDF dosyaları yüklenebilir.")

    # Gerçek PDF olup olmadığını bayt düzeyinde kontrol et
    if not await is_pdf(file):
        raise HTTPException(status_code=400, detail="Dosya PDF formatında değil.")

    # Dosya adını güvenli hale getir
    safe_name = secure_filename(file.filename)
    file_path = os.path.join(UPLOAD_DIR, safe_name)

    try:
        # Asenkron olarak dosyayı kaydet
        async with aiofiles.open(file_path, 'wb') as out_file:
            while True:
                chunk = await file.read(1024 * 1024)  # 1 MB'lık parçalar halinde oku
                if not chunk:
                    break
                await out_file.write(chunk)

        logging.info(f"Yükleme başarılı: {safe_name} - Açıklama: {description}")

        # WebSocket üzerinden ilk mesajı gönder (bilgi mesajı)
        await send_websocket_message(user_id, {
            "status": "uploaded",
            "message": "PDF başarıyla yüklendi, işlem başlatılıyor...",
            "filename": safe_name
        })

        # Arka planda PDF işleme görevini başlat
        asyncio.create_task(process_pdf_background(file_path, safe_name, user_id, description))

        # Kullanıcıya anında HTTP yanıtı dön (arka plan işlemi devam eder)
        return JSONResponse(
            status_code=200,
            content={
                "success": True,
                "message": "PDF başarıyla yüklendi. Sorular arka planda oluşturuluyor...",
                "filename": safe_name,
                "original_filename": file.filename,
                "description": description,
                "status": "processing"
            }
        )

    except Exception as e:
        # Herhangi bir hata olursa logla ve WebSocket’e hata mesajı gönder
        logging.error(f"Yükleme hatası ({file.filename}): {str(e)}")

        await send_websocket_message(user_id, {
            "status": "error",
            "message": f"Yükleme hatası: {str(e)}"
        })

        raise HTTPException(status_code=500, detail="Sunucu hatası. Lütfen tekrar deneyin.")


#ARKA PLAN İŞLEMİ — PDF İŞLEME VE SORU OLUŞTURMA
async def process_pdf_background(file_path: str, safe_name: str, user_id: str, description: str):
    """
    PDF içeriğini okur, metin çıkarır ve yapay zekadan sorular üretir.
    Sonuç WebSocket üzerinden kullanıcıya gönderilir.
    """
    try:
        logging.info(f"Arka plan işlemi başladı: {safe_name} için {user_id}")

        # 1. Kullanıcıya PDF işleniyor mesajı gönder
        await send_websocket_message(user_id, {
            "status": "processing",
            "message": "PDF işleniyor...",
            "filename": safe_name
        })

        # 2. Metin çıkarma aşaması bildir
        await send_websocket_message(user_id, {
            "status": "extracting",
            "message": "PDF'den metin çıkarılıyor...",
            "filename": safe_name
        })

        # PDF içeriğini oku (pdf_oku fonksiyonu senin tarafında tanımlı)
        text = await asyncio.to_thread(pdf_oku, file_path)

        if not text or len(text.strip()) < 10:
            raise Exception("PDF'den yeterli metin çıkarılamadı")

        # 3. Yapay zeka ile soru oluşturma süreci
        await send_websocket_message(user_id, {
            "status": "generating",
            "message": "Yapay zeka soruları oluşturuyor...",
            "filename": safe_name
        })

        # Gerçekte burada LLM fonksiyonu çağrılacak
        sorular_json = await asyncio.to_thread(soru_olustur, text)
        #sorular_json = "text"  # Şimdilik test amacıyla düz metin

        # JSON formatına dönüştürme denemesi
        try:
            sorular_data = json.loads(sorular_json)
        except json.JSONDecodeError as e:
            # JSON düzgün değilse örnek sorular oluştur (fallback)
            logging.error(f"JSON parse hatası: {e}")
            sorular_data = {
            "multiple_choice": [
      {
                  "question": "Bu bir örnek sorudur: Hukukun temel amaçlarından biri aşağıdakilerden hangisidir?",
                  "options": [
                      "A) Toplumda düzeni sağlamak",
                      "B) Ekonomik krizleri çözmek",
                      "C) Bilimsel araştırmaları desteklemek",
                      "D) Teknolojik gelişmeleri hızlandırmak"
                  ],
                  "correct_answer": "A) Toplumda düzeni sağlamak"
              },
              {
                  "question": "Bu bir örnek sorudur: Aşağıdakilerden hangisi hukukun yazılı kaynaklarından değildir?",
                  "options": [
                      "A) Kanun",
                      "B) Yönetmelik",
                      "C) Tüzük",
                      "D) Gelenek"
                  ],
                  "correct_answer": "D) Gelenek"
              },
              {
                  "question": "Bu bir örnek sorudur: Ahlak kurallarını diğer sosyal düzen kurallarından ayıran temel özellik nedir?",
                  "options": [
                      "A) Vicdani baskı oluşturması",
                      "B) Devlet tarafından zorla uygulanması",
                      "C) Maddi yaptırım içermesi",
                      "D) Mahkeme tarafından belirlenmesi"
                  ],
                  "correct_answer": "A) Vicdani baskı oluşturması"
              }
          ],
          "fill_blank": [
              {
                  "question": "Bu bir örnek sorudur: Hukuk kuralları devletin ___ gücüyle desteklenir.",
                  "options": ["zorlayıcı", "manevi", "yönlendirici", "teorik"],
                  "correct_answer": "zorlayıcı"
              },
              {
                  "question": "Bu bir örnek sorudur: Yazısız hukuk kaynaklarına ___ hukuku denir.",
                  "options": ["örf ve adet", "anayasa", "idare", "ticaret"],
                  "correct_answer": "örf ve adet"
              },
              {
                  "question": "Bu bir örnek sorudur: Kanunlar genellikle yürürlüğe girdikten sonraki olaylara uygulanır ve ___ etkili olmaz.",
                  "options": ["geniş", "geleceğe", "geçmişe", "kişiye"],
                  "correct_answer": "geçmişe"
              }
          ]
      }



        # Oluşturulan soruları JSON dosyası olarak kaydet
        JSON_DIR = "json"
        os.makedirs(JSON_DIR, exist_ok=True)
        json_filename = f"{os.path.splitext(safe_name)[0]}_sorular.json"
        json_path = os.path.join(JSON_DIR, json_filename)

        async with aiofiles.open(json_path, 'w', encoding='utf-8') as json_file:
            await json_file.write(json.dumps(sorular_data, ensure_ascii=False, indent=2))

        #  Başarılı sonucu kullanıcıya bildir
        await send_websocket_message(user_id, {
            "status": "completed",
            "message": "Sorular başarıyla oluşturuldu!",
            "filename": safe_name,
            "json_filename": json_filename,
            "questions": sorular_data  # Sorular doğrudan gönderilir
        })

        logging.info(f"İşlem tamamlandı: {safe_name}")

    except Exception as e:
        # Herhangi bir hata olursa kullanıcıya hata mesajı gönder
        logging.error(f"Arka plan işlem hatası ({safe_name}): {str(e)}")
        await send_websocket_message(user_id, {
            "status": "error",
            "message": f"İşlem başarısız: {str(e)}",
            "filename": safe_name
        })


# KONTROL ENDPOINTLERİ
@app.get("/")
async def root():
    """
    Ana sayfa endpoint'i — API'nin çalıştığını doğrulamak için.
    """
    return {"message": "PDF Soru Üretici API Çalışıyor", "status": "active"}


@app.get("/health")
async def health_check():
    """
    Sistem durumu kontrolü:
    - Sunucu sağlıklı mı?
    - Aktif WebSocket bağlantısı sayısı
    - Upload klasörü var mı?
    """
    return {
        "status": "healthy",
        "active_connections": len(active_connections),
        "upload_dir_exists": os.path.exists(UPLOAD_DIR)
    }
