package com.example.malmal;

import static com.example.malmal.ReceiverFragment.cosineSimilarity;
import static com.example.malmal.ReceiverFragment.findIndexOfMax;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.malmal.databinding.ActivityOverlayBinding;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OverlayActivity extends AppCompatActivity {
    private ActivityOverlayBinding binding;
    private String jsonFilename = "inferenceData.json";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        binding = ActivityOverlayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String callerName = extras.getString("CALLER_NAME");
            binding.callerName.setText(callerName+"님께 전화가 오고 있습니다.\n일상 얘기를 나누어 보세요!");
        }
        getPicFromServer(new OnImageFetchedListener() {
            @Override
            public void onImageFetched(List<Double> grandmaVector, String imagePath) {
                String maxFilePath = readFeatureData(grandmaVector);
                System.out.println(maxFilePath);
                Bitmap bitmap = BitmapFactory.decodeFile(maxFilePath);
                binding.inferredImage.setImageBitmap(bitmap);

//        binding.inferredImage.setImageResource(R.drawable.your_image1);
//        binding.receivedImage.setImageResource(R.drawable.your_image2);
            }
        });
    }
    public void getPicFromServer(OnImageFetchedListener listener) {
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
                    if (listener != null) {
                        listener.onImageFetched(vector, imagePath);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("getPicFromServer", "failed");
            }
        });

    }
    public String readFeatureData(List<Double> grandmaVector) {

        List<Double> similarity = new ArrayList<>();
        List<String> filePath = new ArrayList<>();

        try {
            FileInputStream fis = this.openFileInput(jsonFilename);
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
            System.out.println("grandMa" + grandmaVector);
            // Extract the "feature" values
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String featureString = jsonObject.getString("feature");
                String pathString = jsonObject.getString("imagePath");

                filePath.add(pathString);
                List<Double> featureValues = convertFeatureStringToList(featureString);
                double score = cosineSimilarity(featureValues, grandmaVector);
                similarity.add(score);
                System.out.println(i +" "+ score);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        int maxIndex = findIndexOfMax(similarity);

        String maxFilePath = filePath.get(maxIndex);

        return maxFilePath;

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
    private void loadImageWithPicasso(String imageUrl) {
        Picasso.get().load(imageUrl).transform(new RotateTransformation(0)).into(binding.receivedImage);
    }
}
