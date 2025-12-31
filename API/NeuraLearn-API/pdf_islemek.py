import PyPDF2
import pytesseract
from pdf2image import convert_from_path
import io
import os


def pdf_oku(pdf_dosyasi, ocr_dil='tur'):

    text_parts = []

    try:
        # 1. Önce doğrudan metin çıkarmayı dene
        with open(pdf_dosyasi, "rb") as file:
            reader = PyPDF2.PdfReader(file)

            for sayfa_num, sayfa in enumerate(reader.pages):
                try:
                    sayfa_metni = sayfa.extract_text()

                    if sayfa_metni and sayfa_metni.strip():
                        # Metin başarıyla çıkarıldı
                        sayfa_metni = " ".join(sayfa_metni.split())
                        text_parts.append(sayfa_metni)
                    else:
                        # 2. Metin çıkarılamazsa OCR dene
                        ocr_metni = _pdf_sayfasindan_ocr(pdf_dosyasi, sayfa_num, ocr_dil)
                        if ocr_metni and ocr_metni.strip():
                            text_parts.append(ocr_metni)
                        else:
                            text_parts.append(f"[Sayfa {sayfa_num + 1}: İçerik çıkarılamadı]")

                except Exception as e:
                    # Sayfa işleme hatası - diğer sayfalara devam et
                    print(f"Sayfa {sayfa_num + 1} işlenirken hata: {str(e)}")
                    text_parts.append(f"[Sayfa {sayfa_num + 1}: İşlenemedi]")
                    continue

    except Exception as e:
        print(f"PDF açma hatası: {str(e)}")
        return f"[PDF işlenemedi: {str(e)}]"

    return " ".join(text_parts)


def _pdf_sayfasindan_ocr(pdf_dosyasi, sayfa_num, dil='tur'):
    try:
        # OCR kütüphanelerinin mevcut olup olmadığını kontrol et

        # PDF sayfasını resme dönüştür
        images = convert_from_path(
            pdf_dosyasi,
            first_page=sayfa_num + 1,
            last_page=sayfa_num + 1,
            dpi=200  # Düşük çözünürlük için dpi değeri
        )

        if not images:
            return ""

        # OCR uygula
        image = images[0]
        metin = pytesseract.image_to_string(image, lang=dil)

        # Metni temizle
        if metin:
            metin = " ".join(metin.split())

        return metin

    except Exception as e:
        # OCR hatası - sessizce devam et
        return ""
"""
"""
def _tum_pdf_ocr(pdf_dosyasi, dil='tur'):

    try:
        try:
            from pdf2image import convert_from_path
            import pytesseract
        except ImportError:
            print("OCR kütüphaneleri kurulu değil.")
            return ""

        images = convert_from_path(pdf_dosyasi, dpi=200)
        metin_listesi = []

        for i, image in enumerate(images):
            try:
                sayfa_metni = pytesseract.image_to_string(image, lang=dil)
                if sayfa_metni:
                    sayfa_metni = " ".join(sayfa_metni.split())
                    metin_listesi.append(sayfa_metni)
            except Exception:
                # OCR hatası - bu sayfayı atla
                metin_listesi.append(f"[Sayfa {i + 1}: OCR başarısız]")
                continue

        return "\n".join(metin_listesi)

    except Exception as e:
        print(f"Tam OCR hatası: {str(e)}")
        return ""

