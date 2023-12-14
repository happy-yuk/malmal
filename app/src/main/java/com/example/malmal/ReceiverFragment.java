package com.example.malmal;

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

import com.example.malmal.databinding.FragmentSecondBinding;
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

import java.io.File;

public class ReceiverFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
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
                File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File naverMyBoxFolder = new File(downloadsFolder, "NAVER MYBOX");

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

    private void initialize(Uri pathname) {

    }
}