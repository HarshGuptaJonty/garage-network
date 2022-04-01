package com.jonty.garagenetwork;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class VerifyPin extends AppCompatActivity {
    private String lastPin;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private TextView textAccount;
    private EditText inputPinC1, inputPinC2, inputPinC3, inputPinC4, inputPinC5, inputPinC6;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private GoogleSignInClient googleSignInClient;
    private ProgressDialog progressDialog;
    private InputMethodManager inputMethodManager;
    private ImageButton pinHideShow, fingerPrintIcon;
    private LinearLayout biometric;
    private boolean pinVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_pin);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(VerifyPin.this);
        editor = sharedPreferences.edit();
        long currentTimeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long previousTimeStamp = sharedPreferences.getLong("TimeStamp", 0);
        if (sharedPreferences.getBoolean("Activate Developer", false) && currentTimeStamp > previousTimeStamp + 86400) {
            editor.putBoolean("Activate Developer", false);
            editor.commit();
        }
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(this, Authentication.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        database = FirebaseDatabase.getInstance();
        textAccount = findViewById(R.id.textAccount);
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        inputPinC1 = findViewById(R.id.inputPinC1);
        inputPinC2 = findViewById(R.id.inputPinC2);
        inputPinC3 = findViewById(R.id.inputPinC3);
        inputPinC4 = findViewById(R.id.inputPinC4);
        inputPinC5 = findViewById(R.id.inputPinC5);
        inputPinC6 = findViewById(R.id.inputPinC6);

        setupMPINInputs(inputPinC1, inputPinC2);
        setupMPINInputs(inputPinC2, inputPinC3);
        setupMPINInputs(inputPinC3, inputPinC4);
        setupMPINInputs(inputPinC4, inputPinC5);
        setupMPINInputs(inputPinC5, inputPinC6);

        setupMPINInputsReverse(inputPinC1, inputPinC2);
        setupMPINInputsReverse(inputPinC2, inputPinC3);
        setupMPINInputsReverse(inputPinC3, inputPinC4);
        setupMPINInputsReverse(inputPinC4, inputPinC5);
        setupMPINInputsReverse(inputPinC5, inputPinC6);

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        pinHideShow = findViewById(R.id.pinHideShow);
        biometric = findViewById(R.id.biometric);
        fingerPrintIcon = findViewById(R.id.fingerPrintIcon);

        inputPinC6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty()) {
                    View view = findViewById(android.R.id.content).getRootView();
                    login(view);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        if (getIntent().hasExtra("ChangePin")) {
            biometric.setVisibility(View.GONE);
            lastPin = getIntent().getStringExtra("ChangePin");
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setTitle("Loading...");
            progressDialog.setMessage("Please wait while we index your details...");
            progressDialog.show();
            database.getReference().child(auth.getUid()).child("MPIN").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        lastPin = snapshot.getValue(String.class);
                        progressDialog.dismiss();
                        inputPinC1.requestFocus();
                        if (lastPin.compareTo("NULL") == 0) {
                            Intent intent = new Intent(VerifyPin.this, SetPin.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    } else {
                        lastPin = "";
                        Intent intent = new Intent(VerifyPin.this, SetPin.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
        pinVisible = false;
        textAccount.setText(sharedPreferences.getString("PersonName", ""));
        if (sharedPreferences.getBoolean("Activate Biometric", false)) {
            //create the BiometricManager and check if the user can use fingerprint or now
            BiometricManager biometricManager = BiometricManager.from(this);
            switch (biometricManager.canAuthenticate()) {
                case BiometricManager.BIOMETRIC_SUCCESS: //yes we can use the sensor
                    if (!getIntent().hasExtra("ChangePin"))
                        biometric.setVisibility(View.VISIBLE);
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE: //device don't have sensor
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED: //user have not set fingerprint
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE: //sensors are not reachable
                case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                    biometric.setVisibility(View.GONE);
                    break;
            }
            //this will give us the result of the authentication
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(VerifyPin.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (errString.toString().compareToIgnoreCase("Cancel") == 0)
                        return;
                    View view = findViewById(android.R.id.content).getRootView();
                    PopupMessage popupMessage = new PopupMessage("Something went wrong.", errString + "\n\nPlease use MPIN for verification", VerifyPin.this);
                    popupMessage.show(view);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    View view = findViewById(android.R.id.content).getRootView();
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    Intent intent = new Intent(VerifyPin.this, ChoosePerson.class);
                    intent.putExtra("Path", "Data");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });
            //create biometric dialog
            final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Touch ID")
                    .setDescription("Use your fingerprint for authentication")
                    .setNegativeButtonText("Cancel")
                    .build();
            //call the dialog when user press the finger icon
            fingerPrintIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    biometricPrompt.authenticate(promptInfo);
                }
            });
        } else {
            biometric.setVisibility(View.GONE);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) VerifyPin.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }

    private void setupMPINInputs(EditText a, EditText b) {
        a.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty()) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            b.requestFocus();
                        }
                    }, 10);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    public void login(View view) {
        String MpinC = inputPinC1.getText().toString() +
                inputPinC2.getText().toString() +
                inputPinC3.getText().toString() +
                inputPinC4.getText().toString() +
                inputPinC5.getText().toString() +
                inputPinC6.getText().toString();
        PopupMessage popupMessage;
        if (MpinC.length() != 6) {
            String message = "Please enter 6 digit MPIN";
            if (biometric.getVisibility() == View.VISIBLE)
                message = message + "\nor\nUse touch ID for verification";
            popupMessage = new PopupMessage("Verification Failed", message, this);
            popupMessage.show(view);
        } else if (MpinC.compareTo(lastPin) != 0) {
            popupMessage = new PopupMessage("Verification Failed", "You have entered incorrect MPIN, please try again.", this);
            popupMessage.show(view);
        } else {
            Intent intent;
            if (getIntent().hasExtra("ChangePin")) {
                intent = new Intent(this, SetPin.class);
            } else {
                intent = new Intent(this, ChoosePerson.class);
                intent.putExtra("Path", "Data");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
        }
        inputPinC1.setText("");
        inputPinC2.setText("");
        inputPinC3.setText("");
        inputPinC4.setText("");
        inputPinC5.setText("");
        inputPinC6.setText("");
        inputPinC1.requestFocus();
    }

    public void resetMPIN(View view) {
        LayoutInflater inflater = (VerifyPin.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_delete, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
        TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
        TextView deleteDelete = popup.findViewById(R.id.deleteDelete);

        deleteTitle.setText("Reset MPIN");
        deleteMessage.setText("Reset MPIN will log you out from this account " +
                "for verification. You need to login again to this account to set new MPIN. \n\nContinue?\n\n");

        popup.findViewById(R.id.cancelDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        popup.findViewById(R.id.fadeBG_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        deleteDelete.setText("Continue");
        deleteDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.getReference().child(auth.getUid()).child("MPIN").setValue("NULL");
                auth.signOut();
                googleSignInClient.signOut();
                Intent intent = new Intent(VerifyPin.this, Authentication.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void setupMPINInputsReverse(EditText a, EditText b) {
        b.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_DEL && b.getText().length() == 0) {
                    a.requestFocus();
                    a.setSelection(a.getText().length());
                }
                return false;
            }
        });
    }

    public void logout(View view) {
        LayoutInflater inflater = (VerifyPin.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_delete, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
        TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
        TextView deleteDelete = popup.findViewById(R.id.deleteDelete);
        deleteTitle.setText("Logout");
        deleteMessage.setText("Are you sure?");
        deleteDelete.setText("Logout");
        popup.findViewById(R.id.cancelDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        popup.findViewById(R.id.fadeBG_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        deleteDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                googleSignInClient.signOut();
                editor.clear().apply();
                Intent intent = new Intent(VerifyPin.this, Authentication.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    public void showHidePin(View view) {
        if (pinVisible) {
            //Hide pins
            inputPinC1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            inputPinC2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            inputPinC3.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            inputPinC4.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            inputPinC5.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            inputPinC6.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinHideShow.setImageResource(R.drawable.ic_baseline_remove_red_eye_24);
            pinVisible = false;
        } else {
            //show pins
            inputPinC1.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputPinC2.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputPinC3.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputPinC4.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputPinC5.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputPinC6.setInputType(InputType.TYPE_CLASS_NUMBER);
            pinHideShow.setImageResource(R.drawable.ic_baseline_remove_black_eye_24);
            pinVisible = true;
        }
    }
}