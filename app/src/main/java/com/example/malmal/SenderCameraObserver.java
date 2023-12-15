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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenderCameraObserver extends ContentObserver {
    private Context context;
    private long lastProcessedTimestamp = 0;
    private long SOME_THRESHOLD = 60000;

    private SceneSegmentation sceneSegmentation = new SceneSegmentation();

    public SenderCameraObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastProcessedTimestamp > SOME_THRESHOLD) {
            Log.d("SenderCameraObserver", "Media change detected: " + uri.toString());
            lastProcessedTimestamp = currentTimestamp;
            // sendPicToServer 내부에서 모델 적용하기
            Log.d("snedPicToServer", "Enter sendPicToServer");
            sendPicToServer(context);
        }
    }

    public void sendPicToServer(Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        Uri selectedImageUri = getLatestLocalPhoto(context);
        Log.d("selectedImageUri", selectedImageUri.toString());
        try {
            Bitmap originalBitmap = getBitmapFromUri(selectedImageUri, context);

            Bitmap resizedBitmap = resizeImageToMaxSize(originalBitmap, 1024);
            Bitmap bitmap = BitmapFactory.decodeStream(context
                    .getContentResolver().openInputStream(selectedImageUri));
            Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, 256, 256, false);
            //sceneSegmentation.initialize(context);
            List<Double> score = sceneSegmentation.inference(bitmapResized);

            Uri resizedImageUri = saveBitmapAndGetUri(resizedBitmap, context);

            String filename = "image_" + System.currentTimeMillis();
            StorageReference fileRef = storageRef.child("images/" + filename);
            fileRef.putFile(resizedImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // 업로드 성공
                            FirebaseDatabase database = FirebaseDatabase.getInstance("https://malmal-f11e9-default-rtdb.asia-southeast1.firebasedatabase.app");
                            DatabaseReference databaseRef = database.getReference("image_metadata");

                            String imageId = databaseRef.push().getKey();
                            Map<String, Object> imageData = new HashMap<>();
                            imageData.put("path", fileRef.getPath());
                            imageData.put("timestamp", ServerValue.TIMESTAMP);
                            imageData.put("featureVec", score);

                            databaseRef.child(imageId).setValue(imageData);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // 업로드 실패
                            Log.d("sendPicToServer", "failure");
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Uri getLatestLocalPhoto(Context context) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.Images.Media._ID};
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        } catch (Exception e) {
            Log.d("getLatestLocalPhoto", "error");
        }
        return null;
    }
    private Bitmap getBitmapFromUri(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private Bitmap resizeImageToMaxSize(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            // 가로가 세로보다 긴 경우
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // 세로가 가로보다 긴 경우
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(original, width, height, true);
    }
    private Uri saveBitmapAndGetUri(Bitmap bitmap, Context context)  {
        // 임시 파일 생성
        String filename = "temp_image_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
        try {
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out); // JPEG 형식으로 압축
            out.flush();
            out.close();
            return Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}