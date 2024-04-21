package com.example.app223;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        OpenCVLoader.initLocal();
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        bareCodeDetector = new QRCodeDetector();
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
                Mat gray = new Mat();
                Mat gradX = new Mat();
                Mat gradY = new Mat();
                Mat gradient = new Mat();
                Mat blurred = new Mat();
                Mat thresh = new Mat();
                Mat closed = new Mat();
                MatOfRect qrCodes = new MatOfRect();

                // Преобразование в оттенки серого
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);
                // Рассчитываем градиенты по X и Y
                Imgproc.Sobel(gray, gradX, CvType.CV_32F, 1, 0, -1);
                Imgproc.Sobel(gray, gradY, CvType.CV_32F, 0, 1, -1);
                Core.subtract(gradX, gradY, gradient);
                Core.convertScaleAbs(gradient, gradient);

                // Размытие
                Imgproc.blur(gradient, blurred, new Size(9, 9));

                // Бинаризация
                Imgproc.threshold(blurred, thresh, 200, 255, Imgproc.THRESH_BINARY);

                // Закрытие
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 7));
                Imgproc.morphologyEx(thresh, closed, Imgproc.MORPH_CLOSE, kernel);
                // Распознавание QR-кодов
                bareCodeDetector.detectMulti(thresh, qrCodes);

                // Поиск контуров на бинаризированном изображении
                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(thresh, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // Сортировка контуров по площади в порядке убывания
                contours.sort((c1, c2) -> Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

                // Отрисовка самого большого контура (предполагая, что это QR-код)
                if (!contours.isEmpty()) {
                    MatOfPoint largestContour = contours.get(0);
                    Imgproc.drawContours(frame, Collections.singletonList(largestContour), -1, new Scalar(0, 255, 0), 2);
                }

                return frame;

            }
        });
        cameraBridgeViewBase.enableView();
    }
    @Override
    protected List<?extends CameraBridgeViewBase> getCameraViewList(){
        return Collections.singletonList(cameraBridgeViewBase);
    }
}