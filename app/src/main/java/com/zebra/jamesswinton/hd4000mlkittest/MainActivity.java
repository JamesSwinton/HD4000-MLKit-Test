package com.zebra.jamesswinton.hd4000mlkittest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.symbol.zebrahud.ZebraHud;
import com.zebra.jamesswinton.hd4000mlkittest.databinding.ActivityMainBinding;

import java.util.List;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity implements ZebraHud.EventListener {

    // Debugging
    private static final String TAG = "MainActivity";

    // DataBinding
    private ActivityMainBinding mDataBinding;

    // HUD
    private ZebraHud mZebraHud = new ZebraHud();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // Init HUD
        mZebraHud.setScale(100);
        mZebraHud.setBrightness(100);
        mZebraHud.setDisplayOn(true);
        mZebraHud.setCameraEnabled(true);
        mZebraHud.setMicrophoneEnabled(true);
        mZebraHud.setOperationMode(ZebraHud.OperationMode.NORMAL);
    }
    @Override
    protected void onStart() {
        super.onStart();
        mZebraHud.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mZebraHud.onResume(this, this);
        mZebraHud.clearDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mZebraHud.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mZebraHud.onStop(this, false);
    }

    /**
     * HUD Callbacks
     */

    @Override
    public void onConnected(Boolean connected) {
        Log.i(TAG, "HUD " + (connected ? "connected" : "disconnected"));

        if (connected) {
            mZebraHud.startCameraCapture();
            Snackbar.make(mDataBinding.coordLayout, "HUD Connected", LENGTH_LONG);
            mDataBinding.capture.setOnClickListener(view -> captureFrame = true);
        } else {
            Snackbar.make(mDataBinding.coordLayout, "HUD Disconnected", LENGTH_LONG);
            mDataBinding.capture.setOnClickListener(view ->
                    Toast.makeText(this, "Please connected HUD", Toast.LENGTH_LONG)
                            .show());
        }
    }

    boolean captureFrame = false;
    boolean readyForNextFrame = true;
    @Override
    public void onImageUpdated(byte[] bytes) {
        Log.i(TAG, "Image Updated");
        readyForNextFrame = true;

        if (captureFrame) {
            processFrame(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            captureFrame = false;
        }
    }

    @Override
    public void onCameraImage(Bitmap bitmap) {
        Log.i(TAG, "Bitmap captured");

        if (readyForNextFrame) {
            // Update View
            mZebraHud.showImage(bitmap);

            // Disable Processing
            readyForNextFrame = false;
        }
    }

    public void processFrame(Bitmap frame) {
        // Create InputImage for MLKit
        InputImage inputImage = InputImage.fromBitmap(frame, 0);

        /// Process
        processBarcode(inputImage, frame);
    }

    public void processText(InputImage inputImage, Bitmap frame) {
        // Get TextRecogniser
        TextRecognizer textRecognizer = TextRecognition.getClient();

        // Process Image
        Task<Text> result = textRecognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    if (visionText != null && visionText.getTextBlocks().size() > 0) {
                        drawResult(frame, visionText);
                    } else {
                        Toast.makeText(this, "No Text Detected",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    public void processBarcode(InputImage inputImage, Bitmap frame) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_QR_CODE)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        Task<List<Barcode>> result = scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes != null && barcodes.size() > 0) {
                        drawResultBarcode(barcodes, frame);
                    } else {
                        Toast.makeText(this, "No Barcode Detected",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Task failed with an exception
                    // ...
                });
    }

    public void drawResult(Bitmap bitmap, Text text) {
        // The Color of the Rectangle to Draw on top of Text
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.parseColor("WHITE"));
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        // The Color of the Rectangle to Draw on top of Text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("WHITE"));
        textPaint.setTextSize(35);

        // Create the Canvas object,
        // Which ever way you do image that is ScreenShot for example, you
        // need the views Height and Width to draw recatngles
        // because the API detects the position of Text on the View
        // So Dimesnions are important for Draw method to draw at that Text
        // Location
        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Loop through each `Block`
        for (Text.TextBlock textBlock : text.getTextBlocks()) {
            for (Text.Line line : textBlock.getLines()) {
                for (Text.Element element : line.getElements()) {
                    // Get the Rectangle / BoundingBox of the word
                    RectF rect = new RectF(element.getBoundingBox());
                    rectPaint.setColor(Color.parseColor("red"));
                    rectPaint.setStrokeWidth(4.0f);

                    // Finally Draw Rectangle/boundingBox around word
                    canvas.drawRect(rect, rectPaint);

                    // Draw Text
                    canvas.drawText(element.getText(),  element.getCornerPoints()[0].x, element.getCornerPoints()[0].y, textPaint);

                    // Set image to the `View`
                    mDataBinding.image.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                }
            }
        }
    }

    private void drawResultBarcode(List<Barcode> barcodes, Bitmap bitmap) {
        // The Color of the Rectangle to Draw on top of Text
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.parseColor("WHITE"));
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4.0f);

        // The Color of the Rectangle to Draw on top of Text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("WHITE"));
        textPaint.setTextSize(35);

        // Create the Canvas object,
        // Which ever way you do image that is ScreenShot for example, you
        // need the views Height and Width to draw recatngles
        // because the API detects the position of Text on the View
        // So Dimesnions are important for Draw method to draw at that Text
        // Location
        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);

        for (Barcode barcode : barcodes) {
            // Get the Rectangle / BoundingBox of the word
            Rect rect = barcode.getBoundingBox();
            rectPaint.setColor(Color.parseColor("red"));
            rectPaint.setStrokeWidth(4.0f);

            // Finally Draw Rectangle/boundingBox around word
            canvas.drawRect(rect, rectPaint);

            // Draw Text
            canvas.drawText(barcode.getRawValue(),  barcode.getCornerPoints()[0].x, barcode.getCornerPoints()[0].y, textPaint);

            // Set image to the `View`
            mDataBinding.image.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                captureFrame = true;
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}