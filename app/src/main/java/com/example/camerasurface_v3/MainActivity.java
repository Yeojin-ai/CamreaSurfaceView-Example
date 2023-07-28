package com.example.camerasurface_v3;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "IRISCameraApp";

    public String CamID = "0";
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptReqBuilder;
    private CameraCaptureSession mSession;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private ImageReader mImageReader;

    private Handler mHandler;
    private int mWidth = 0;
    private int mHeight = 0;

    private MediaRecorder mMediaRecorder;






    private static final SparseArray ORIENTATIONS = new SparseArray();
    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"iris___onCreate() start");

        setContentView(R.layout.main_activity);
        mSurfaceView = findViewById(R.id.surfaceView);
        Toast.makeText(this,"onCreate",Toast.LENGTH_SHORT).show();
        Log.d(TAG,"iris onCreate2");

        Button capture = findViewById(R.id.captureButton);
        capture.setAlpha(0.9f);

        initSurfaceView();
    }



    private void initSurfaceView(){
        Log.d(TAG,"iris initSurfaceView");
/*
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mWidth=displayMetrics.widthPixels;
        mHeight=displayMetrics.heightPixels;

 */

        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.d(TAG,"iris surfaceCreated");
                initCamAndPrev();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG,"iris surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                mCameraDevice.close();
            }
        });
    }

    public void initCamAndPrev(){
        Log.d(TAG,"iris initCamAndPrev");
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        openCamera();
    }

    private void openCamera() {
        Log.d(TAG,"iris openCamera");
        CameraManager mCameraManager= (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(CamID);
            StreamConfigurationMap scm = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size mPreviewSize = scm.getOutputSizes(ImageFormat.JPEG)[0];
            //setAspectRatioView(mPreviewSize.getHeight(),mPreviewSize.getWidth());

            mImageReader = ImageReader.newInstance(
                    mPreviewSize.getWidth(),
                    mPreviewSize.getHeight(),
                    ImageFormat.JPEG,
                    7);

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) return;
            mCameraManager.openCamera(CamID,deviceStateCallback, mHandler);

        } catch (CameraAccessException e) {
            Toast.makeText(this,"couldn't open camera",Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }
    private CameraDevice.StateCallback deviceStateCallback= new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                takePreview();
            }catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
            mCameraDevice=null;

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Toast.makeText(null,"couldn't open camera",Toast.LENGTH_SHORT).show();

        }
    };
    public void takePreview() throws CameraAccessException {
        Log.d(TAG,"iris takePreview");
        Surface previewSurface = mSurfaceHolder.getSurface();
        //Surface imageSurface = mImageReader.getSurface();

        mCaptReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCaptReqBuilder.addTarget(previewSurface);
        mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),mSessionPreviewStateCallback, mHandler);
    }

    public CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            try {
                mCaptReqBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mCaptReqBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mSession.setRepeatingRequest(mCaptReqBuilder.build(),null,mHandler);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(null,"Configure camera failed",Toast.LENGTH_SHORT).show();
        }
    };

    private void setAspectRatioView(int width, int height){
        if(width>height){
            int newWidth = mWidth;
            int newHeight = ((mWidth*width)/height);
            Log.d("@@@", "TextureView Width : " + newWidth + " TextureView Height : " + newHeight);
            mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(newWidth,newHeight));
        }
        else{
            int newWidth = mWidth;
            int newHeight = ((mWidth*height)/width);
            Log.d("@@@", "TextureView Width : " + newWidth + " TextureView Height : " + newHeight);
            mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(newWidth,newHeight));
        }
    }
}
