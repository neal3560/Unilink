package com.teamxod.unilink;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickClick;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.ByteArrayOutputStream;
import java.net.URL;

public class InitiateProfile extends AppCompatActivity implements IPickResult {
    private final Uri MALE_PROFILE_PIC = Uri.parse("https://firebasestorage.googleapis.com/v0/b/fir-project-7cabd.appspot.com/o/male.png?alt=media&token=02a80321-a6ae-4194-af4d-bd658de9348f");
    private final Uri FEMALE_PROFILE_PIC = Uri.parse("https://firebasestorage.googleapis.com/v0/b/fir-project-7cabd.appspot.com/o/female.png?alt=media&token=69a0c9c9-eda5-481d-9043-b718d899121b");

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String uid;
    private String name;
    private Uri picture;
    private String gender;
    private String yearGraduate;
    private String description;

    private ImageView mProfilePic;
    private EditText mEditName;
    private TextView mEmail;
    private Spinner mGenderSpinner;
    private Spinner mYearSpinner;
    private EditText mDescription;
    private Bitmap imageBitmap;
    private CardView mSave;
    private PickImageDialog dialog;

    private final int PICK_IMAGE_REQUEST = 71;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initiate_profile);

        mProfilePic = findViewById(R.id.profile_pic);
        mEditName = findViewById(R.id.edit_name);
        mEmail = findViewById(R.id.email);
        mGenderSpinner = findViewById(R.id.gender_spinner);
        mYearSpinner = findViewById(R.id.year_spinner);
        mDescription = findViewById(R.id.description);
        mSave = findViewById(R.id.save);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth != null && mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();

            // using google login
            if(getLoginmethod()) {
                mEditName.setText(mAuth.getCurrentUser().getDisplayName());
                Uri mPhoto = mAuth.getCurrentUser().getPhotoUrl();
                if (mPhoto != null) {
                    Glide.with(this)
                            .load(mPhoto)
                            .apply(RequestOptions.circleCropTransform())
                            .into(mProfilePic);
                }
            }


            mEmail.setText(mAuth.getCurrentUser().getEmail());
        }

        //choose picture from camera or gallery
        mProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = PickImageDialog.build(new PickSetup()).show(InitiateProfile.this);

            }
        });


        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = mEditName.getText().toString();
                gender = mGenderSpinner.getSelectedItem().toString();
                if(picture == null) {
                    //FIXME check gender and set default pic
                    if (gender.equals("Female")) {
                        picture = FEMALE_PROFILE_PIC;
                    }
                    else {
                        picture = MALE_PROFILE_PIC;
                    }
                }
                yearGraduate = mYearSpinner.getSelectedItem().toString();
                description = mDescription.getText().toString();
                writeNewUser(name, picture.toString(),gender,yearGraduate,description);
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .setPhotoUri(picture)
                        .build();
                mAuth.getCurrentUser().updateProfile(profileUpdates);
                Intent mainIntent = new Intent(InitiateProfile.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });


    }

    //PickImage Plug-in
    //choose picture from camera or gallery
    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            //If you want the Uri.
            //Mandatory to refresh image from Uri.
            picture = r.getUri();
            mProfilePic.setImageURI(picture);


            //Setting the real returned image.
            //getImageView().setImageURI(r.getUri());

            //If you want the Bitmap.
            //getImageView().setImageBitmap(r.getBitmap());

            //Image path
            //r.getPath();
        } else {
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void writeNewUser(String name, String picture, String gender, String yearGraduate,
                              String description) {
        User user = new User(name, picture, gender, yearGraduate, description);

        mDatabase.child("Users").child(uid).setValue(user);
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private boolean getLoginmethod() {
        if(mAuth != null && mAuth.getCurrentUser() != null) {
            for (UserInfo user : mAuth.getCurrentUser().getProviderData()) {
                Log.d("TAG",user.getProviderId());
                if (user.getProviderId().equals("google.com")) { // google login
                    return true;
                }
            }
            return false;
        } else {
            return true; // not login, no profile photo
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


}
