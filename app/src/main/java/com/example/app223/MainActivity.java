package com.example.app223;

import static org.opencv.android.NativeCameraView.TAG;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.core.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.videoio.VideoCapture;


import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends CameraActivity {
    private CameraBridgeViewBase cameraBridgeViewBase;
    private QRCodeDetector bareCodeDetector;
    private SurfaceView cameraView;

    private VideoCapture cameraSource;

    Button btn_scan;
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    Paint boxPaint = new Paint();
    Paint textPain = new Paint();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        OpenCVLoader.initLocal();
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        bareCodeDetector = new QRCodeDetector();
        yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("yolov5s-fp16.tflite");

        yolov5TFLiteDetector.initialModel(this);

        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);

        textPain.setTextSize(50);
        textPain.setColor(Color.GREEN);
        textPain.setStyle(Paint.Style.FILL);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(new CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                cameraSource = new VideoCapture(0);
                if (!cameraSource.isOpened()) {
                    Log.e("Camera", "Error opening camera");
                    // Обработка ошибки
                } else {
                    Log.d("Camera", "Camera opened successfully");
                }

            }

            @Override
            public void onCameraViewStopped() {
                // Остановка камеры
                cameraSource.release();
            }
            @Override
            public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
                Mat frame = inputFrame.rgba();
                // ... (Ваш код для обработки кадра, например, изменение размера)
                try {
                    // Преобразуем Mat в Bitmap
                    Bitmap bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(frame, bitmap);

                    // Выполняем детектирование с помощью YOLOv5
                    ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(bitmap);

                    // Рисуем прямоугольники и метки на Bitmap
                    Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);

                    for (Recognition recognition : recognitions) {
                        if (recognition.getConfidence() > 0.4) {
                            RectF location = recognition.getLocation();
                            canvas.drawRect(location, boxPaint);
                            canvas.drawText(recognition.getLabelName() + ":" + recognition.getConfidence(), location.left, location.top, textPain);
                        }
                    }

                    // Преобразуем Bitmap обратно в Mat
                    Mat newFrame = new Mat();
                    Utils.bitmapToMat(mutableBitmap, newFrame);

                    return newFrame;

                } catch (Exception e) {
                    Log.e(TAG, "Error processing frame: " + e.getMessage());
                    return frame; // Возвращаем исходную матрицу в случае ошибки
                }

            }
        });
        cameraBridgeViewBase.enableView();
    }
    @Override
    protected List<?extends CameraBridgeViewBase> getCameraViewList(){
        return Collections.singletonList(cameraBridgeViewBase);
    }

}