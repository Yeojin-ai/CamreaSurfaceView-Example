package com.example.camerasurface_v3;

import static android.provider.MediaStore.Images.Media.insertImage;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "IRISCameraApp";

    //Camera2 and SurfaceView
    public String CamID = "0";
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptReqBuilder;
    private CameraCaptureSession mSession;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    //Save Image
    private ImageReader mImageReader;
    private File file;

    //Handler and Thread
    private Handler mHandler;
    HandlerThread handlerThread;
    Handler mainHandler ;
    
    private int mWidth = 0;
    private int mHeight = 0;

    private Button buttonCapture;

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
        Log.d(TAG,"iris onCreate");
        Toast.makeText(this,"onCreate",Toast.LENGTH_SHORT).show();

        setContentView(R.layout.main_activity);
        mSurfaceView = findViewById(R.id.surfaceView);
        buttonCapture = findViewById(R.id.captureButton);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"iris onResume");

        startBackgroundThread();

        buttonCapture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    takePicture();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        initSurfaceView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"iris onPause");
        Toast.makeText(this,"onPause",Toast.LENGTH_SHORT).show();
        stopBackgroundThread();
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
                //mSurfaceView.setAlpha(0.9f);

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG,"iris surfaceChanged");
                openCamera();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                mCameraDevice.close();
            }
        });
    }

    private void openCamera() {
        Log.d(TAG,"iris openCamera");

        CameraManager mCameraManager= (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(CamID);
            StreamConfigurationMap scm = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size mPreviewSize = scm.getOutputSizes(ImageFormat.JPEG)[0];
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) return;
            mCameraManager.openCamera(CamID,deviceStateCallback, mHandler);

            //setAspectRatioView(mPreviewSize.getHeight(),mPreviewSize.getWidth());
            
            //set to save image
            mImageReader = ImageReader.newInstance(
                    mPreviewSize.getWidth(),
                    mPreviewSize.getHeight(),
                    ImageFormat.JPEG,
                    7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mainHandler);
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
            mCameraDevice.close();
            mCameraDevice=null;

        }
    };
    public void takePreview() throws CameraAccessException {
        Log.d(TAG,"iris takePreview");
        Surface previewSurface = mSurfaceHolder.getSurface();
        Surface imageSurface = mImageReader.getSurface();

        mCaptReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCaptReqBuilder.addTarget(previewSurface);
        mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,imageSurface),mSessionPreviewStateCallback, mHandler);
    }

    
    private void takePicture() throws CameraAccessException{
        Log.d(TAG,"iris takePicture");

        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            CaptureRequest.Builder cCaptReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            cCaptReqBuilder.addTarget(mImageReader.getSurface());

            Long timeStampLong= System.currentTimeMillis()/1000;
            String ts=timeStampLong.toString();
            file = new File("sdcard/DCIM/irisPic"+"/"+"surface"+ts+".jpg");

            // Use the same AE and AF modes as the preview.
            cCaptReqBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            cCaptReqBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            CaptureRequest mCaptureRequest = cCaptReqBuilder.build();
            mSession.capture(mCaptureRequest,mSessionCaptureCallback,mainHandler);

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
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
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback= new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG,"iris onCaptureProgressed");
            mSession =session;
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d(TAG,"iris onCaptureCompleted");
            mSession=session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG,"iris onImageAvailable");

            Image image = null;
            image=reader.acquireLatestImage();  //[CHECK] reader.acquireLastImage()
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            try {
                save(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                if(image!=null){
                    image.close();
                }
            }
        }
    };
    private void save(byte[] bytes) throws IOException{
        Log.d(TAG,"iris save");
        OutputStream outputStream=null;
        try{
            Log.d(TAG,"iris save-try");
            outputStream=new FileOutputStream(file);
            outputStream.write(bytes);
            Toast.makeText(this,"saved image",Toast.LENGTH_SHORT).show();
        }finally {
            if (null != outputStream){
                outputStream.close();
            }
        }
    }

    private void startBackgroundThread(){
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mainHandler= new Handler(getMainLooper());
    }
    private void stopBackgroundThread(){
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread=null;
            mHandler=null;
            mainHandler=null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

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

    private class SaveImageTask extends AsyncTask<Bitmap,Void,Void> {
        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
            Log.d(TAG,"iris onPostExecute");

            Toast.makeText(MainActivity.this,"saved Image.",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            Bitmap bitmap = null;
            insertImage(getContentResolver(),bitmap,""+System.currentTimeMillis(),"");
            return null;
        }
    }

}
