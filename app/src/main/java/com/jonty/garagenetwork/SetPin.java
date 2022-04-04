package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

public class SetPin extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private EditText inputPinA1, inputPinA2, inputPinA3, inputPinA4, inputPinA5, inputPinA6;
    private EditText inputPinB1, inputPinB2, inputPinB3, inputPinB4, inputPinB5, inputPinB6;
    private Button setMPINDone;
    private InputMethodManager inputMethodManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SetPin.this);
        editor = sharedPreferences.edit();
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        inputPinA1 = findViewById(R.id.inputPinA1);
        inputPinA2 = findViewById(R.id.inputPinA2);
        inputPinA3 = findViewById(R.id.inputPinA3);
        inputPinA4 = findViewById(R.id.inputPinA4);
        inputPinA5 = findViewById(R.id.inputPinA5);
        inputPinA6 = findViewById(R.id.inputPinA6);
        inputPinB1 = findViewById(R.id.inputPinB1);
        inputPinB2 = findViewById(R.id.inputPinB2);
        inputPinB3 = findViewById(R.id.inputPinB3);
        inputPinB4 = findViewById(R.id.inputPinB4);
        inputPinB5 = findViewById(R.id.inputPinB5);
        inputPinB6 = findViewById(R.id.inputPinB6);
        setMPINDone = findViewById(R.id.setMPINDone);

        setupMPINInputs(inputPinA1, inputPinA2);
        setupMPINInputs(inputPinA2, inputPinA3);
        setupMPINInputs(inputPinA3, inputPinA4);
        setupMPINInputs(inputPinA4, inputPinA5);
        setupMPINInputs(inputPinA5, inputPinA6);
        setupMPINInputs(inputPinA6, inputPinB1);
        setupMPINInputs(inputPinB1, inputPinB2);
        setupMPINInputs(inputPinB2, inputPinB3);
        setupMPINInputs(inputPinB3, inputPinB4);
        setupMPINInputs(inputPinB4, inputPinB5);
        setupMPINInputs(inputPinB5, inputPinB6);

        setupMPINInputsReverse(inputPinA1, inputPinA2);
        setupMPINInputsReverse(inputPinA2, inputPinA3);
        setupMPINInputsReverse(inputPinA3, inputPinA4);
        setupMPINInputsReverse(inputPinA4, inputPinA5);
        setupMPINInputsReverse(inputPinA5, inputPinA6);
        setupMPINInputsReverse(inputPinA6, inputPinB1);
        setupMPINInputsReverse(inputPinB1, inputPinB2);
        setupMPINInputsReverse(inputPinB2, inputPinB3);
        setupMPINInputsReverse(inputPinB3, inputPinB4);
        setupMPINInputsReverse(inputPinB4, inputPinB5);
        setupMPINInputsReverse(inputPinB5, inputPinB6);

        inputPinB6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty()) {
                    View view = findViewById(android.R.id.content).getRootView();
                    done(view);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        inputPinA1.requestFocus();

        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        database.getReference().child(auth.getUid()).child("SetName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    editor.putString("PersonName", snapshot.getValue(String.class));
                    editor.commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void takeName(View view) {
        LayoutInflater inflater = (SetPin.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_add_person, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        EditText newPersonName = popup.findViewById(R.id.newPersonName);
        newPersonName.setHint("enter your name");
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        newPersonName.requestFocus();
        popup.findViewById(R.id.cancelAddPerson).setVisibility(View.GONE);
        popup.findViewById(R.id.middleBorder).setVisibility(View.GONE);
        popup.findViewById(R.id.savePerson).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                String name = newPersonName.getText().toString().trim().replaceAll("[^a-zA-Z0-9 .]", "");
                PopupMessage popupMessage;
                if (name.length() < 3) {
                    popupMessage = new PopupMessage("Name", "Name too short.", SetPin.this);
                    popupMessage.show(view);
                } else if (name.length() > 20) {
                    popupMessage = new PopupMessage("Name", "Name too long.", SetPin.this);
                    popupMessage.show(view);
                } else {
                    editor.putString("PersonName", name);
                    editor.commit();
                    database.getReference().child(auth.getUid()).child("SetName").setValue(name);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    popupWindow.dismiss();
                    done(view);
                }
            }
        });
    }

    public void done(View view) {
        if (sharedPreferences.getString("PersonName", "").contains("+91")) {
            takeName(findViewById(android.R.id.content).getRootView());
            return;
        }
        String MpinA = inputPinA1.getText().toString() + inputPinA2.getText().toString() +
                inputPinA3.getText().toString() + inputPinA4.getText().toString() +
                inputPinA5.getText().toString() + inputPinA6.getText().toString();
        String MpinB = inputPinB1.getText().toString() + inputPinB2.getText().toString() +
                inputPinB3.getText().toString() + inputPinB4.getText().toString() +
                inputPinB5.getText().toString() + inputPinB6.getText().toString();
        PopupMessage popupMessage;
        if (MpinA.length() != 6 || MpinB.length() != 6) {
            popupMessage = new PopupMessage("Invalid MPIN", "Please enter 6 digit MPIN!", this);
            popupMessage.show(view);
        } else if (MpinA.compareTo(MpinB) != 0) {
            popupMessage = new PopupMessage("Invalid MPIN", "MPIN doesn't match!", this);
            popupMessage.show(view);
        } else {
            database.getReference().child(auth.getUid()).child("MPIN").setValue(MpinA);
            Intent intent;
            if (getIntent().hasExtra("ChangePin"))
                intent = new Intent(this, Setting.class);
            else {
                intent = new Intent(this, MainScreen.class);
                intent.putExtra("Path", "Buyer");
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        inputPinA1.setText("");
        inputPinA2.setText("");
        inputPinA3.setText("");
        inputPinA4.setText("");
        inputPinA5.setText("");
        inputPinA6.setText("");
        inputPinB1.setText("");
        inputPinB2.setText("");
        inputPinB3.setText("");
        inputPinB4.setText("");
        inputPinB5.setText("");
        inputPinB6.setText("");
        inputPinA1.requestFocus();
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

    private void setupMPINInputsReverse(EditText a, EditText b) {
        b.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().isEmpty())
                    a.requestFocus();
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

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) SetPin.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}