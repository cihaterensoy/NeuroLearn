from google import genai
import json
import logging
import re

client = genai.Client(api_key="")


def soru_olustur(text: str) -> str:
    try:
        # Metni kısalt (token sınırı için)
        if len(text) > 3000:
            text = text[:3000] + "..."
            logging.info("Metin 3000 karaktere kısaltıldı")

        prompt = f"""
        Aşağıdaki metinden 10 çoktan seçmeli ve 10 boşluk doldurma sorusu hazırla.

        ÇOKTAN SEÇMELİ FORMAT:
        - Her soru için 4 seçenek (A, B, C, D)
        - Sadece 1 doğru cevap
        - Seçenekler mantıklı ve birbirine benzer olmalı

        BOŞLUK DOLDURMA FORMAT:
        - Cümlede boşluğu "___" ile göster
        - Doğru cevabı ver
        - 4 yanlış seçenek ekle

        ÇIKTI FORMATI (JSON):
        {{
            "multiple_choice": [
                {{
                    "question": "soru metni",
                    "options": ["A seçeneği", "B seçeneği", "C seçeneği", "D seçeneği"],
                    "correct_answer": "A seçeneği"
                }}
            ],
            "fill_blank": [
                {{
                    "question": "Cümle ___ boşluk var",
                    "correct_answer": "doğrucevap",
                    "options": ["yanlış1", "yanlış2", "yanlış3", "yanlış4"]
                }}
            ]
        }}

        Lütfen sadece JSON formatında cevap ver, başka hiçbir şey yazma.

        METİN:
        {text}
        """

        logging.info("Gemini API'ye istek gönderiliyor...")

        response = client.models.generate_content(
            model="gemini-2.5-flash",  # Daha stabil model
            contents=[{"role": "user", "parts": [{"text": prompt}]}]
        )

        result_text = response.text.strip()
        logging.info(f"API Yanıtı: {result_text[:200]}...")  # İlk 200 karakteri logla

        # JSON temizleme
        result_text = clean_json_response(result_text)

        # JSON validasyonu
        questions_data = json.loads(result_text)

        # Minimum soru kontrolü
        if (len(questions_data.get("multiple_choice", [])) < 2 or
                len(questions_data.get("fill_blank", [])) < 2):
            logging.warning("Yeterli soru oluşturulamadı, fallback kullanılıyor")
            return create_fallback_questions()

        logging.info(
            f"Başarıyla oluşturulan sorular: {len(questions_data['multiple_choice'])} çoktan seçmeli, {len(questions_data['fill_blank'])} boşluk doldurma")

        return json.dumps(questions_data, ensure_ascii=False)

    except Exception as e:
        logging.error(f"LLM hatası: {str(e)}")
        return create_fallback_questions()


def clean_json_response(text: str) -> str:
    # ```json ... ``` formatını temizle
    if "```json" in text:
        text = re.sub(r'```json\s*', '', text)
        text = re.sub(r'\s*```', '', text)
    elif "```" in text:
        text = re.sub(r'```\s*', '', text)
        text = re.sub(r'\s*```', '', text)

    # Baştaki ve sondaki boşlukları temizle
    text = text.strip()

    # Eğer hala JSON başlamıyorsa, { işaretini ara
    if not text.startswith('{'):
        start_index = text.find('{')
        if start_index != -1:
            text = text[start_index:]

    # Sonunda fazlalık varsa kes
    end_index = text.rfind('}')
    if end_index != -1:
        text = text[:end_index + 1]

    return text


def create_fallback_questions() -> str:
    fallback_questions = {
        "multiple_choice": [
            {
                "question": "Aşağıdakilerden hangisi bir programlama dilidir?",
                "options": ["Python", "Excel", "Word", "Photoshop"],
                "correct_answer": "Python"
            },
            {
                "question": "HTML'in açılımı nedir?",
                "options": [
                    "Hyper Text Markup Language",
                    "High Tech Modern Language",
                    "Home Tool Markup Language",
                    "Hyper Transfer Markup Language"
                ],
                "correct_answer": "Hyper Text Markup Language"
            },
            {
                "question": "Aşağıdakilerden hangisi bir veritabanı yönetim sistemidir?",
                "options": ["MySQL", "Java", "HTML", "CSS"],
                "correct_answer": "MySQL"
            }
        ],
        "fill_blank": [
            {
                "question": "Türkiye'nin başkenti ___'dir.",
                "correct_answer": "Ankara",
                "options": ["İstanbul", "İzmir", "Bursa", "Antalya"]
            },
            {
                "question": "Güneş sistemindeki en büyük gezegen ___'dir.",
                "correct_answer": "Jüpiter",
                "options": ["Mars", "Dünya", "Venüs", "Satürn"]
            },
            {
                "question": "Python ___ programlama dilidir.",
                "correct_answer": "yüksek seviyeli",
                "options": ["düşük seviyeli", "makine", "donanım", "binary"]
            }
        ]
    }

    logging.info("Fallback sorular kullanılıyor")
    return json.dumps(fallback_questions, ensure_ascii=False, indent=2)

