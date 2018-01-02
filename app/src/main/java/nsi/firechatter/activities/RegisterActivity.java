package nsi.firechatter.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import nsi.firechatter.R;
import nsi.firechatter.models.User;
import retrofit2.http.Url;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_STORAGE_PERMISSION = 1;
    private static final int RC_CHOOSE_IMAGE = 2;

    private EditText nameEt;
    private EditText emailEt;
    private EditText passwordEt;
    private EditText repeatPasswordEt;
    private ImageView avatarImg;
    private TextView avatarErrorTv;
    private Button avatarBtn;
    private Button registerBtn;

    private String selectedAvatarLocalPath;

    private DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");
    private StorageReference avatarsStorageRef = FirebaseStorage.getInstance().getReference().child("avatars");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEt = findViewById(R.id.register_activity_name_et);
        emailEt = findViewById(R.id.register_activity_email_et);
        passwordEt = findViewById(R.id.register_activity_password_et);
        repeatPasswordEt = findViewById(R.id.register_activity_repeat_password_et);
        avatarImg = findViewById(R.id.register_activity_avatar_img);
        avatarErrorTv = findViewById(R.id.register_activity_avatar_error_txt);
        avatarBtn = findViewById(R.id.register_activity_avatar_btn);
        registerBtn = findViewById(R.id.register_activity_register_btn);

        avatarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAvatarButtonClick();
            }
        });
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegisterClick();
            }
        });
    }

    private void onAvatarButtonClick() {
        checkReadStoragePermission();
    }

    private void checkReadStoragePermission() {
        final String storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int userPermission = ContextCompat.checkSelfPermission(RegisterActivity.this, storagePermission);
        boolean permissionGranted = userPermission == PackageManager.PERMISSION_GRANTED;

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{storagePermission},
                    RC_STORAGE_PERMISSION);
        } else {
            onStoragePermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onStoragePermissionGranted() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");

        startActivityForResult(galleryIntent, RC_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedAvatarLocalPath = getRealPathFromURI(data.getData());
            }

            avatarImg.setBackground(null);
            Glide.with(this)
                    .load(selectedAvatarLocalPath)
                    .into(avatarImg);
            avatarErrorTv.setError(null);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = this.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void onRegisterClick() {
        if (!isFieldValid(nameEt) || !isFieldValid(emailEt) ||
                !isFieldValid(passwordEt) || !isFieldValid(repeatPasswordEt)) {
            return;
        }

        final String name = nameEt.getText().toString().trim();
        final String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();
        String repeatPassword = repeatPasswordEt.getText().toString().trim();

        if (!password.equals(repeatPassword)) {
            repeatPasswordEt.setError(getString(R.string.register_activity_mismatch_password_error));
            repeatPasswordEt.requestFocus();
            return;
        }

        if (selectedAvatarLocalPath == null || selectedAvatarLocalPath.isEmpty()) {
            avatarErrorTv.setError(getString(R.string.register_activity_no_avatar_error));
            avatarErrorTv.requestFocus();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.register_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        final FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            final FirebaseUser user = auth.getCurrentUser();
                            final String newUserId = user.getUid();
                            final String avatarFileName = newUserId + ".jpg";

                            avatarsStorageRef.child(avatarFileName).putFile(Uri.fromFile(new File(selectedAvatarLocalPath)))
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            final Uri imageUrl = taskSnapshot.getDownloadUrl();
                                            User newUser = new User(email, name, imageUrl.toString());

                                            usersDbRef.child(newUserId).setValue(newUser)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                                    .setDisplayName(name)
                                                                    .setPhotoUri(imageUrl)
                                                                    .build();

                                                            user.updateProfile(profileUpdates)
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                progressDialog.dismiss();
                                                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                                                startActivity(intent);
                                                                                finish();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    });
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                if (e.getErrorCode().equals("ERROR_INVALID_EMAIL")) {
                                    emailEt.setError(getString(R.string.login_activity_invalid_email_error));
                                    emailEt.requestFocus();
                                } else if (e.getErrorCode().equals("ERROR_WEAK_PASSWORD")) {
                                    passwordEt.setError(getString(R.string.register_activity_weak_password_error));
                                    passwordEt.requestFocus();
                                }
                            } catch (FirebaseAuthUserCollisionException e) {
                                emailEt.setError(getString(R.string.register_activity_email_exists_error));
                                emailEt.requestFocus();
                            } catch (Exception e) {
                                Toast.makeText(RegisterActivity.this, "Exception: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private boolean isFieldValid(EditText et) {
        if (et.getText().toString().trim().isEmpty()) {
            et.setError(getString(R.string.login_activity_required_field_error));
            et.requestFocus();
            return false;
        }
        return true;
    }
}
