package com.example.malmal;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.squareup.picasso.Transformation;

public class RotateTransformation implements Transformation {
    private final int rotationAngle;

    public RotateTransformation(int rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        if (rotatedBitmap != source) {
            source.recycle();
        }
        return rotatedBitmap;
    }

    @Override
    public String key() {
        return "rotate" + rotationAngle;
    }

}
