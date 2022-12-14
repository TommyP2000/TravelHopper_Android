package uk.ac.tees.b1662096.travelhopper_travelapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.lang.Math;

import uk.ac.tees.b1662096.travelhopper_travelapp.databinding.ActivityMyCameraBinding;

public class MyCameraActivity extends AppCompatActivity {

    private ActivityMyCameraBinding myCameraActivityBinding;

    private View rootView;

    private static final String[] CAMERA_ACTIVITY_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_ACTIVITY_PERMISSION_REQUEST_CODE = 10;

    private Intent navigateToHome;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private ProcessCameraProvider cameraProvider;

    private ExecutorService cameraExecutor;

    private int[] facingLens;

    private Preview preview;

    private PreviewView previewView;

    private ImageCapture imageCapture;

    private VideoCapture<Recorder> videoCapture;

    private Recorder recorder;

    private Recording videoRecording;

    private boolean curRecording = false;

    private FloatingActionButton imageCaptureButton, videoCaptureButton, navigateBackButton, switchCameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up View Binding for the Root View
        myCameraActivityBinding = ActivityMyCameraBinding.inflate(getLayoutInflater());
        rootView = myCameraActivityBinding.getRoot();
        setContentView(rootView);

        // Check if device has an available camera
        if (cameraIsAvailable()) {
            Log.i("VIDEO_RECORD_TAG", "Camera is detected");
            // Set up the CameraProvider and check for permissions granted
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    if (cameraPermissionGranted()) {
                        startCamera(cameraProvider);
                    } else {
                        requestMyCameraPermissions();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("ExecutionException/InterruptedException", "CameraX Binding Failed", e);
                }
            }, ContextCompat.getMainExecutor(this));

        } else {
            Log.i("VIDEO_RECORD_TAG", "Error - Unable to detect camera");
        }

        // Set up the previewView for CameraX through view binding
        previewView = myCameraActivityBinding.previewViewFinder;

        // Set up Photo, Switch Camera and Video Buttons
        imageCaptureButton = myCameraActivityBinding.imageCaptureButton;
        imageCaptureButton.setOnClickListener(view -> {
            // Check for permissions once the imageCaptureButton was clicked
            if (readExternalStoragePermissionGranted() && writeExternalStoragePermissionGranted()) {
                capturePhoto();
            }
        });
        videoCaptureButton = myCameraActivityBinding.videoCaptureButton;
        videoCaptureButton.setOnClickListener(view -> {
            // Check for permissions once the videoCaptureButton was clicked
            if (readExternalStoragePermissionGranted() && writeExternalStoragePermissionGranted() && recordAudioPermissionGranted()) {
                captureVideoRecording();
            }
        });

        facingLens = new int[]{CameraSelector.LENS_FACING_BACK};
        switchCameraButton = myCameraActivityBinding.switchCameraButton;

        switchCameraButton.setOnClickListener(view -> {
            if (facingLens[0] == CameraSelector.LENS_FACING_BACK) {
                // Switch to the front camera if the back camera is currently selected
                facingLens[0] = CameraSelector.LENS_FACING_FRONT;
            } else if (facingLens[0] == CameraSelector.LENS_FACING_FRONT) {
                // Switch to the back camera if the front camera is currently selected
                facingLens[0] = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraProvider);
        });

        // Set up the back Button
        navigateBackButton = myCameraActivityBinding.navigateBackButton;
        navigateToHome = new Intent(this, MainActivity.class);
        navigateBackButton.setOnClickListener(view -> startActivity(navigateToHome));

        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    private boolean cameraIsAvailable() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    // Before the main Camera starts, check if permissions are granted by the user
    private boolean cameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA_ACTIVITY_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED;
    }

    // Before recording a video, check if the RECORD_AUDIO permission was granted by the user
    private boolean recordAudioPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA_ACTIVITY_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    // Before capturing a photo or video, check if READ and WRITE permissions are granted by the user
    private boolean readExternalStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA_ACTIVITY_PERMISSIONS[2]) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean writeExternalStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, CAMERA_ACTIVITY_PERMISSIONS[3]) == PackageManager.PERMISSION_GRANTED;
    }

    // If all permissions were not already granted by the user, ask the user for these permissions at runtime
    private void requestMyCameraPermissions() {
        ActivityCompat.requestPermissions(this, CAMERA_ACTIVITY_PERMISSIONS, CAMERA_ACTIVITY_PERMISSION_REQUEST_CODE);
    }

    // Start the main Camera functionality
    private void startCamera(ProcessCameraProvider cameraProvider) {

        // Set up the ImageCapture use case
        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Set up the VideoCapture use case
        // Build Recorder as a part of the VideoCapture
        QualitySelector videoQualitySelector = QualitySelector.fromOrderedList(Arrays.asList(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD), FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));
        recorder = new Recorder.Builder().setExecutor(ContextCompat.getMainExecutor(this)).setQualitySelector(videoQualitySelector).build();

        // Create the VideoCapture with the Recorder object
        videoCapture = VideoCapture.withOutput(recorder);

        // Set up the CameraSelector use case
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(facingLens[0])
                .build();

        // Set up the Preview use case
        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        // Create an orientation listener that rotates the SurfaceView of the PreviewView based on the display's orientation
        OrientationEventListener cameraOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int cameraOrientation) {
                int cameraRotation;

                if (cameraOrientation >= 45 && cameraOrientation < 135) {
                    cameraRotation = Surface.ROTATION_270;
                } else if (cameraOrientation >= 135 && cameraOrientation < 225) {
                    cameraRotation = Surface.ROTATION_180;
                } else if (cameraOrientation >= 225 && cameraOrientation < 315) {
                    cameraRotation = Surface.ROTATION_90;
                } else {
                    cameraRotation = Surface.ROTATION_0;
                }
                imageCapture.setTargetRotation(cameraRotation);
            }
        };

        cameraOrientationListener.enable();

        // Set up the implementation mode for the PreviewView
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

        // Set up the scaleType for the PreviewView
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        // Set up the X and Y scales for the PreviewView
        previewView.setScaleX(1f);
        previewView.setScaleY(1f);

        // Set up the previewView SurfaceProvider
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            // Unbind use cases before rebinding to the lifecycle
            cameraProvider.unbindAll();
            // Bind all use cases to the lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
        } catch (IllegalArgumentException ie) {
            Log.e("IllegalArgumentException", "Camera use case binding failed", ie);
        }

    }

    // Capture and save photo to phone storage
    private void capturePhoto() {

        // Check if the imageCapture object is not pointing to null
        if (imageCapture != null) {

            // Create the file name string for the photo file config object - photoContentValues
            String photoFileName = "TravelHopper_Photo-" + new SimpleDateFormat("M", Locale.ENGLISH).format(System.currentTimeMillis()) + ".jpg";
            ContentValues photoContentValues = new ContentValues();
            // Use MediaStore to set the photo file for the content resolver to handle
            photoContentValues.put(MediaStore.Images.Media.TITLE, photoFileName);
            photoContentValues.put(MediaStore.Images.Media.DISPLAY_NAME, photoFileName);
            photoContentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            photoContentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                photoContentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "TravelHopper");
            }

            // Taking the photoContentValues object created, build the output options for the file
            ImageCapture.OutputFileOptions photoOutputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            photoContentValues).build();
            // Capture the photo with the output options and record the capture from callback to output a new photo file
            imageCapture.takePicture(photoOutputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        // If the photo has successfully saved, return a success message/Snackbar and pass the new image to the CreateNewTripFragment
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Snackbar.make(rootView, "Photo has been saved successfully", Snackbar.LENGTH_SHORT).show();
//                            // Pass the image to the MyTripsFragment using an Intent
//                            Uri photoUri = outputFileResults.getSavedUri();
//                            assert photoUri != null;
//                            Intent passImageToNewTrip = new Intent(MyCameraActivity.this, MyTripsFragment.class);
//                            passImageToNewTrip.putExtra("CAPTURED_PHOTO", photoUri);
//                            startActivity(passImageToNewTrip);
                        }

                        // If the photo has not successfully saved, return an error message/Snackbar
                        @Override
                        public void onError(@NonNull ImageCaptureException error) {
                            Snackbar.make(rootView, "Error while saving photo: " + error.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    }
            );
            // Clear the contents of the photo once the photo file has been successfully saved
            photoContentValues.clear();
        }
    }

    @SuppressLint("MissingPermission")
    private void captureVideoRecording() {

        // Check if the videoCapture object is not pointing to null
        if (videoCapture != null) {

            // Create the file name string for the video file config object - videoContentValues
            String videoFileName = "TravelHopper_Video-" + new SimpleDateFormat("M", Locale.ENGLISH).format(System.currentTimeMillis()) + ".mp4";
            ContentValues videoContentValues = new ContentValues();
            // Use MediaStore to set the video file for the content resolver to handle
            videoContentValues.put(MediaStore.MediaColumns.TITLE, videoFileName);
            videoContentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, videoFileName);
            videoContentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
            videoContentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                videoContentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Video/" + "TravelHopper");
            }

            // Taking the videoOutputFileOptions object created, build the output options for the file
            MediaStoreOutputOptions videoOutputFileOptions = new MediaStoreOutputOptions.Builder(getContentResolver(),
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(videoContentValues).build();
            // Retrieve the output stream from the capture and start the recording with the output options for the file
            try {
                if (!curRecording) {
                    videoRecording = videoCapture.getOutput()
                            .prepareRecording(this, videoOutputFileOptions)
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                    Snackbar.make(rootView, "Video is now recording", Snackbar.LENGTH_SHORT).show();
                                    // Change the icon of the record button when recording starts and the button is clicked
                                    videoCaptureButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop_recording_icon, getApplicationContext().getTheme()));
                                    videoCaptureButton.isEnabled();
                                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                    if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                                        videoCaptureButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_record_red_icon, getApplicationContext().getTheme()));
                                        Snackbar.make(rootView, "Video has been captured successfully", Snackbar.LENGTH_SHORT).show();
                                        // Clear the contents of the video once the recording has stopped
                                        videoContentValues.clear();
                                    } else {
                                        videoRecording = null;
                                        Snackbar.make(rootView, "Error while capturing video: " + "${error}", Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    videoRecording.stop();
                    curRecording = false;
                }
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

//    private void changeSwitchCameraButton() {
//        try {
//            backCameraAvailable();
//        } catch (CameraInfoUnavailableException | ExecutionException | InterruptedException e) {
//            e.printStackTrace();
//            boolean disabledSwitchCameraBtn = !myCameraActivityBinding.switchCameraButton.isEnabled();
//        }
//    }
//
//    private boolean backCameraAvailable() throws ExecutionException, InterruptedException, CameraInfoUnavailableException {
//        return cameraProviderFuture.get().hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
//    }
//
//    private boolean frontCameraAvailable() throws ExecutionException, InterruptedException, CameraInfoUnavailableException {
//        return cameraProviderFuture.get().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
//    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!cameraPermissionGranted() && !readExternalStoragePermissionGranted() && !writeExternalStoragePermissionGranted() && !recordAudioPermissionGranted()) {
            requestMyCameraPermissions();
        } else if (cameraPermissionGranted() && readExternalStoragePermissionGranted() && writeExternalStoragePermissionGranted() & recordAudioPermissionGranted()) {
            try {
                startCamera(cameraProviderFuture.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            startActivity(navigateToHome);
            Snackbar.make(rootView, "Permissions are required to use the camera", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Once MyCameraActivity is destroyed, shutdown the cameraExecutor thread
        cameraExecutor.shutdown();
    }
}
