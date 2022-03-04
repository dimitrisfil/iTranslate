package com.example.itranslate.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.itranslate.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private List<String> languageCodes;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Resources resources = getResources();
        languageCodes = Arrays.asList(resources.getStringArray(R.array.supportedLanguageCodes));

        sharedPreferences = getApplicationContext().getSharedPreferences("translationModels", 0);

        int languageIndexFrom = languageCodes.indexOf(sharedPreferences.getString("sourceLanguage", "en"));
        int languageIndexTo = languageCodes.indexOf(sharedPreferences.getString("targetLanguage", Locale.getDefault().getLanguage()));

        // Spinner settings
        Spinner languageSpinnerFrom = findViewById(R.id.languageSpinnerFrom);
        Spinner languageSpinnerTo = findViewById(R.id.languageSpinnerTo);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.supportedLanguages,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinnerFrom.setAdapter(adapter);
        languageSpinnerTo.setAdapter(adapter);
        // Select the language set in shared preferences
        languageSpinnerFrom.setSelection(languageIndexFrom);
        languageSpinnerTo.setSelection(languageIndexTo);
        languageSpinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) adapterView.getChildAt(0)).setTextColor(Color.WHITE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                // Save language to shared preferences
                editor.putString("sourceLanguage", languageCodes.get(i));
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No use
            }
        });
        languageSpinnerTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) adapterView.getChildAt(0)).setTextColor(Color.WHITE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                // Save language to shared preferences
                editor.putString("targetLanguage", languageCodes.get(i));
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No use
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}