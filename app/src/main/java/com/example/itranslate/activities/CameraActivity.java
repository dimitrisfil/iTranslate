package com.example.itranslate.activities;

import androidx.annotation.NonNull;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Size;
import android.view.ViewTreeObserver;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import info.debatty.java.stringsimilarity.Levenshtein;

public class CameraActivity extends AppCompatActivity implements LocationListener {

    private PreviewView cameraView;
    private ImageAnalysis imageAnalysis;
    private TextRecognizer recognizer;
    private FrameLayout frameLayout;
    private ArrayList<TextView> textBlocks;
    private SharedPreferences sharedPreferences;
    private Translator translator;
    private static final int MINIMUM_BLOCK_SIZE = 400;
    private String sourceLanguage;
    private String targetLanguage;
    private FirebaseAuth mAuth;
    private List<String> recognisedTexts;
    private String userCountry;
    private FirebaseFirestore db;
    private Map<String, String> storedTranslations;
    private LocationManager manager;
    private Location myLocation;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mAuth = FirebaseAuth.getInstance();

        cameraView = findViewById(R.id.previewView);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        textBlocks = new ArrayList<>();
        frameLayout = findViewById(R.id.frameLayout);
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        ViewTreeObserver viewTreeObserver = frameLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    frameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
                    cameraProviderFuture = ProcessCameraProvider.getInstance(CameraActivity.this);
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
                    }, ContextCompat.getMainExecutor(CameraActivity.this));

                }
            });
        }

        sharedPreferences = getApplicationContext().getSharedPreferences("translationModels", 0);

        recognisedTexts = new ArrayList<>();

        Locale locale = new Locale("", getUserCountry(this));
        userCountry = !locale.getDisplayCountry().isEmpty() ? locale.getDisplayCountry() : "Unknown";
        db = FirebaseFirestore.getInstance();
        storedTranslations = new HashMap<>();
    }

    private void initializeBlock() {
        TextView textView = new TextView(this);
        textView.setTextColor(Color.BLACK);
        frameLayout.addView(textView);
        textBlocks.add(textView);
    }

    private void destroyLastBlock() {
        frameLayout.removeView(textBlocks.get(textBlocks.size() - 1));
        textBlocks.remove(textBlocks.size() - 1);
    }

    @SuppressLint("MissingPermission")
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

        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
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
                            List<Text.TextBlock> renderedBlocks = resizeTextBlocks(visionText);
                            for (Text.TextBlock block : renderedBlocks) {
                                translateText(block, renderedBlocks.indexOf(block));
                            }
                        })
                        .addOnFailureListener(Throwable::printStackTrace)
                        .addOnCompleteListener(task ->
                                imageProxy.close());
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private List<Text.TextBlock> resizeTextBlocks(Text visionText) {
        List<Text.TextBlock> renderedBlocks = new ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            Rect blockFrame = block.getBoundingBox();
            assert blockFrame != null;
            int rectSize = (blockFrame.right - blockFrame.left) + (blockFrame.bottom - blockFrame.top);
            if (rectSize > MINIMUM_BLOCK_SIZE) {
                renderedBlocks.add(block);
            }
        }
        int blockSizeDiff = Math.abs(renderedBlocks.size() - textBlocks.size());
        if (renderedBlocks.size() > textBlocks.size()) {
            for (int i = 0; i < blockSizeDiff; i++) {
                initializeBlock();
            }
        } else if (renderedBlocks.size() < textBlocks.size()) {
            for (int i = 0; i < blockSizeDiff; i++) {
                destroyLastBlock();
            }
        }
        return renderedBlocks;
    }

    private void translateText(Text.TextBlock block, int index) {
        String blockText = block.getText();
        TextView textView = positionTextview(textBlocks.get(index), block.getBoundingBox());
        textView.setBackgroundColor(Color.WHITE);
        textView.getBackground().setAlpha(200);
        if (storedTranslations.containsKey(blockText)) {
            textView.setText(String.valueOf(storedTranslations.get(blockText)));
        } else {
            translator.translate(blockText)
                    .addOnSuccessListener(
                            (OnSuccessListener) translatedText -> {
                                storedTranslations.put(blockText, String.valueOf(translatedText));
                                textView.setText(String.valueOf(translatedText));
                                recordTranslation(blockText);
                            });
        }
    }

    private TextView positionTextview(TextView textView, Rect blockFrame) {
        int left = blockFrame.left;
        int top = blockFrame.top;
        int right = blockFrame.right;
        int bottom = blockFrame.bottom;

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
                    .withLanguages(new Locale(sourceLanguage).getDisplayLanguage(), new Locale(targetLanguage).getDisplayLanguage())
                    .withText(text)
                    .withLocation(myLocation.getLatitude(), myLocation.getLongitude())
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
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toLowerCase(Locale.US);
                }
            }
        } catch (Exception e) {
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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        myLocation = location;
        manager.removeUpdates(this);
    }
}