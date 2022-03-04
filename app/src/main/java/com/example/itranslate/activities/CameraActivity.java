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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.itranslate.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CameraActivity extends AppCompatActivity {

    private PreviewView cameraView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageAnalysis imageAnalysis;
    private TextRecognizer recognizer;
    private FrameLayout frameLayout;
    private ArrayList<TextView> textBlocks;
    private final int TEXT_BLOCKS = 10;
    private SharedPreferences sharedPreferences;
    private Translator translator;
    private static final int MINIMUM_BLOCK_SIZE = 400;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.previewView);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        initializeBlocks();
        sharedPreferences = getApplicationContext().getSharedPreferences("translationModels", 0);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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
        String sourceLanguage = sharedPreferences.getString("sourceLanguage", "en");
        String targetLanguage = sharedPreferences.getString("targetLanguage", Locale.getDefault().getLanguage());

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
                System.out.println(imageProxy.getImageInfo().getRotationDegrees());
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
                                int rectSize = (blockFrame.right - blockFrame.left) + (blockFrame.bottom - blockFrame.top);
                                if (rectSize < MINIMUM_BLOCK_SIZE) {
                                    continue;
                                }

                                int index = visionText.getTextBlocks().indexOf(block);
                                if (index < TEXT_BLOCKS) {
                                    TextView textView = textBlocks.get(index);

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

                                    // Translate text
                                    translator.translate(blockText)
                                            .addOnSuccessListener(
                                                    (OnSuccessListener) translatedText -> {
                                                        textView.setBackgroundColor(Color.WHITE);
                                                        textView.getBackground().setAlpha(200);
                                                        textView.setText(String.valueOf(translatedText));
                                                    });
                                }
                            }




                        })
                        .addOnFailureListener(Throwable::printStackTrace)
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                        });
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @Override
    public void onBackPressed() {
        imageAnalysis.clearAnalyzer();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}