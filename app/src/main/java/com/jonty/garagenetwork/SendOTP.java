package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

import java.util.concurrent.TimeUnit;

public class SendOTP extends AppCompatActivity {
    private EditText inputMobile;
    private ProgressBar OTPSendProgressBar;
    private Button buttonGetOTP;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_otp);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SendOTP.this);
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        inputMobile = findViewById(R.id.inputMobile);
        OTPSendProgressBar = findViewById(R.id.OTPSendProgressBar);
        buttonGetOTP = findViewById(R.id.buttonGetOTP);

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        inputMobile.requestFocus();

        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    public void getOTP(View view) {
        final String mobileNumber = inputMobile.getText().toString().trim();
        PopupMessage popupMessage;
        if (mobileNumber.length() != 10) {
            popupMessage = new PopupMessage("Invalid Number", "Please Enter a valid 10 digit mobile number!", this);
            popupMessage.show(view);
        } else {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            OTPSendProgressBar.setVisibility(View.VISIBLE);
            buttonGetOTP.setVisibility(View.INVISIBLE);

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+91" + mobileNumber,
                    60, TimeUnit.SECONDS,
                    SendOTP.this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                            OTPSendProgressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            OTPSendProgressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);
                            PopupMessage popupMessage1 = new PopupMessage("Failed to send OTP", e.getMessage(), SendOTP.this);
                            popupMessage1.show(view);
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationID, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            OTPSendProgressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);
                            Intent intent = new Intent(SendOTP.this, VerifyOTP.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("MobileNumber", mobileNumber);
                            intent.putExtra("VerificationID", verificationID);
                            startActivity(intent);
                        }
                    });
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) SendOTP.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}