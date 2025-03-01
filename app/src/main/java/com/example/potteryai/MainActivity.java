package com.example.potteryai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView resultTextView;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.textView);

        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button analyzeImageButton = findViewById(R.id.analyzeImageButton);

        // Set up button to select an image
        selectImageButton.setOnClickListener(v -> openImageSelector());

        // Set up button to analyze the selected image
        analyzeImageButton.setOnClickListener(v -> {
            if (selectedImage != null) {
                analyzeImage();
            } else {
                Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Open the image selector
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageSelectorLauncher.launch(intent);
    }

    // Handle the result of the image selector
    private final ActivityResultLauncher<Intent> imageSelectorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        // Load the selected image as a Bitmap
                        selectedImage = BitmapFactory.decodeStream(getContentResolver()
                                .openInputStream(result.getData().getData()));
                        imageView.setImageBitmap(selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Analyze the selected image
    private void analyzeImage() {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Preprocess the image
                float[][][][] inputImage = preprocessImage(selectedImage);

                // Initialize ImageAnalyzer and analyze
                ImageAnalyzer analyzer = new ImageAnalyzer(this, "model.tflite", "labels.txt");
                Map<String, Float> tagProbabilities = analyzer.analyzeImage(inputImage);

                // Update the UI
                runOnUiThread(() -> {
                    updateUI(tagProbabilities);
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error during analysis: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }




    // Preprocess the image
    private float[][][][] preprocessImage(Bitmap bitmap) {
        int targetWidth = 224;
        int targetHeight = 224;

        // Resize the image to 224x224
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);

        // Create a 4D tensor
        float[][][][] input = new float[1][targetHeight][targetWidth][3];
        int[] intValues = new int[targetWidth * targetHeight];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        for (int i = 0; i < intValues.length; i++) {
            int pixel = intValues[i];
            input[0][i / targetWidth][i % targetWidth][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red
            input[0][i / targetWidth][i % targetWidth][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green
            input[0][i / targetWidth][i % targetWidth][2] = (pixel & 0xFF) / 255.0f;         // Blue
        }

        return input;
    }




    // Update the UI with the analysis results
    private void updateUI(Map<String, Float> tagProbabilities) {
        StringBuilder resultText = new StringBuilder();

        for (Map.Entry<String, Float> entry : tagProbabilities.entrySet()) {
            String tag = entry.getKey();
            float probability = entry.getValue();
            resultText.append(tag).append(": ").append(String.format("%.2f", probability * 100)).append("%\n");
        }

        // Display the results in the TextView
        resultTextView.setText(resultText.toString());
    }
}
