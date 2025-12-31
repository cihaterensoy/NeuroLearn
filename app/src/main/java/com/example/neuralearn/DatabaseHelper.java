package com.example.neuralearn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "QuestionsDatabase.db";
    private static final int DATABASE_VERSION = 1;

    // Tablo isimleri
    private static final String TABLE_QUIZ_SESSIONS = "quiz_sessions";
    private static final String TABLE_QUESTIONS = "questions";
    private static final String TABLE_OPTIONS = "options";

    // quiz_sessions tablosu sütunları
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_FILENAME = "filename";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_TOTAL_QUESTIONS = "total_questions";
    private static final String KEY_CORRECT_ANSWERS = "correct_answers";

    // questions tablosu sütunları
    private static final String KEY_QUESTION_ID = "question_id";
    private static final String KEY_QUESTION_TEXT = "question_text";
    private static final String KEY_QUESTION_TYPE = "question_type";
    private static final String KEY_CORRECT_ANSWER = "correct_answer";
    private static final String KEY_USER_ANSWER = "user_answer";
    private static final String KEY_IS_CORRECT = "is_correct";

    // options tablosu sütunları
    private static final String KEY_OPTION_ID = "option_id";
    private static final String KEY_OPTION_TEXT = "option_text";
    private static final String KEY_IS_CORRECT_OPTION = "is_correct_option";

    // Tablo oluşturma SQL'leri
    private static final String CREATE_TABLE_SESSIONS = "CREATE TABLE " + TABLE_QUIZ_SESSIONS + "("
            + KEY_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_FILENAME + " TEXT,"
            + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_TOTAL_QUESTIONS + " INTEGER,"
            + KEY_CORRECT_ANSWERS + " INTEGER"
            + ")";

    private static final String CREATE_TABLE_QUESTIONS = "CREATE TABLE " + TABLE_QUESTIONS + "("
            + KEY_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_SESSION_ID + " INTEGER,"
            + KEY_QUESTION_TEXT + " TEXT,"
            + KEY_QUESTION_TYPE + " TEXT,"
            + KEY_CORRECT_ANSWER + " TEXT,"
            + KEY_USER_ANSWER + " TEXT,"
            + KEY_IS_CORRECT + " INTEGER DEFAULT 0,"
            + "FOREIGN KEY(" + KEY_SESSION_ID + ") REFERENCES " + TABLE_QUIZ_SESSIONS + "(" + KEY_SESSION_ID + ")"
            + ")";

    private static final String CREATE_TABLE_OPTIONS = "CREATE TABLE " + TABLE_OPTIONS + "("
            + KEY_OPTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_QUESTION_ID + " INTEGER,"
            + KEY_OPTION_TEXT + " TEXT,"
            + KEY_IS_CORRECT_OPTION + " INTEGER DEFAULT 0,"
            + "FOREIGN KEY(" + KEY_QUESTION_ID + ") REFERENCES " + TABLE_QUESTIONS + "(" + KEY_QUESTION_ID + ")"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SESSIONS);
        db.execSQL(CREATE_TABLE_QUESTIONS);
        db.execSQL(CREATE_TABLE_OPTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OPTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_SESSIONS);
        onCreate(db);
    }

    // Yeni quiz oturumu oluştur
    public long createQuizSession(String filename) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_FILENAME, filename);

            long sessionId = db.insert(TABLE_QUIZ_SESSIONS, null, values);
            Log.d("DB_HELPER", "Yeni oturum oluşturuldu: " + sessionId + " - " + filename);
            return sessionId;
        } catch (Exception e) {
            Log.e("DB_HELPER", "Oturum oluşturma hatası: " + e.getMessage());
            return -1;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // Soruları ve seçenekleri veritabanına kaydet
    public boolean saveQuestions(long sessionId, String questionsJson) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.beginTransaction();

            JSONObject json = new JSONObject(questionsJson);

            // Çoktan seçmeli soruları kaydet
            if (json.has("multiple_choice")) {
                JSONArray mcArray = json.getJSONArray("multiple_choice");
                for (int i = 0; i < mcArray.length(); i++) {
                    JSONObject questionObj = mcArray.getJSONObject(i);
                    saveMultipleChoiceQuestion(db, sessionId, questionObj);
                }
            }

            // Boşluk doldurma sorularını kaydet
            if (json.has("fill_blank")) {
                JSONArray fbArray = json.getJSONArray("fill_blank");
                for (int i = 0; i < fbArray.length(); i++) {
                    JSONObject questionObj = fbArray.getJSONObject(i);
                    saveFillBlankQuestion(db, sessionId, questionObj);
                }
            }

            db.setTransactionSuccessful();
            Log.d("DB_HELPER", "Tüm sorular veritabanına kaydedildi. Oturum: " + sessionId);
            return true;

        } catch (JSONException e) {
            Log.e("DB_HELPER", "JSON parse hatası: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e("DB_HELPER", "Soru kaydetme hatası: " + e.getMessage());
            return false;
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    private long saveMultipleChoiceQuestion(@NonNull SQLiteDatabase db, long sessionId,
                                            @NonNull JSONObject questionObj) throws JSONException {
        // Soruyu kaydet
        ContentValues questionValues = new ContentValues();
        questionValues.put(KEY_SESSION_ID, sessionId);
        questionValues.put(KEY_QUESTION_TEXT, questionObj.getString("question"));
        questionValues.put(KEY_QUESTION_TYPE, "multiple_choice");
        questionValues.put(KEY_CORRECT_ANSWER, questionObj.getString("correct_answer"));

        long questionId = db.insert(TABLE_QUESTIONS, null, questionValues);

        // Seçenekleri kaydet
        JSONArray optionsArray = questionObj.getJSONArray("options");
        String correctAnswer = questionObj.getString("correct_answer");

        for (int j = 0; j < optionsArray.length(); j++) {
            String optionText = optionsArray.getString(j);
            ContentValues optionValues = new ContentValues();
            optionValues.put(KEY_QUESTION_ID, questionId);
            optionValues.put(KEY_OPTION_TEXT, optionText);
            optionValues.put(KEY_IS_CORRECT_OPTION, optionText.equals(correctAnswer) ? 1 : 0);

            db.insert(TABLE_OPTIONS, null, optionValues);
        }

        return questionId;
    }

    private long saveFillBlankQuestion(@NonNull SQLiteDatabase db, long sessionId,
                                       @NonNull JSONObject questionObj) throws JSONException {
        // Soruyu kaydet
        ContentValues questionValues = new ContentValues();
        questionValues.put(KEY_SESSION_ID, sessionId);
        questionValues.put(KEY_QUESTION_TEXT, questionObj.getString("question"));
        questionValues.put(KEY_QUESTION_TYPE, "fill_blank");
        questionValues.put(KEY_CORRECT_ANSWER, questionObj.getString("correct_answer"));

        long questionId = db.insert(TABLE_QUESTIONS, null, questionValues);

        // Seçenekleri kaydet (boşluk doldurma için) - GÜVENLİ VERSİYON
        List<String> optionsList = new ArrayList<>();
        String correctAnswer = questionObj.getString("correct_answer");

        // Doğru cevabı ekle
        optionsList.add(correctAnswer);

        // Yanlış seçenekleri ekle (eğer varsa)
        if (questionObj.has("options")) {
            JSONArray optionsArray = questionObj.getJSONArray("options");
            for (int j = 0; j < optionsArray.length(); j++) {
                String option = optionsArray.getString(j);
                if (!option.equals(correctAnswer)) {
                    optionsList.add(option);
                }
            }
        } else {
            // Eğer options yoksa, varsayılan yanlış seçenekler ekle
            // Sorunun içeriğine göre akıllı seçenekler oluşturabiliriz
            if (questionObj.getString("question").toLowerCase().contains("başkenti")) {
                optionsList.add("İstanbul");
                optionsList.add("İzmir");
                optionsList.add("Bursa");
                optionsList.add("Antalya");
            } else if (questionObj.getString("question").toLowerCase().contains("ingilizce") ||
                    questionObj.getString("question").toLowerCase().contains("english")) {
                optionsList.add("is");
                optionsList.add("are");
                optionsList.add("were");
                optionsList.add("was");
            } else {
                // Genel varsayılan seçenekler
                optionsList.add("seçenek1");
                optionsList.add("seçenek2");
                optionsList.add("seçenek3");
                optionsList.add("seçenek4");
            }
        }

        // Tüm seçenekleri veritabanına kaydet
        for (String option : optionsList) {
            ContentValues optionValues = new ContentValues();
            optionValues.put(KEY_QUESTION_ID, questionId);
            optionValues.put(KEY_OPTION_TEXT, option);
            optionValues.put(KEY_IS_CORRECT_OPTION, option.equals(correctAnswer) ? 1 : 0);

            db.insert(TABLE_OPTIONS, null, optionValues);
        }

        return questionId;
    }

    // Kullanıcı cevabını kaydet
    public boolean saveUserAnswer(long questionId, String userAnswer, boolean isCorrect) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ANSWER, userAnswer);
            values.put(KEY_IS_CORRECT, isCorrect ? 1 : 0);

            int rowsAffected = db.update(TABLE_QUESTIONS, values,
                    KEY_QUESTION_ID + " = ?",
                    new String[]{String.valueOf(questionId)});

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("DB_HELPER", "Cevap kaydetme hatası: " + e.getMessage());
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    // Quiz sonuçlarını güncelle
    public boolean updateQuizResults(long sessionId, int totalQuestions, int correctAnswers) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_TOTAL_QUESTIONS, totalQuestions);
            values.put(KEY_CORRECT_ANSWERS, correctAnswers);

            int rowsAffected = db.update(TABLE_QUIZ_SESSIONS, values,
                    KEY_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e("DB_HELPER", "Sonuç güncelleme hatası: " + e.getMessage());
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    // Yeni: Session ID'ye göre soruları JSON formatında getir
    public String getQuestionsJsonBySessionId(long sessionId) {
        try {
            List<Question> questions = getQuestionsForSession(sessionId);
            JSONObject json = new JSONObject();
            JSONArray multipleChoiceArray = new JSONArray();
            JSONArray fillBlankArray = new JSONArray();

            for (Question question : questions) {
                JSONObject questionObj = new JSONObject();
                questionObj.put("question", question.getQuestionText());
                questionObj.put("correct_answer", question.getCorrectAnswer());

                // Seçenekleri ekle
                JSONArray optionsArray = new JSONArray();
                List<String> options = getOptionsForQuestion(Long.parseLong(question.getId()));
                for (String option : options) {
                    optionsArray.put(option);
                }
                questionObj.put("options", optionsArray);

                // Soru tipine göre ilgili diziye ekle
                if ("multiple_choice".equals(question.getQuestionType())) {
                    multipleChoiceArray.put(questionObj);
                } else if ("word_bank".equals(question.getQuestionType()) || "fill_blank".equals(question.getQuestionType())) {
                    fillBlankArray.put(questionObj);
                }
            }

            json.put("multiple_choice", multipleChoiceArray);
            json.put("fill_blank", fillBlankArray);

            return json.toString();

        } catch (Exception e) {
            Log.e("DB_HELPER", "JSON oluşturma hatası: " + e.getMessage());
            return null;
        }
    }
    // DatabaseHelper.java'ya bu metodları ekleyin:
    // DatabaseHelper.java'ya bu metodu ekleyin:
    public boolean resetQuizSession(long sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // 1. Session sonuçlarını sıfırla
            ContentValues sessionValues = new ContentValues();
            sessionValues.put(KEY_TOTAL_QUESTIONS, 0);
            sessionValues.put(KEY_CORRECT_ANSWERS, 0);

            db.update(TABLE_QUIZ_SESSIONS, sessionValues,
                    KEY_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});

            // 2. Tüm soruların kullanıcı cevaplarını ve doğruluk durumlarını sıfırla
            ContentValues questionValues = new ContentValues();
            questionValues.put(KEY_USER_ANSWER, "");
            questionValues.put(KEY_IS_CORRECT, 0);

            db.update(TABLE_QUESTIONS, questionValues,
                    KEY_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});

            db.setTransactionSuccessful();
            Log.d("DB_HELPER", "Session resetlendi: " + sessionId);
            return true;

        } catch (Exception e) {
            Log.e("DB_HELPER", "Session reset hatası: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Quiz oturumunu ve ilişkili tüm verileri sil
    public boolean deleteQuizSession(long sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Transaction başlat - tüm silme işlemleri atomik olsun
            db.beginTransaction();

            // 1. Önce bu session'a ait soruların seçeneklerini sil
            String deleteOptionsSql = "DELETE FROM " + TABLE_OPTIONS +
                    " WHERE " + KEY_QUESTION_ID + " IN (" +
                    "SELECT " + KEY_QUESTION_ID + " FROM " + TABLE_QUESTIONS +
                    " WHERE " + KEY_SESSION_ID + " = ?)";
            db.execSQL(deleteOptionsSql, new String[]{String.valueOf(sessionId)});

            // 2. Sonra bu session'a ait soruları sil
            db.delete(TABLE_QUESTIONS, KEY_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            // 3. En son session'ı sil
            db.delete(TABLE_QUIZ_SESSIONS, KEY_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            // Transaction'ı başarıyla bitir
            db.setTransactionSuccessful();
            Log.d("DB_HELPER", "Quiz session silindi: " + sessionId);
            return true;

        } catch (Exception e) {
            Log.e("DB_HELPER", "Quiz session silme hatası: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Tüm geçmişi temizle
    public boolean clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // Tüm tabloları temizle
            db.delete(TABLE_OPTIONS, null, null);
            db.delete(TABLE_QUESTIONS, null, null);
            db.delete(TABLE_QUIZ_SESSIONS, null, null);

            db.setTransactionSuccessful();
            Log.d("DB_HELPER", "Tüm geçmiş temizlendi");
            return true;

        } catch (Exception e) {
            Log.e("DB_HELPER", "Geçmiş temizleme hatası: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // DatabaseHelper.java içinde getQuizSessions metodunu güncelleyin:
    public List<QuizSession> getQuizSessions() {
        List<QuizSession> sessions = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_QUIZ_SESSIONS + " ORDER BY " + KEY_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                QuizSession session = new QuizSession();
                // getColumnIndex yerine getColumnIndexOrThrow kullanabiliriz
                session.setSessionId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SESSION_ID)));
                session.setFilename(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FILENAME)));
                session.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT)));
                session.setTotalQuestions(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_QUESTIONS)));
                session.setCorrectAnswers(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CORRECT_ANSWERS)));

                sessions.add(session);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return sessions;
    }

    // getQuestionsForSession metodunu güncelleyin:
    public List<Question> getQuestionsForSession(long sessionId) {
        List<Question> questions = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_QUESTIONS +
                " WHERE " + KEY_SESSION_ID + " = " + sessionId;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Question question = new Question();
                question.setId(String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_QUESTION_ID))));
                question.setQuestionText(cursor.getString(cursor.getColumnIndexOrThrow(KEY_QUESTION_TEXT)));
                question.setQuestionType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_QUESTION_TYPE)));
                question.setCorrectAnswer(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CORRECT_ANSWER)));
                question.setUserAnswer(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ANSWER)));

                // Seçenekleri getir
                List<String> options = getOptionsForQuestion(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_QUESTION_ID)));
                question.setOptions(options);

                questions.add(question);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return questions;
    }

    @NonNull
    private List<String> getOptionsForQuestion(long questionId) {
        List<String> options = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT " + KEY_OPTION_TEXT + " FROM " + TABLE_OPTIONS
                    + " WHERE " + KEY_QUESTION_ID + " = ?";

            db = this.getReadableDatabase();
            cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(questionId)});

            if (cursor.moveToFirst()) {
                do {
                    options.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DB_HELPER", "Seçenek getirme hatası: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return options;
    }
}