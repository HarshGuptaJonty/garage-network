package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

import java.util.concurrent.TimeUnit;

public class VerifyOTP extends AppCompatActivity {
    private TextView textMobile;
    private String mobileNumber, verificationID;
    private ProgressBar OTPVerifyProgressBar;
    private Button buttonVerify;
    private EditText inputCode1, inputCode2, inputCode3, inputCode4, inputCode5, inputCode6;
    private FirebaseAuth auth;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseDatabase database;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(VerifyOTP.this);
        editor = sharedPreferences.edit();
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        textMobile = findViewById(R.id.textMobile);
        OTPVerifyProgressBar = findViewById(R.id.OTPVerifyProgressBar);
        buttonVerify = findViewById(R.id.buttonVerify);
        inputCode1 = findViewById(R.id.inputCode1);
        inputCode2 = findViewById(R.id.inputCode2);
        inputCode3 = findViewById(R.id.inputCode3);
        inputCode4 = findViewById(R.id.inputCode4);
        inputCode5 = findViewById(R.id.inputCode5);
        inputCode6 = findViewById(R.id.inputCode6);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        mobileNumber = "+91-" + getIntent().getStringExtra("MobileNumber");
        verificationID = getIntent().getStringExtra("VerificationID");
        textMobile.setText(mobileNumber);

        setupOTPInputs(inputCode1, inputCode2);
        setupOTPInputs(inputCode2, inputCode3);
        setupOTPInputs(inputCode3, inputCode4);
        setupOTPInputs(inputCode4, inputCode5);
        setupOTPInputs(inputCode5, inputCode6);

        setupOTPInputsReverse(inputCode1, inputCode2);
        setupOTPInputsReverse(inputCode2, inputCode3);
        setupOTPInputsReverse(inputCode3, inputCode4);
        setupOTPInputsReverse(inputCode4, inputCode5);
        setupOTPInputsReverse(inputCode5, inputCode6);

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        inputCode1.requestFocus();

        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void setupOTPInputs(EditText a, EditText b) {
        a.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty())
                    b.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void setupOTPInputsReverse(EditText a, EditText b) {
        b.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().isEmpty()) {
                    a.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        b.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_DEL && b.getText().length() == 0) {
                    a.requestFocus();
                }
                return false;
            }
        });
    }

    public void verifyOTP(View view) {
        PopupMessage popupMessage;
        String inputCode = inputCode1.getText().toString() +
                inputCode2.getText().toString() +
                inputCode3.getText().toString() +
                inputCode4.getText().toString() +
                inputCode5.getText().toString() +
                inputCode6.getText().toString();
        if (inputCode.length() != 6) {
            popupMessage = new PopupMessage("Invalid OTP", "Please enter valid OTP!", VerifyOTP.this);
            popupMessage.show(view);
            inputCode1.setText("");
            inputCode2.setText("");
            inputCode3.setText("");
            inputCode4.setText("");
            inputCode5.setText("");
            inputCode6.setText("");
            inputCode1.requestFocus();
        } else {
            if (verificationID != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                OTPVerifyProgressBar.setVisibility(View.VISIBLE);
                buttonVerify.setVisibility(View.INVISIBLE);
                PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(verificationID, inputCode);
                auth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        OTPVerifyProgressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);
                        if (task.isSuccessful()) {
                            editor.putString("PersonName", mobileNumber);
                            editor.commit();
                            loginMPI();
                        } else {
                            PopupMessage popupMessage1 = new PopupMessage("Invalid OTP",
                                    "The OTP entered was invalid, please try again!", VerifyOTP.this);
                            popupMessage1.show(view);
                            inputCode1.setText("");
                            inputCode2.setText("");
                            inputCode3.setText("");
                            inputCode4.setText("");
                            inputCode5.setText("");
                            inputCode6.setText("");
                            inputCode1.requestFocus();
                        }
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
                        intent = new Intent(VerifyOTP.this, SetPin.class);
                    else
                        intent = new Intent(VerifyOTP.this, VerifyPin.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    database.getReference().child(auth.getUid()).child("MPIN").setValue("NULL");
                    Intent intent = new Intent(VerifyOTP.this, SetPin.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void resendOTP(View view) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobileNumber,
                60, TimeUnit.SECONDS,
                VerifyOTP.this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        OTPVerifyProgressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        OTPVerifyProgressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);
                        PopupMessage popupMessage1 = new PopupMessage("Failed to send OTP", e.getMessage(), VerifyOTP.this);
                        popupMessage1.show(view);
                    }

                    @Override
                    public void onCodeSent(@NonNull String newVerificationID, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verificationID = newVerificationID;
                        OTPVerifyProgressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);
                        PopupMessage popupMessage1 = new PopupMessage("OTP Sent", "OTP send successfully!", VerifyOTP.this);
                        popupMessage1.show(view);
                    }
                });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) VerifyOTP.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}