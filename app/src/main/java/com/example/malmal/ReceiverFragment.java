package com.example.malmal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.firebase.database.GenericTypeIndicator;
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
import java.util.ArrayList;
import java.util.Collections;
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
                getPicFromServer();

            }
        });

        binding.buttonInit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext())
                    .setTitle("초기화 진행")
                    .setMessage("처음 실행하는 거 맞습니까? 아니면 큰일 나")
                    .setPositiveButton("당연하죠", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                initialize();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    })
                    .setNegativeButton("아뇨 꺼 주세요", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
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
        List<Double> vector;
        lastImageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot imageSnapshot : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<List<Double>> genericTypeIndicator = new GenericTypeIndicator<List<Double>>() {
                    };
                    List<Double> vector = imageSnapshot.child("featureVec").getValue(genericTypeIndicator);
                    System.out.println(cosineSimilarity(vector, vector));
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
        updatePhotosInfo(getContext(), naverMyBoxFolder, "test_4.json");
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
    public List<Double> readFeatureData(Context context, String filename) {
        List<Double> featureList = new ArrayList<>();

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

            // Parse the JSON array
            JSONArray jsonArray = new JSONArray(sb.toString());

            // Extract the "feature" values
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String featureString = jsonObject.getString("feature");
                // Convert the featureString to a List<Double>
                List<Double> featureValues = convertFeatureStringToList(featureString);
                // Add the feature values to the overall list
                System.out.println(featureValues);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return featureList;
    }

    private List<Double> convertFeatureStringToList(String featureString) {
        List<Double> featureValues = new ArrayList<>();
        try {
            JSONArray featureArray = new JSONArray(featureString);
            for (int i = 0; i < featureArray.length(); i++) {
                double value = featureArray.getDouble(i);
                featureValues.add(value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return featureValues;
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

    public static double cosineSimilarity(List<Double> list1, List<Double> list2) {
        if (list1 == null || list2 == null) {
            throw new IllegalArgumentException("Lists cannot be null.");
        }

        if (list1.size() != list2.size()) {
            throw new IllegalArgumentException("Lists must have the same length.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < list1.size(); i++) {
            dotProduct += list1.get(i) * list2.get(i);
            normA += Math.pow(list1.get(i), 2);
            normB += Math.pow(list2.get(i), 2);
        }

        if (normA == 0 || normB == 0) {
            throw new IllegalArgumentException("One of the vectors is zero, cannot compute similarity.");
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}