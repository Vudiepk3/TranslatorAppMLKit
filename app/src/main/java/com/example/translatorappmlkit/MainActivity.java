package com.example.translatorappmlkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.translatorappmlkit.databinding.ActivityMainBinding;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Translator translator;
    private ActivityMainBinding binding;
    private Map<String, String> languageMap;
    private Map<String, Translator> translatorMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo map chứa các ngôn ngữ và mã tương ứng từ ML Kit
        languageMap = new HashMap<>();
        languageMap.put("English", TranslateLanguage.ENGLISH);
        languageMap.put("German", TranslateLanguage.GERMAN);
        languageMap.put("French", TranslateLanguage.FRENCH);
        languageMap.put("Japanese", TranslateLanguage.JAPANESE);
        languageMap.put("Korean", TranslateLanguage.KOREAN);

        // Khởi tạo map chứa các Translator
        translatorMap = new HashMap<>();

        // Thiết lập Spinner với danh sách tên ngôn ngữ
        setupSpinner(binding.inputLanguageSpinner, languageMap.keySet().toArray(new String[0]));
        setupSpinner(binding.outputLanguageSpinner, languageMap.keySet().toArray(new String[0]));

        // Tải trước các mô hình dịch
        preDownloadModels();

        // Khi nhấn nút Translate
        binding.translateButton.setOnClickListener(v -> {
            // Nếu bàn phím đang hiển thị thì ẩn bàn phím, nếu không thì bỏ qua
            if (isKeyboardVisible()) {
                hideKeyboard();
            }

            String input = binding.inputEditText.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter some text", Toast.LENGTH_SHORT).show();
                return;
            }
            // Lấy mã ngôn ngữ từ Spinner dựa trên tên đã chọn
            String sourceLanguage = languageMap.get(binding.inputLanguageSpinner.getSelectedItem().toString());
            String targetLanguage = languageMap.get(binding.outputLanguageSpinner.getSelectedItem().toString());
            assert sourceLanguage != null;
            performTranslation(input, sourceLanguage, targetLanguage);
        });

        // Khi nhấn nút Clear
        binding.clearButton.setOnClickListener(v -> {
            binding.inputEditText.setText("");
            binding.outputText.setText("");
        });
    }

    // Thiết lập Spinner
    private void setupSpinner(Spinner spinner, String[] languages) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Hàm kiểm tra xem bàn phím có đang hiển thị hay không
    private boolean isKeyboardVisible() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            return imm.isActive(view); // Trả về true nếu bàn phím đang hoạt động cho view hiện tại
        }
        return false;
    }

    // Hàm ẩn bàn phím (chỉ gọi khi cần)
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            // Đặt focus cho layout root sau khi ẩn bàn phím
            binding.getRoot().clearFocus();
        }
    }

    // Tải trước các mô hình dịch
    private void preDownloadModels() {
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        for (String sourceLanguage : languageMap.values()) {
            for (String targetLanguage : languageMap.values()) {
                if (!sourceLanguage.equals(targetLanguage)) {
                    TranslatorOptions options = new TranslatorOptions.Builder()
                            .setSourceLanguage(sourceLanguage)
                            .setTargetLanguage(targetLanguage)
                            .build();
                    Translator translator = Translation.getClient(options);
                    translator.downloadModelIfNeeded(conditions)
                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Model download failed for " + sourceLanguage + " to " + targetLanguage, Toast.LENGTH_SHORT).show());
                    translatorMap.put(sourceLanguage + "_" + targetLanguage, translator);
                }
            }
        }
    }

    // Phương thức điều phối quá trình dịch
    private void performTranslation(String inputText, String sourceLanguage, String targetLanguage) {
        binding.progressBar.setVisibility(View.VISIBLE);
        // Nếu một trong hai ngôn ngữ là tiếng Anh thì dịch trực tiếp
        if (sourceLanguage.equals(TranslateLanguage.ENGLISH) || targetLanguage.equals(TranslateLanguage.ENGLISH)) {
            directTranslate(inputText, sourceLanguage, targetLanguage);
        } else {
            // Nếu cả hai ngôn ngữ đều không phải tiếng Anh, thực hiện dịch qua tiếng Anh trung gian
            chainTranslate(inputText, sourceLanguage, targetLanguage);
        }
    }

    // Dịch trực tiếp khi một trong các ngôn ngữ là tiếng Anh
    @SuppressLint("SetTextI18n")
    private void directTranslate(String inputText, String sourceLanguage, String targetLanguage) {
        Translator translator = translatorMap.get(sourceLanguage + "_" + targetLanguage);
        if (translator == null) {
            Toast.makeText(MainActivity.this, "Translator not initialized", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        translator.translate(inputText)
                .addOnSuccessListener(result -> {
                    binding.outputText.setText(result);
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.outputText.setText("Error");
                    Toast.makeText(MainActivity.this, "Translation failed", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    // Dịch qua tiếng Anh trung gian: từ nguồn -> English -> đích
    @SuppressLint("SetTextI18n")
    private void chainTranslate(String inputText, String sourceLanguage, String targetLanguage) {
        Translator translator1 = translatorMap.get(sourceLanguage + "_" + TranslateLanguage.ENGLISH);
        Translator translator2 = translatorMap.get(TranslateLanguage.ENGLISH + "_" + targetLanguage);

        if (translator1 == null || translator2 == null) {
            Toast.makeText(MainActivity.this, "Translator not initialized", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        translator1.translate(inputText)
                .addOnSuccessListener(intermediateText -> translator2.translate(intermediateText)
                        .addOnSuccessListener(finalText -> {
                            binding.outputText.setText(finalText);
                            binding.progressBar.setVisibility(View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            binding.outputText.setText("Error");
                            Toast.makeText(MainActivity.this, "Second translation failed", Toast.LENGTH_SHORT).show();
                            binding.progressBar.setVisibility(View.GONE);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "First translation failed", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
    }
}