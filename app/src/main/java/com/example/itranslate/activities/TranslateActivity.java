package com.example.itranslate.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.itranslate.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.Locale;

public class TranslateActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final int CAMERA_PERMISSION = 1001;
    private SharedPreferences sharedPreferences;
    private String targetLanguageCode, sourceLanguageCode;
    private ProgressBar progressBar;
    private TextView downloadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getApplicationContext().getSharedPreferences("translationModels", 0);

        progressBar = findViewById(R.id.translateProgressBar);
        progressBar.setVisibility(View.GONE);
        downloadingText = findViewById(R.id.downloadingTextView);
        downloadingText.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        targetLanguageCode = sharedPreferences.getString("targetLanguage", Locale.getDefault().getLanguage());
        sourceLanguageCode = sharedPreferences.getString("sourceLanguage", "en");
    }

    @Override
    @Deprecated
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            goToCameraActivity();
        }
    }

    /**
     * Opens the camera if requirements are met
     */
    private void goToCameraActivity() {
        if (sharedPreferences.getBoolean(sourceLanguageCode + "_" + targetLanguageCode, false)) {
            Intent intent = new Intent(TranslateActivity.this, CameraActivity.class);
            startActivity(intent);
        } else {
            downloadTranslationModel();
        }
    }

    /**
     * Downloads the required translation models
     */
    private void downloadTranslationModel() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            return;
        }
        DialogInterface.OnClickListener dialogClickListener = getTranslationModelDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("The required translation model doesn't exist. Do you wish to download it?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    /**
     * Download model dialog
     */
    private DialogInterface.OnClickListener getTranslationModelDialog() {
        return (dialog, which) -> {
            if (which == -1) {
                progressBar.setVisibility(View.VISIBLE);
                downloadingText.setVisibility(View.VISIBLE);
                TranslatorOptions options =
                        new TranslatorOptions.Builder()
                                .setSourceLanguage(sourceLanguageCode)
                                .setTargetLanguage(targetLanguageCode)
                                .build();
                final Translator translator =
                        Translation.getClient(options);

                DownloadConditions conditions = new DownloadConditions.Builder()
                        .requireWifi()
                        .build();
                translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener(
                                (OnSuccessListener) o -> {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean(sourceLanguageCode + "_" + targetLanguageCode, true);
                                    editor.apply();
                                    progressBar.setVisibility(View.GONE);
                                    downloadingText.setVisibility(View.GONE);
                                    Toast.makeText(TranslateActivity.this, R.string.downloadSuccess,
                                            Toast.LENGTH_SHORT).show();
                                })
                        .addOnFailureListener(
                                e -> {
                                    progressBar.setVisibility(View.GONE);
                                    downloadingText.setVisibility(View.GONE);
                                    Toast.makeText(TranslateActivity.this, R.string.downloadFailed,
                                            Toast.LENGTH_SHORT).show();
                                });
            }
        };
    }

    /**
     * Close app on back pressed
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Opens camera and translates text
     *
     * @param view View
     */
    public void translateCamera(View view) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TranslateActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        } else {
            goToCameraActivity();
        }
    }

    /**
     * Log out from Firebase
     *
     * @param view View
     */
    public void logout(View view) {
        mAuth.signOut();
        Intent intent = new Intent(TranslateActivity.this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Go to settings
     *
     * @param view View
     */
    public void goToSettings(View view) {
        Intent intent = new Intent(TranslateActivity.this, SettingsActivity.class);
        startActivity(intent);
    }
}