package com.jonty.garagenetwork;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

public class Authentication extends AppCompatActivity {
    private final int AUTH_RC = 12;
    private TextView authHeading, authForgotPassword, authMessage, authText;
    private Button authButton;
    private ImageButton passwordHideShow;
    private EditText authEmail, authPassword;
    private boolean passwordVisible;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Authentication.this);
        editor = sharedPreferences.edit();
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        authHeading = findViewById(R.id.authHeading);
        authForgotPassword = findViewById(R.id.authForgotPassword);
        authMessage = findViewById(R.id.authMessage);
        authText = findViewById(R.id.authText);
        authButton = findViewById(R.id.authButton);
        passwordHideShow = findViewById(R.id.passwordHideShow);
        authEmail = findViewById(R.id.authEmail);
        authPassword = findViewById(R.id.authPassword);
        auth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(Authentication.this);
        database = FirebaseDatabase.getInstance();

        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        passwordHideShow.setVisibility(View.GONE);
        passwordVisible = false;
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Please wait while we login to your account...");

        authPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0)
                    passwordHideShow.setVisibility(View.VISIBLE);
                else
                    passwordHideShow.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        if (auth.getCurrentUser() != null) {
            auth.signOut();
            googleSignInClient.signOut();
        }
    }

    public void changeAuth(View view) {
        if (authText.getText().toString().compareTo("Sign up") == 0) {
            //change to sign up mode
            authHeading.setText("Sign Up");
            authForgotPassword.setVisibility(View.INVISIBLE);
            authMessage.setText("Already have an account?");
            authText.setText("Login");
            authButton.setText("Sign up");
            progressDialog.setTitle("Sign up");
            progressDialog.setMessage("Please wait while we setup your account...");
        } else {
            //change to login mode
            authHeading.setText("Login");
            authForgotPassword.setVisibility(View.VISIBLE);
            authMessage.setText("Don't have account?");
            authText.setText("Sign up");
            authButton.setText("Login");
            progressDialog.setTitle("Login");
            progressDialog.setMessage("Please wait while we login to your account...");
        }
        authEmail.setText("");
        authPassword.setText("");
    }

    public void forgotPassword(View view) {
        final String email = authEmail.getText().toString().trim();
        PopupMessage popupMessage;
        if (email.isEmpty()) {
            popupMessage = new PopupMessage("Email", "Please enter email!", this);
            popupMessage.show(view);
        } else {
            auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    PopupMessage popupMessage1;
                    if (task.isSuccessful()) {
                        popupMessage1 = new PopupMessage("Reset password", "Reset password link send to your provided email" +
                                "use the given link to reset password!", Authentication.this);
                    } else
                        popupMessage1 = new PopupMessage("Something went wrong!", task.getException().getMessage(), Authentication.this);
                    popupMessage1.show(view);
                }
            });
        }
    }

    public void showHidePassword(View view) {
        if (passwordVisible) {
            //hide the password
            authPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordHideShow.setImageResource(R.drawable.ic_baseline_remove_red_eye_24);
            passwordVisible = false;
        } else {
            //show the password
            authPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordHideShow.setImageResource(R.drawable.ic_baseline_remove_black_eye_24);
            passwordVisible = true;
        }
        Typeface typeface = ResourcesCompat.getFont(this, R.font.acme);
        authPassword.setTypeface(typeface);
        authPassword.setSelection(authPassword.length());
    }

    public void authWithPhone(View view) {
        Intent intent = new Intent(this, SendOTP.class);
        startActivity(intent);
    }

    public void authWithGoogle(View view) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, AUTH_RC);
    }

    public void authWithEmail(View view) {
        progressDialog.show();
        final String email = authEmail.getText().toString().trim();
        final String password = authPassword.getText().toString().trim();
        PopupMessage popupMessage;
        if (email.isEmpty()) {
            popupMessage = new PopupMessage("Email", "Please enter email!", this);
            popupMessage.show(view);
            progressDialog.dismiss();
        } else if (password.isEmpty()) {
            popupMessage = new PopupMessage("Password", "Please enter password!", this);
            popupMessage.show(view);
            progressDialog.dismiss();
        } else {
            if (authText.getText().toString().compareTo("Sign up") == 0) {
                //perform login action
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            editor.putString("PersonName", email);
                            editor.commit();
                            loginMPI();
                        } else {
                            PopupMessage popupMessage1 = new PopupMessage("Something went Wrong!", task.getException().getMessage() +
                                    "\n\nHint: Try google login for same account!", Authentication.this);
                            popupMessage1.show(view);
                        }
                        progressDialog.dismiss();
                    }
                });
            } else {
                //perform signup action
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            editor.putString("PersonName", email);
                            editor.commit();
                            loginMPI();
                        } else {
                            PopupMessage popupMessage1 = new PopupMessage("Something went Wrong!", task.getException().getMessage(), Authentication.this);
                            popupMessage1.show(view);
                        }
                        progressDialog.dismiss();
                    }
                });
            }
        }
    }

    public void loginMPI() {
        database.getReference().child(auth.getUid()).child("MPIN").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String lastPin = snapshot.getValue(String.class);
                    Intent intent;
                    if (lastPin.compareTo("NULL") == 0)
                        intent = new Intent(Authentication.this, SetPin.class);
                    else
                        intent = new Intent(Authentication.this, VerifyPin.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    database.getReference().child(auth.getUid()).child("MPIN").setValue("NULL");
                    Intent intent = new Intent(Authentication.this, SetPin.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Authentication.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_RC) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acc = task.getResult(ApiException.class);
                AuthCredential authCredential = GoogleAuthProvider.getCredential(acc.getIdToken(), null);
                auth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            if (account != null) {
                                String personName = account.getDisplayName();
                                editor.putString("PersonName", personName);
                                editor.commit();
                                loginMPI();
                            }
                        } else {
                            PopupMessage popupMessage = new PopupMessage("Something went wrong", task.getException().getMessage(), Authentication.this);
                            popupMessage.show(findViewById(android.R.id.content).getRootView());
                        }
                    }
                });
            } catch (ApiException e) {
                PopupMessage popupMessage = new PopupMessage("Something went wrong", "APIException: " + e.getMessage(), Authentication.this);
                popupMessage.show(findViewById(android.R.id.content).getRootView());
            }
        }
    }
}