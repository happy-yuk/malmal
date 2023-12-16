package com.example.malmal;

import java.util.List;

public interface OnImageFetchedListener {
    void onImageFetched(List<Double> vector, String imagePath);
}