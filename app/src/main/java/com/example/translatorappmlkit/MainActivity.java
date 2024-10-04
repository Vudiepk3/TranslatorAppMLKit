package com.example.translatorappmlkit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class MainActivity extends AppCompatActivity {
    // Biến lưu trữ Translator cho tiếng Anh - Đức
    private Translator englishGermanTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo các thành phần giao diện người dùng
        EditText inputEditText = findViewById(R.id.inputEditText);
        Button translateButton = findViewById(R.id.translateButton);
        TextView outputText = findViewById(R.id.outputText);

        // Thiết lập tùy chọn cho Translator (ngôn ngữ nguồn và ngôn ngữ đích)
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH) // Ngôn ngữ nguồn là tiếng Anh
                        .setTargetLanguage(TranslateLanguage.GERMAN)  // Ngôn ngữ đích là tiếng Đức
                        .build();

        // Khởi tạo Translator
        englishGermanTranslator = Translation.getClient(options);

        // Thiết lập điều kiện tải mô hình (chỉ tải khi có kết nối Wi-Fi)
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        // Tải mô hình nếu cần thiết
        englishGermanTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Khi mô hình đã tải thành công, thiết lập sự kiện click cho nút dịch
                        translateButton.setOnClickListener(v -> {
                            String input = inputEditText.getText().toString(); // Lấy văn bản đầu vào
                            translateText(input, outputText); // Gọi phương thức dịch
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Hiển thị thông báo lỗi nếu tải mô hình thất bại
                        Toast.makeText(MainActivity.this, "Model download failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Phương thức để dịch văn bản
    public void translateText(String inputText, TextView outputText) {
        // Gọi phương thức dịch của Translator
        englishGermanTranslator.translate(inputText)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // Cập nhật văn bản đã dịch lên TextView
                        outputText.setText(translatedText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Hiển thị thông báo lỗi nếu dịch thất bại
                        outputText.setText("Error");
                        Toast.makeText(MainActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
