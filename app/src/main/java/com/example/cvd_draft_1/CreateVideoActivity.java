package com.example.cvd_draft_1;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.CountDownTimer;
import android.util.Size;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Quality;
import androidx.camera.video.FileOutputOptions;

import android.hardware.camera2.CameraManager;
import android.widget.TextView;

public class CreateVideoActivity extends AppCompatActivity {
    public static final int STABILIZATION_MODE_ON = 1;
    public static final int STABILIZATION_MODE_OFF = 0;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean isRecording = false;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private CameraSelector cameraSelector;
    private boolean isBackCamera = true;
    private boolean isFlashlightOn = false;
    private boolean isStabilizationOn = false;
    private CameraManager cameraManager;
    private String cameraId;
    private Camera camera;
    private SeekBar zoomSeekBar;
    private SeekBar exposureSeekBar;
    private Spinner videoQualitySpinner;
    private Quality selectedQuality = Quality.HIGHEST;
    private CircularProgressIndicator recordingProgressBar;
    private TextView recordingTime;
    private Handler handler;
    private Runnable updateRecordingTimeRunnable;
    private long startTime;


    private HorizontalScrollView scriptScrollView;
    private Button btnIncreaseSpeed, btnDecreaseSpeed;
    private Handler scrollHandler = new Handler();
    private int scrollSpeed = 30;
    private int scrollPosition = 0;
    private final int SCROLL_INCREMENT = 5;
    private Runnable scrollRunnable;
    private Button timerButton;
    private boolean isRecordingStarted = false;
    private ImageView recordingImageView;
    private TextView scriptTextView;
    private int maxScrollAmount = 0;
    private TextView timerDisplay;

    // overlay View

    private OverlayView overlayView;

    private ExecutorService cameraExecutor;

    private int selectedSegmentationStyle;

    boolean isSegmentationEnabled = false; // This should be set based on user input
    PreviewView previewView;






    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_video);

        recordingImageView = findViewById(R.id.recordingImageView);



        scriptScrollView = findViewById(R.id.script_scroll_view);
        btnIncreaseSpeed = findViewById(R.id.btn_increase_speed);
        btnDecreaseSpeed = findViewById(R.id.btn_decrease_speed);
        scriptTextView = findViewById(R.id.script_text);

        overlayView = findViewById(R.id.overlayView);
        previewView = findViewById(R.id.previewView);





//        scrollRunnable = new Runnable() {
//            @Override
//            public void run() {
//                int maxScrollAmount = scriptTextView.getWidth(); // Total width of the text content
//
//                scrollPosition += SCROLL_INCREMENT;
//                if (scrollPosition >= maxScrollAmount) {
//                    scrollPosition = 0;
//                }
//
//                scriptScrollView.smoothScrollTo(scrollPosition, 0); // Scroll horizontally
//                scrollHandler.postDelayed(this, scrollSpeed); // Schedule the next scroll
//            }
//        };
//
//        // Start the auto-scroll
//        scrollHandler.post(scrollRunnable);

        scriptTextView.post(new Runnable() {
            @Override
            public void run() {
                // Create the TranslateAnimation after the TextView has been fully laid out
                TranslateAnimation translateAnimation = new TranslateAnimation(
                        TranslateAnimation.ABSOLUTE, 0,
                        TranslateAnimation.ABSOLUTE, -scriptTextView.getWidth(),
                        TranslateAnimation.ABSOLUTE, 0,
                        TranslateAnimation.ABSOLUTE, 0);

                translateAnimation.setDuration(5000); // Duration for one full slide
                translateAnimation.setRepeatCount(Animation.INFINITE); // Repeat the animation infinitely
                translateAnimation.setInterpolator(new LinearInterpolator());

                // Start the animation on the TextView
                scriptTextView.startAnimation(translateAnimation);
            }
        });



        // Button listeners to increase or decrease speed
        btnIncreaseSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scrollSpeed > 5) {
                    scrollSpeed -= 5;
                }
            }
        });

        btnDecreaseSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollSpeed += 5;
            }
        });


        timerButton = findViewById(R.id.toggle_timer);
        timerDisplay = findViewById(R.id.timer_display);

        timerDisplay.setVisibility(View.GONE);

        timerButton.setOnClickListener(view -> {
            if (!isRecordingStarted || !isRecording) {
                startCountdownBeforeRecording();
            } else {
                Toast.makeText(this, "Recording in progress...", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize UI components
        Button toggleStabilizationButton = findViewById(R.id.toggle_stabilization);



        // segmentation spinner

        // Set up segmentation options (e.g., grayscale, colored overlay)
        Spinner segmentationOptions = findViewById(R.id.segmentationOptions);
        segmentationOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSegmentationStyle = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // Set up the button click listener for the video stabilization button
        toggleStabilizationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStabilization();
            }
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        FloatingActionButton fabRecording = findViewById(R.id.fab_recording);
        FloatingActionButton showBottomSheetButton = findViewById(R.id.show_bottom_sheet_button);
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        zoomSeekBar = findViewById(R.id.zoomSeekBar);
        exposureSeekBar = findViewById(R.id.exposure_control);
        videoQualitySpinner = findViewById(R.id.video_quality);
        recordingProgressBar = findViewById(R.id.recording_progress_bar);
        recordingTime = findViewById(R.id.recording_time);

        List<String> supportedQualities = getSupportedQualities();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, supportedQualities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoQualitySpinner.setAdapter(adapter);

        videoQualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String quality = parent.getItemAtPosition(position).toString();
                switch (quality) {
                    case "Lowest":
                        selectedQuality = Quality.LOWEST;
                        break;
                    case "SD":
                        selectedQuality = Quality.SD;
                        break;
                    case "HD":
                        selectedQuality = Quality.HD;
                        break;
                    case "FHD":
                        selectedQuality = Quality.FHD;
                        break;
                    case "UHD":
                        selectedQuality = Quality.UHD;
                        break;
                    default:
                        selectedQuality = Quality.HIGHEST;
                        break;
                }
                startCamera();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        fabRecording.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
//                startRecording();
                startCountdownBeforeRecording();
            }
        });

        showBottomSheetButton.setOnClickListener(v -> {
            if (bottomSheet.getVisibility() == View.GONE) {
                bottomSheet.setVisibility(View.VISIBLE);
            } else {
                bottomSheet.setVisibility(View.GONE);
            }
        });

        Button rotateButton = findViewById(R.id.rotateButton);
        rotateButton.setOnClickListener(v -> {
            isBackCamera = !isBackCamera;
            startCamera();
        });

        Button toggleFlashlightButton = findViewById(R.id.toggle_flashlight);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        toggleFlashlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (camera != null) {
                    float maxZoomRatio = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
                    float zoomRatio = progress / 100.0f * (maxZoomRatio - 1) + 1;
                    camera.getCameraControl().setZoomRatio(zoomRatio);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (camera != null) {
                    float exposureCompensation = (progress - 50) / 50.0f; // Assuming the range is -1 to 1
                    camera.getCameraControl().setExposureCompensationIndex((int) (exposureCompensation * camera.getCameraInfo().getExposureState().getExposureCompensationRange().getUpper()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> resetExposure());

        Button resetZoomButton = findViewById(R.id.reset_zoom_button);
        resetZoomButton.setOnClickListener(v -> resetZoom());

        handler = new Handler(Looper.getMainLooper());
        updateRecordingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    int seconds = (int) (elapsedTime / 1000) % 60;
                    int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
                    int hours = (int) (elapsedTime / (1000 * 60 * 60)) % 24;
                    recordingTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    recordingProgressBar.setProgress((int) (elapsedTime / 1000));
                    handler.postDelayed(this, 1000);
                }
            }
        };

        cameraExecutor = Executors.newSingleThreadExecutor();

    }


    private void startCountdownBeforeRecording() {
        runOnUiThread(() -> {
            timerDisplay.setVisibility(View.VISIBLE); // Ensure the timer display is visible
        });

        new CountDownTimer(3000, 1000) { // 3 seconds countdown with 1-second intervals
            public void onTick(long millisUntilFinished) {
                runOnUiThread(() -> {
                    timerDisplay.setText(String.valueOf(millisUntilFinished / 1000)); // Update the countdown timer display
                });
            }

            public void onFinish() {
                runOnUiThread(() -> {
                    timerDisplay.setVisibility(View.GONE); // Hide the timer display once the countdown is over
                    startRecording(); // Start the recording after the countdown finishes
                });
            }
        }.start();
    }

    private List<String> getSupportedQualities() {
        List<String> supportedQualities = new ArrayList<>();
        if (isQualitySupported(Quality.LOWEST)) supportedQualities.add("Lowest");
        if (isQualitySupported(Quality.SD)) supportedQualities.add("SD");
        if (isQualitySupported(Quality.HD)) supportedQualities.add("HD");
        if (isQualitySupported(Quality.FHD)) supportedQualities.add("FHD");
        if (isQualitySupported(Quality.UHD)) supportedQualities.add("UHD");
        if (isQualitySupported(Quality.HIGHEST)) supportedQualities.add("Highest");
        return supportedQualities;
    }



    private boolean isQualitySupported(Quality quality) {
        // Check if the selected quality is supported by the device
        // This is a placeholder implementation, you need to replace it with actual checks
        // based on your device's capabilities
        return true; // Replace with actual check
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecording) {
            stopRecording();
        }
    }

    private void toggleFlashlight() {
        try {
            if (camera != null) {
                camera.getCameraControl().enableTorch(!isFlashlightOn);
                isFlashlightOn = !isFlashlightOn;
            } else {
                Toast.makeText(this, "Camera is not initialized", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CameraXApp", "Error toggling flashlight", e);
            Toast.makeText(this, "Error toggling flashlight: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleStabilization() {
        isStabilizationOn = !isStabilizationOn;
        if (camera != null) {
            // Enable or disable stabilization for the Video
            Recorder recorder = new Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(selectedQuality))
                    .build();
            videoCapture = VideoCapture.withOutput(recorder);
            Toast.makeText(this, "Stabilization " + (isStabilizationOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Camera is not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        recordingImageView.setVisibility(View.GONE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // Conditional Segmentation Setup

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                int rotation = previewView.getDisplay().getRotation();


                // Preview setup
                Preview preview = new Preview.Builder().setTargetRotation(rotation).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());


                // Setup the camera selector (front or back camera)
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                        .build();

                // Real-time Image Analysis setup for background segmentation
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setTargetRotation(rotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // Process each frame for segmentation
                    processImageProxy(imageProxy);
                });



                // Video Capture setup
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(selectedQuality))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                // Bind all use cases to the camera
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);
//                cameraProvider.bindToLifecycle(
//                        this, cameraSelector,videoCapture, preview, imageAnalysis // Not supported more than three bind may support in another device.
//                );

//                if (isSegmentationEnabled) {
//                    // Bind imageAnalysis to enable segmentation
//                    camera = cameraProvider.bindToLifecycle(
//                            this, cameraSelector, videoCapture, imageAnalysis);
//                } else {
//                    // Bind without imageAnalysis for normal video recording
//                    camera = cameraProvider.bindToLifecycle(
//                            this, cameraSelector, preview, imageAnalysis);
//                }

            } catch (Exception e) {
                Log.e("CameraXApp", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy imageProxy) {
        // Ensure the image is not null before processing
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Convert ImageProxy to Bitmap

//        Bitmap bitmap = imageProxyToBitmap(imageProxy);

        Bitmap bitmap = yuvToRgb(imageProxy);


        if (bitmap == null) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap");
            imageProxy.close();
            return;
        }

        // Convert ImageProxy to InputImage for ML Kit segmentation
        InputImage inputImage = InputImage.fromMediaImage(
                Objects.requireNonNull(imageProxy.getImage()),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // Perform segmentation and close the imageProxy after completion
        segmentImage(inputImage, bitmap, imageProxy);
    }

    private void segmentImage(InputImage image, Bitmap bitmap, ImageProxy imageProxy) {
        SelfieSegmenterOptions options = new SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)  // Real-time segmentation mode
                .enableRawSizeMask()
                .build();

        Segmentation.getClient(options).process(image)
                .addOnSuccessListener(mask -> {
                    // Process the segmentation mask using the converted Bitmap
                    processSegmentationMask(mask, bitmap);

                    // Close the imageProxy after processing
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Segmentation failed: " + e.getMessage(), e);
                    imageProxy.close();
                });
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processSegmentationMask(SegmentationMask mask, Bitmap cameraBitmap) {
        if (cameraBitmap == null) {
            Log.e(TAG, "Bitmap is null. Cannot process segmentation.");
            return;
        }


        // Rotate and resize the cameraBitmap to match the mask dimensions
        int currentRotation = previewView.getDisplay().getRotation();
        Bitmap rotatedCameraBitmap = rotateBitmapToPortrait(cameraBitmap, currentRotation);
        Bitmap resizedCameraBitmap = Bitmap.createScaledBitmap(rotatedCameraBitmap, mask.getWidth(), mask.getHeight(), true);

        // Load and resize the background image to match the mask dimensions (for case 1)
        Bitmap backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.step1);
        Bitmap resizedBackgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, mask.getWidth(), mask.getHeight(), true);

        // Create a mutable bitmap for the final composited image
        Bitmap resultBitmap = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Get the segmentation mask buffer
        ByteBuffer buffer = mask.getBuffer();
        buffer.rewind();

        int width = mask.getWidth();
        int height = mask.getHeight();
        int[] pixels = new int[width * height];
        resizedCameraBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        if(selectedSegmentationStyle !=0){
            overlayView.setVisibility(View.VISIBLE);
        }

        // Iterate through the mask pixels and blend the camera feed or apply effects
        for (int i = 0; i < width * height; i++) {
            float confidence = buffer.getFloat();

            // Coordinates for pixel location
            int x = i % width;
            int y = i / width;

            if (confidence < 0.5f) { // Background pixel
                if (selectedSegmentationStyle == 1) {
                    // Case 1: Replace the background with the image
                    int backgroundPixel = resizedBackgroundBitmap.getPixel(x, y);
                    resultBitmap.setPixel(x, y, backgroundPixel);
                } else {
                    // Handle other cases with visual effects on the background pixels
                    int color = pixels[i];
                    int alpha = Color.alpha(color);
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                    switch (selectedSegmentationStyle) {
                    case 0:
                        overlayView.setVisibility(View.GONE);
                        isSegmentationEnabled = false;
                        break;

//                    case 1: // Red overlay
//                        pixels[i] = Color.argb(128, 255, 0, 0); // Semi-transparent red
//                        break;

                    case 2: // Blue overlay
                        pixels[i] = Color.argb(128, 0, 0, 255); // Semi-transparent blue
                        break;

                    case 3: // Green overlay
                        pixels[i] = Color.argb(128, 0, 255, 0); // Semi-transparent green
                        break;

                    case 4: // Inverted colors
                        int invertedRed = 255 - red;
                        int invertedGreen = 255 - green;
                        int invertedBlue = 255 - blue;
                        pixels[i] = Color.argb(alpha, invertedRed, invertedGreen, invertedBlue);
                        break;

                    case 5: // Sepia tone
                        int sepiaRed = (int)(red * 0.393 + green * 0.769 + blue * 0.189);
                        int sepiaGreen = (int)(red * 0.349 + green * 0.686 + blue * 0.168);
                        int sepiaBlue = (int)(red * 0.272 + green * 0.534 + blue * 0.131);
                        sepiaRed = Math.min(255, sepiaRed);
                        sepiaGreen = Math.min(255, sepiaGreen);
                        sepiaBlue = Math.min(255, sepiaBlue);
                        pixels[i] = Color.argb(alpha, sepiaRed, sepiaGreen, sepiaBlue);
                        break;

                    case 6: // Pixelation effect
                        if (i % 10 == 0) {
                            int blockGray = (red + green + blue) / 3;
                            pixels[i] = Color.argb(alpha, blockGray, blockGray, blockGray);
                        }
                        break;

                    case 7: // Vignette effect
                        int centerX = width / 2;
                        int centerY = height / 2;
                        double radius = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                        double maxRadius = Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
                        double vignetteFactor = radius / maxRadius;
                        int vignetteGray = (int)(255 - vignetteFactor * 255);
                        pixels[i] = Color.argb(alpha, vignetteGray, vignetteGray, vignetteGray);
                        break;

                    case 8: // Black and White
                        int bw = (red + green + blue) / 3;
                        pixels[i] = Color.argb(alpha, bw, bw, bw);
                        break;

                    case 9: // Brighten
                        int brightRed = Math.min(255, red + 50);
                        int brightGreen = Math.min(255, green + 50);
                        int brightBlue = Math.min(255, blue + 50);
                        pixels[i] = Color.argb(alpha, brightRed, brightGreen, brightBlue);
                        break;

                    case 10: // Darken
                        int darkRed = Math.max(0, red - 50);
                        int darkGreen = Math.max(0, green - 50);
                        int darkBlue = Math.max(0, blue - 50);
                        pixels[i] = Color.argb(alpha, darkRed, darkGreen, darkBlue);
                        break;

                    case 11: // Red Tint
                        pixels[i] = Color.argb(128, Math.min(255, red + 100), green, blue);
                        break;

                    case 12: // Green Tint
                        pixels[i] = Color.argb(128, red, Math.min(255, green + 100), blue);
                        break;

                    case 13: // Blue Tint
                        pixels[i] = Color.argb(128, red, green, Math.min(255, blue + 100));
                        break;

                    case 14: // Warm Tone
                        int warmRed = Math.min(255, red + 50);
                        int warmYellow = Math.min(255, green + 30);
                        pixels[i] = Color.argb(alpha, warmRed, warmYellow, blue);
                        break;

                    case 15: // Cold Tone
                        int coldBlue = Math.min(255, blue + 50);
                        pixels[i] = Color.argb(alpha, red, green, coldBlue);
                        break;

                    case 16: // Increase Contrast
                        int contrastRed = (int) ((red - 128) * 1.5 + 128);
                        int contrastGreen = (int) ((green - 128) * 1.5 + 128);
                        int contrastBlue = (int) ((blue - 128) * 1.5 + 128);
                        contrastRed = Math.min(255, Math.max(0, contrastRed));
                        contrastGreen = Math.min(255, Math.max(0, contrastGreen));
                        contrastBlue = Math.min(255, Math.max(0, contrastBlue));
                        pixels[i] = Color.argb(alpha, contrastRed, contrastGreen, contrastBlue);
                        break;

                    case 17: // Blur effect (Approximation: Reduce sharpness)
                        if (i > 1 && i < width * height - 1) {
                            int avgRed = (red + Color.red(pixels[i - 1]) + Color.red(pixels[i + 1])) / 3;
                            int avgGreen = (green + Color.green(pixels[i - 1]) + Color.green(pixels[i + 1])) / 3;
                            int avgBlue = (blue + Color.blue(pixels[i - 1]) + Color.blue(pixels[i + 1])) / 3;
                            pixels[i] = Color.argb(alpha, avgRed, avgGreen, avgBlue);
                        }
                        break;

                    case 18: // Grayscale background
                        int gray = (red + green + blue) / 3;
                        pixels[i] = Color.argb(alpha, gray, gray, gray);
                        break;

                    default:
                        break;
                }

                    resultBitmap.setPixel(x, y, pixels[i]);
                }
            } else {
                int cameraPixel = pixels[i];
                resultBitmap.setPixel(x, y, cameraPixel);
            }
        }

        runOnUiThread(() -> {
            overlayView.setOverlayBitmap(resultBitmap);
        });
    }


    private Bitmap rotateBitmapToPortrait(Bitmap bitmap, int currentRotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(-90);

//        switch (currentRotation) {
//            case Surface.ROTATION_0:
//                // No rotation needed for portrait
//                break;
//            case Surface.ROTATION_90:
//                matrix.postRotate(-90); // Rotate back to portrait from landscape right
//                break;
//            case Surface.ROTATION_180:
//                matrix.postRotate(180); // Rotate 180 degrees to get back to portrait
//                break;
//            case Surface.ROTATION_270:
//                matrix.postRotate(90); // Rotate back to portrait from landscape left
//                break;
//            default:
//                break;
//        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }




    private Bitmap yuvToRgb(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer(); // Y
        ByteBuffer uBuffer = planes[1].getBuffer(); // U
        ByteBuffer vBuffer = planes[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }



    private void startRecording() {
        isRecordingStarted = true;
        showFileNameDialog();
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter file name");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String fileName = input.getText().toString();
            if (!fileName.isEmpty()) {
                File videoFile = new File(getExternalFilesDir(null), fileName + ".mp4");
                try {
                    recording = videoCapture.getOutput()
                            .prepareRecording(this, new FileOutputOptions.Builder(videoFile).build())
                            .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                    isRecording = true;
                                    startTime = System.currentTimeMillis();
                                    handler.post(updateRecordingTimeRunnable);
                                    toggleRecordingButtons();
                                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                    isRecording = false;
                                    handler.removeCallbacks(updateRecordingTimeRunnable);
                                    toggleRecordingButtons();
                                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                                        Toast.makeText(this, "Recording saved: " + videoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e("CameraXApp", "Recording error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError());
                                        handleRecordingError(videoFile);
                                    }
                                }
                            });
                } catch (Exception e) {
                    Log.e("CameraXApp", "Error starting recording", e);
                    Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    handleRecordingError(videoFile);
                }
            } else {
                Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void handleRecordingError(File videoFile) {
        // Save the recording and reset the state
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        Toast.makeText(this, "Recording saved with errors: " + videoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        resetToInitialState();
    }

    private void resetToInitialState() {
        // Reset UI components and state variables to initial state
        toggleRecordingButtons();
        resetExposure();
        resetZoom();
        recordingTime.setText("00:00:00");
        recordingProgressBar.setProgress(0);
        isRecording = false;
        isFlashlightOn = false;
        isStabilizationOn = false;
        startCamera();
    }

    private void stopRecording() {
        isRecordingStarted = false;
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }

    private void toggleRecordingButtons() {
        FloatingActionButton fabRecording = findViewById(R.id.fab_recording);
        if (isRecording) {
            fabRecording.setImageResource(R.drawable.ic_stop); // Change icon to stop
        } else {
            fabRecording.setImageResource(R.drawable.ic_record); // Change icon to record
        }
    }

    private void resetExposure() {
        if (camera != null) {
            camera.getCameraControl().setExposureCompensationIndex(0);
            exposureSeekBar.setProgress(50); // Reset SeekBar to the middle position
        }
    }

    private void resetZoom() {
        if (camera != null) {
            camera.getCameraControl().setZoomRatio(1.0f); // Reset zoom to normal
            zoomSeekBar.setProgress(0); // Reset SeekBar to the initial position
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }
}

