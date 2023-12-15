package com.example.malmal;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.malmal.databinding.FragmentSenderBinding;
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
import java.util.Map;

public class SenderFragment extends Fragment {

    private FragmentSenderBinding binding; // 처음 만들어 질 때의 이름이 FirstFragment였는데, 이 부분은 수정이 안 됨
    private ActivityResultLauncher<String> mGetContent;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSenderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SenderFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
        binding.buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Gallery", "tired");
                sendPicToServer();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void sendPicToServer() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        Uri selectedImageUri = getLatestLocalPhoto();
        Log.d("selectedImageUri", selectedImageUri.toString());
        try {
            Bitmap originalBitmap = getBitmapFromUri(selectedImageUri);
            Bitmap resizedBitmap = resizeImageToMaxSize(originalBitmap, 1024);
            Uri resizedImageUri = saveBitmapAndGetUri(resizedBitmap);

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
//                            imageData.put("featureVec", modelVectorData);  // Add model-processed vector

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

    private Uri getLatestLocalPhoto() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.Images.Media._ID};
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, sortOrder)) {
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
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContext().getContentResolver().openFileDescriptor(uri, "r");
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
    private Uri saveBitmapAndGetUri(Bitmap bitmap) {
        // 임시 파일 생성
        String filename = "temp_image_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
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