package com.example.malmal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.malmal.databinding.FragmentReceiverBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ReceiverFragment extends Fragment {
    private Context context;
    private FragmentReceiverBinding binding;
    private SceneSegmentation sceneSegmentation = new SceneSegmentation();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentReceiverBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ReceiverFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
        binding.buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    initialize();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                getPicFromServer();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    public void getPicFromServer() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://malmal-f11e9-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference databaseRef = database.getReference("image_metadata");
        Query lastImageQuery = databaseRef.orderByChild("timestamp").limitToLast(1);

        lastImageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot imageSnapshot : dataSnapshot.getChildren()) {
                    String imagePath = imageSnapshot.child("path").getValue(String.class);
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference imageRef = storage.getReference().child(imagePath);

                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // 이제 uri를 사용하여 Picasso로 이미지 로드
                            loadImageWithPicasso(uri.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // 오류 처리
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("getPicFromServer", "failed");
            }
        });

    }
    private void loadImageWithPicasso(String imageUrl) {
        Picasso.get().load(imageUrl).transform(new RotateTransformation(90)).into(binding.receivedImage);
    }

    private void initialize() throws IOException {
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File naverMyBoxFolder = new File(downloadsFolder, "NAVER MYBOX");
        updatePhotosInfo(getContext(), naverMyBoxFolder, "test_2.json");

    }

    public void updatePhotosInfo(Context context, File directory, String jsonFilename) throws IOException {
        this.context = context;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String imagePath = file.getAbsolutePath();
                    double[] dummyFeature = new double[] {1.0};

                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);


                    Log.d("image", "null"+imagePath);

                    Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, 256, 256, false);
                    sceneSegmentation.initialize(context);
                    List<Double> score = sceneSegmentation.inference(bitmapResized);
                    updateData(context, jsonFilename, "imagePath", imagePath, "feature", score);

                }
            }
        }
        readData(context, jsonFilename);
    }

    private boolean isImageFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".png");
    }

    public void updateData(Context context, String filename, String newLabel1, String newData1, String newLabel2, List<Double> newData2) {
        try {
            // 기존 JSON 데이터 불러오기
            JSONArray dataArray = loadData(context, filename);
            if (dataArray == null) {
                dataArray = new JSONArray();
            }
           // System.out.println(dataArray.toString(4)); // Prints the JSON Object with indentation for readability
            // 새 엔트리 생성
            JSONObject newEntry = new JSONObject();
            newEntry.put(newLabel1, newData1);
            newEntry.put(newLabel2, newData2);

            // 엔트리를 JSONArray에 추가
            dataArray.put(newEntry);

            // 업데이트된 데이터 저장
            saveData(context, filename, dataArray);
            Log.d("json", "data saving finished");

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
    public String readData(Context context, String filename) {
        String result = null;
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(result);
        return result;
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