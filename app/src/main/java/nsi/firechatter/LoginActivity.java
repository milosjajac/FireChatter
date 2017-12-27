package nsi.firechatter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;

    private ImageButton fbBtn;
    private ImageButton twitterBtn;
    private ImageButton gplusBtn;
    private EditText emailEt;
    private EditText passwordEt;
    private Button loginBtn;

    private CallbackManager fbCallbackManager;
    private TwitterAuthClient twitterAuthClient;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        goToMainIfAuthenticated();

        fbBtn = findViewById(R.id.login_activity_fb_btn);
        twitterBtn = findViewById(R.id.login_activity_twitter_btn);
        gplusBtn = findViewById(R.id.login_activity_gplus_btn);
        emailEt = findViewById(R.id.login_activity_email_et);
        passwordEt = findViewById(R.id.login_activity_password_et);
        loginBtn = findViewById(R.id.login_activity_login_btn);

        fbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFbLoginClick();
            }
        });
        twitterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTwitterLoginClick();
            }
        });
        gplusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGplusLoginClick();
            }
        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginClick();
            }
        });

        fbCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookLogin(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() { }

            @Override
            public void onError(FacebookException error) { }
        });

        twitterAuthClient = new TwitterAuthClient();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void goToMainIfAuthenticated() {
        boolean loggedIn = auth.getCurrentUser() != null;
        if (loggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        twitterAuthClient.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGplusLogin(task);
        }
    }

    private void onFbLoginClick() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookLogin(AccessToken token) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            goToMainIfAuthenticated();
                            progressDialog.dismiss();
                        } else {
                            Toast.makeText(LoginActivity.this, "Facebook login failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void onTwitterLoginClick() {
        twitterAuthClient.authorize(this, new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                handleTwitterLogin(result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(LoginActivity.this, "Twitter login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleTwitterLogin(TwitterSession session) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AuthCredential credential = TwitterAuthProvider.getCredential(
                session.getAuthToken().token,
                session.getAuthToken().secret
        );

        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            goToMainIfAuthenticated();
                            progressDialog.dismiss();
                        } else {
                            Toast.makeText(LoginActivity.this, "Twitter login failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void onGplusLoginClick() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleGplusLogin(Task<GoogleSignInAccount> task) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                goToMainIfAuthenticated();
                                progressDialog.dismiss();
                            } else {
                                Toast.makeText(LoginActivity.this, "Gplus login failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (ApiException e) {
            Toast.makeText(this, "Gplus login failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void onLoginClick() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_activity_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        final String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException ex) {
                                emailEt.setError(getString(R.string.login_activity_no_user_error));
                                emailEt.requestFocus();
                            } catch (FirebaseAuthInvalidCredentialsException ex) {
                                if (ex.getErrorCode().equals("ERROR_INVALID_EMAIL")) {
                                    emailEt.setError(getString(R.string.login_activity_invalid_email_error));
                                    emailEt.requestFocus();
                                } else if (ex.getErrorCode().equals("ERROR_WRONG_PASSWORD")) {
                                    passwordEt.setError(getString(R.string.login_activity_invalid_password_error));
                                    passwordEt.requestFocus();
                                }
                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

}
