package com.example.potteryai;

import android.content.Context;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAnalyzer {
    private Interpreter tflite;
    private List<String> labels;

    public ImageAnalyzer(Context context, String modelFileName, String labelsFileName) throws IOException {
        // Load the model and labels using the provided context and file names
        tflite = new Interpreter(loadModelFile(context, modelFileName));
        labels = loadLabels(context, labelsFileName);
    }

    private MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelFileName).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelFileName).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelFileName).getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(Context context, String labelsFileName) throws IOException {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsFileName)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    public Map<String, Float> analyzeImage(float[][][][] input) {
        // Create a 2D array for the output to handle batch size
        float[][] output = new float[1][labels.size()]; // Batch size of 1

        // Run inference with TensorFlow Lite
        tflite.run(input, output);

        // Extract results from the first (and only) batch
        Map<String, Float> tagProbabilities = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            tagProbabilities.put(labels.get(i), output[0][i]); // Access the first batch
        }

        return tagProbabilities;
    }


}



