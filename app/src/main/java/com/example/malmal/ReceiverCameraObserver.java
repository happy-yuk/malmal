package com.example.malmal;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiverCameraObserver extends ContentObserver {
    private Context context;
    private long lastProcessedTimestamp = 0;
    private static final long DELAY = 1000; // 1초 지연
    private long SOME_THRESHOLD = 3000;
    public ReceiverCameraObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastProcessedTimestamp > SOME_THRESHOLD) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("ReceiverCameraObserver", "Media change detected: " + uri);
                    lastProcessedTimestamp = currentTimestamp;
                    double[] dummyData = new double[] {1.0}; // dummy data
                    // 이미지에 모델 적용하는 코드 + 로컬에 데이터 저장
                    savePicOnLocal(context, uri.toString(), dummyData);
                }
            }, DELAY);
        }
    }

    public void savePicOnLocal(Context context, String imageName, double[] feature) {
        // replace above line to the model application code
        updateData(context, "malmal_preprocessed_array.json", "imagePath", imageName,"feature", feature);
    }

    public void updateData(Context context, String filename, String newLabel1, String newData1, String newLabel2, double[] newData2) {
        try {
            // 기존 JSON 데이터 불러오기
            JSONArray dataArray = loadData(context, filename);
            if (dataArray == null) {
                dataArray = new JSONArray();
            }
            System.out.println(dataArray.toString(4)); // Prints the JSON Object with indentation for readability
            // 새 엔트리 생성
            JSONObject newEntry = new JSONObject();
            newEntry.put(newLabel1, newData1);
            newEntry.put(newLabel2, newData2);

            // 엔트리를 JSONArray에 추가
            dataArray.put(newEntry);

            // 업데이트된 데이터 저장
            saveData(context, filename, dataArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveData(Context context, String filename, JSONArray data) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(data.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public JSONArray loadData(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            fis.close();
            return new JSONArray(sb.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


}