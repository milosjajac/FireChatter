package nsi.firechatter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEt;
    private EditText emailEt;
    private EditText passwordEt;
    private EditText repeatPasswordEt;
    private Button registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEt = findViewById(R.id.register_activity_name_et);
        emailEt = findViewById(R.id.register_activity_email_et);
        passwordEt = findViewById(R.id.register_activity_password_et);
        repeatPasswordEt = findViewById(R.id.register_activity_repeat_password_et);
        registerBtn = findViewById(R.id.register_activity_register_btn);

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegisterClick();
            }
        });
    }

    private void onRegisterClick() {
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();
        String repeatPassword = repeatPasswordEt.getText().toString().trim();

        if (!password.equals(repeatPassword)) {
            repeatPasswordEt.setError(getString(R.string.register_activity_mismatch_password_error));
            repeatPasswordEt.requestFocus();
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
                            String newUserId = auth.getCurrentUser().getUid();

                            progressDialog.dismiss();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
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
}
