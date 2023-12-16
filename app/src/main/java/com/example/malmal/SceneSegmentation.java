package com.example.malmal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.pytorch.MemoryFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


class SceneSegmentation {
    int[] count = new int[21];
    private Context context;
    Module module = null;
    //Bitmap bitmap = null;

    private static final int CLASSNUM = 21;
    private static final int DOG = 12;
    private static final int PERSON = 15;
    private static final int SHEEP = 17;
    public void initialize(Context context){
        this.context = context;

        try {
            module = Module.load(assetFilePath(context, "deeplabv3_scripted.pt"));
        } catch (IOException e) {
            Log.e("SceneSegmentation", "Error loading model!", e);
        }
        Log.d("SceneSegmentation", "model loaded");
    }

    public List<Double> inference(Bitmap bitmap) throws IOException {
        Log.d("SceneSegmentation", "inference started");

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        final float[] inputs = inputTensor.getDataAsFloatArray();

        Map<String, IValue> outTensors =
                module.forward(IValue.from(inputTensor)).toDictStringKey();

        final Tensor outputTensor = outTensors.get("out").toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] intValues = new int[width * height];

        for (int j = 0; j < width; j++) {
            for (int k = 0; k < height; k++) {
                int maxi = 0, maxj = 0, maxk = 0;
                double maxnum = -100000.0;
                for (int i=0; i < CLASSNUM; i++) {
                    if (outputs[i*(width*height) + j*width + k] > maxnum) {
                        maxnum = outputs[i*(width*height) + j*width + k];
                        maxi = i; maxj = j; maxk= k;
                    }
                }
                count[maxi] = count[maxi] + 1;

            }
        }

        List<Double> scoreList = new ArrayList<>();
        double size = width * height;
        for (int i = 0; i < 21; i++){
            double temp = count[i]/size;
            scoreList.add(temp);
        }

        String scoreString = scoreList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        Log.d("SceneSegmentation",scoreString);

        return scoreList;
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

}