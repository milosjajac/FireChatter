package nsi.firechatter.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import nsi.firechatter.R;

public class ResetPasswordActivity extends AppCompatActivity{
    private EditText emailEt;
    private Button resetPasswordBtn;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        Intent i = getIntent();
        email = i.getStringExtra("Email");

        emailEt = findViewById(R.id.reset_password_activity_email_et);
        emailEt.setText(email);
        emailEt.setSelection(email.length());

        resetPasswordBtn = findViewById(R.id.reset_password_activity_reset_btn);
        resetPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetPasswordClick();
            }
        });
    }

    private void onResetPasswordClick() {
        if (!isFieldValid(emailEt)) {
            return;
        }

        final String email = emailEt.getText().toString().trim();
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.reset_password_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        final FirebaseAuth auth = FirebaseAuth.getInstance();

        (auth.sendPasswordResetEmail(email)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();

                if (task.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Email is sent", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    i.putExtra("Reset_Email", email);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
