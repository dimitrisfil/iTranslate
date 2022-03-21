package com.example.itranslate.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Size;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.itranslate.R;
import com.example.itranslate.models.TranslationRecord;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import info.debatty.java.stringsimilarity.Levenshtein;

public class CameraActivity extends AppCompatActivity {

    private PreviewView cameraView;
    private ImageAnalysis imageAnalysis;
    private TextRecognizer recognizer;
    private FrameLayout frameLayout;
    private ArrayList<TextView> textBlocks;
    private static final int TEXT_BLOCKS = 10;
    private SharedPreferences sharedPreferences;
    private Translator translator;
    private static final int MINIMUM_BLOCK_SIZE = 500;
    private String sourceLanguage;
    private String targetLanguage;
    private FirebaseAuth mAuth;
    private List<String> recognisedTexts;
    private String userCountry;
    private FirebaseFirestore db;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mAuth = FirebaseAuth.getInstance();

        cameraView = findViewById(R.id.previewView);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        initializeBlocks();
        sharedPreferences = getApplicationContext().getSharedPreferences("translationModels", 0);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
        recognisedTexts = new ArrayList<>();

        Locale locale = new Locale("", getUserCountry(this));
        userCountry = !locale.getDisplayCountry().isEmpty() ? locale.getDisplayCountry() : "Unknown";
        db = FirebaseFirestore.getInstance();
    }

    private void initializeBlocks() {
        frameLayout = findViewById(R.id.frameLayout);
        textBlocks = new ArrayList<>();
        for (int i = 0; i < TEXT_BLOCKS; i++) {
            TextView textView = new TextView(this);
            textView.setTextColor(Color.BLACK);
            frameLayout.addView(textView);
            textBlocks.add(textView);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sourceLanguage = sharedPreferences.getString("sourceLanguage", "en");
        targetLanguage = sharedPreferences.getString("targetLanguage", Locale.getDefault().getLanguage());

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage)
                        .build();
        translator =
                Translation.getClient(options);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();


        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());

        // Image Analysis
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(frameLayout.getWidth(), frameLayout.getHeight()))
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
            @SuppressLint("UnsafeOptInUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                recognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            textBlocks.stream()
                                    .filter(textBlock -> textBlocks.indexOf(textBlock) >= visionText.getTextBlocks().size())
                                    .forEach(textBlock -> {
                                        textBlock.setText("");
                                        textBlock.setBackgroundColor(Color.alpha(0));
                                    });
                            for (Text.TextBlock block : visionText.getTextBlocks()) {
                                String blockText = block.getText();
                                Rect blockFrame = block.getBoundingBox();
                                assert blockFrame != null;
                                int rectSize = (blockFrame.right - blockFrame.left) + (blockFrame.bottom - blockFrame.top);
                                if (rectSize < MINIMUM_BLOCK_SIZE) {
                                    continue;
                                }

                                int index = visionText.getTextBlocks().indexOf(block);
                                if (index < TEXT_BLOCKS) {
                                    TextView textView = positionTextview(textBlocks.get(index), blockFrame);

                                    // Translate text
                                    translator.translate(blockText)
                                            .addOnSuccessListener(
                                                    (OnSuccessListener) translatedText -> {
                                                        textView.setBackgroundColor(Color.WHITE);
                                                        textView.getBackground().setAlpha(200);
                                                        textView.setText(String.valueOf(translatedText));

                                                        recordTranslation(blockText);
                                                    });
                                }
                            }
                        })
                        .addOnFailureListener(Throwable::printStackTrace)
                        .addOnCompleteListener(task ->
                            imageProxy.close());
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private TextView positionTextview(TextView textView, Rect blockFrame) {
        int left = blockFrame.left;
        int top = blockFrame.top;
        int right = blockFrame.right;
        int bottom = blockFrame.bottom + 30;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(right - left, bottom - top);
        params.setMargins(
                left,
                top,
                right,
                bottom);
        textView.setLayoutParams(params);
        textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        return textView;
    }

    private void recordTranslation(String text) {
        boolean isSimilar = false;
        for (String recognisedText : recognisedTexts) {
            isSimilar = isTextSimilar(recognisedText, text);
            if (isSimilar) break;
        }

        if (!isSimilar) {
            recognisedTexts.add(text);
            TranslationRecord translationRecord = new TranslationRecord.Builder(mAuth.getUid())
                    .withCountry(userCountry)
                    .withLanguages(sourceLanguage, targetLanguage)
                    .withText(text)
                    .withTimestamp(Instant.now().toEpochMilli())
                    .build();

            db.collection("translations")
                    .add(translationRecord)
                    .addOnFailureListener(Throwable::printStackTrace);
        }
    }

    public String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toLowerCase(Locale.US);
            }
            else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toLowerCase(Locale.US);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isTextSimilar(String text1, String text2) {
        int lengthDifference = Math.abs(text1.length() - text2.length());
        if (lengthDifference > 4) {
            return false;
        }
        double averageLength = (((double) text1.length() + text2.length()) / 2);
        Levenshtein levenshtein = new Levenshtein();
        double score = levenshtein.distance(text1, text2) / averageLength;
        return score < 0.2;
    }

    @Override
    public void onBackPressed() {
        imageAnalysis.clearAnalyzer();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}