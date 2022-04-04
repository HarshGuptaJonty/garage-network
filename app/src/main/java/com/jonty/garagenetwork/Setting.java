package com.jonty.garagenetwork;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jonty.garagenetwork.PopupHandler.PopupMessage;

import java.util.concurrent.TimeUnit;

public class Setting extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private TextView accountPersonName, pinStatus, fingerPrintStatus, storageText;
    private FirebaseAuth auth;
    private ImageView accountProfileImage, settingIconDeveloper;
    private GoogleSignInClient googleSignInClient;
    private FirebaseDatabase database;
    private String lastMPIN;
    private LinearLayout touchIdCard, storageLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Setting.this);
        editor = sharedPreferences.edit();
        if (!sharedPreferences.getBoolean("Activate Developer", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        accountProfileImage = findViewById(R.id.accountProfileImage);
        settingIconDeveloper = findViewById(R.id.settingIconDeveloper);
        accountPersonName = findViewById(R.id.accountPersonName);
        pinStatus = findViewById(R.id.pinStatus);
        fingerPrintStatus = findViewById(R.id.fingerPrintStatus);
        storageText = findViewById(R.id.storageText);
        touchIdCard = findViewById(R.id.touchIdCard);
        storageLayout = findViewById(R.id.storageLayout);
        auth = FirebaseAuth.getInstance();
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

        database.getReference().child(auth.getUid()).child("MPIN").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    lastMPIN = snapshot.getValue(String.class);
                    if (lastMPIN.compareTo("NULL") != 0)
                        pinStatus.setText("Change MPIN");
                } else {
                    lastMPIN = "";
                    database.getReference().child(auth.getUid()).child("MPIN").setValue("NULL");
                    pinStatus.setText("Set MPIN");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        String givenName = sharedPreferences.getString("PersonName", "Not Given");
        if (!givenName.contains("+91"))
            accountPersonName.setText(givenName);
        else {
            database.getReference().child(auth.getUid()).child("SetName").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        accountPersonName.setText(snapshot.getValue(String.class));
                        editor.putString("PersonName", snapshot.getValue(String.class));
                        editor.commit();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
        updateImage(sharedPreferences.getString("PersonName", "!").toLowerCase().charAt(0));

        Window window = (Setting.this).getWindow();
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.setting_dark_status_bar));
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.setting_light_status_bar));
        }

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS: //yes we can use the sensor
                touchIdCard.setVisibility(View.VISIBLE);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE: //device don't have sensor
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED: //user have not set fingerprint
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE: //sensors are not reachable
                touchIdCard.setVisibility(View.GONE);
                break;
        }

        if (sharedPreferences.getBoolean("Activate Biometric", false))
            fingerPrintStatus.setText("Disable touch ID");
        else
            fingerPrintStatus.setText("Enable touch ID");

        settingIconDeveloper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                PopupMessage popupMessage;
                if (sharedPreferences.getBoolean("Activate Developer", false)) {
                    editor.putBoolean("Activate Developer", false);
                    popupMessage = new PopupMessage("Developer", "Developer mode is OFF! " +
                            "\nFeatures like taking screenshots is now deactivated.\n\n" +
                            "Please restart the app to apply the feature.", Setting.this);
                } else {
                    editor.putBoolean("Activate Developer", true);
                    long currentTimeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                    editor.putLong("TimeStamp", currentTimeStamp);
                    popupMessage = new PopupMessage("Developer", "Developer mode is ON for the next 24 hours. " +
                            "\nFeatures like taking screenshots is now activated.\n\n" +
                            "Please restart the app to apply the feature.", Setting.this);
                }
                editor.commit();
                popupMessage.show(view);
                return false;
            }
        });

        long size = sharedPreferences.getLong("Storage size", 0);
        if (size == 0)
            storageLayout.setVisibility(View.GONE);
        else
            storageText.setText("Storage size used: " + String.format("%.2f", ((double) size / 1048576)) + "MB");
    }

    private void updateImage(char ch) {
        ch = Character.toLowerCase(ch);
        int id;
        if (ch == 'a') id = R.drawable.a_alphabet;
        else if (ch == 'b') id = R.drawable.b_alphabet;
        else if (ch == 'c') id = R.drawable.c_alphabet;
        else if (ch == 'd') id = R.drawable.d_alphabet;
        else if (ch == 'e') id = R.drawable.e_alphabet;
        else if (ch == 'f') id = R.drawable.f_alphabet;
        else if (ch == 'g') id = R.drawable.g_alphabet;
        else if (ch == 'h') id = R.drawable.h_alphabet;
        else if (ch == 'i') id = R.drawable.i_alphabet;
        else if (ch == 'j') id = R.drawable.j_alphabet;
        else if (ch == 'k') id = R.drawable.k_alphabet;
        else if (ch == 'l') id = R.drawable.l_alphabet;
        else if (ch == 'm') id = R.drawable.m_alphabet;
        else if (ch == 'n') id = R.drawable.n_alphabet;
        else if (ch == 'o') id = R.drawable.o_alphabet;
        else if (ch == 'p') id = R.drawable.p_alphabet;
        else if (ch == 'q') id = R.drawable.q_alphabet;
        else if (ch == 'r') id = R.drawable.r_alphabet;
        else if (ch == 's') id = R.drawable.s_alphabet;
        else if (ch == 't') id = R.drawable.t_alphabet;
        else if (ch == 'u') id = R.drawable.u_alphabet;
        else if (ch == 'v') id = R.drawable.v_alphabet;
        else if (ch == 'w') id = R.drawable.w_alphabet;
        else if (ch == 'x') id = R.drawable.x_alphabet;
        else if (ch == 'y') id = R.drawable.y_alphabet;
        else if (ch == 'z') id = R.drawable.z_alphabet;
        else if (ch == '1') id = R.drawable.one;
        else if (ch == '2') id = R.drawable.two;
        else if (ch == '3') id = R.drawable.three;
        else if (ch == '4') id = R.drawable.four;
        else if (ch == '5') id = R.drawable.five;
        else if (ch == '6') id = R.drawable.six;
        else if (ch == '7') id = R.drawable.seven;
        else if (ch == '8') id = R.drawable.eight;
        else if (ch == '9') id = R.drawable.nine;
        else if (ch == '0') id = R.drawable.zero;
        else id = R.drawable.exclamation;
        accountProfileImage.setImageResource(id);
    }

    public void logout(View view) {
        LayoutInflater inflater = (Setting.this).getLayoutInflater();
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
                Intent intent = new Intent(Setting.this, Authentication.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    public void setPin(View view) {
        Intent intent;
        if (lastMPIN.compareTo("NULL") == 0)
            intent = new Intent(this, SetPin.class);
        else {
            intent = new Intent(this, VerifyPin.class);
            intent.putExtra("ChangePin", lastMPIN);
        }
        startActivity(intent);
    }

    public void trashBin(View view) {
        Intent intent = new Intent(this, MainScreen.class);
        intent.putExtra("Path", "Owner");
        startActivity(intent);
    }

    public void deleteAccount(View view) {
        LayoutInflater inflater = (Setting.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_delete, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
        TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
        TextView deleteDelete = popup.findViewById(R.id.deleteDelete);
        deleteTitle.setText("Delete account");
        deleteMessage.setText("To delete account you need to login again and proceed to delete account with in 1 min\n\n" +
                "Deleting account will remove all your profiles and cards from database, this cannot be reversed. Are you sure?\n\n");
        deleteDelete.setText("Yes, Delete");
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
                final String uid = auth.getUid();
                auth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            googleSignInClient.signOut();
                            database.getReference().child(uid).removeValue();
                            Intent intent = new Intent(Setting.this, Authentication.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            PopupMessage popupMessage = new PopupMessage("Account deleted", "Failed to delete account: " + task.getException().getMessage(), Setting.this);
                            popupMessage.show(view);
                        }
                    }
                });
                popupWindow.dismiss();
            }
        });
    }

    public void contactMe(View view) {
        LayoutInflater inflater = (Setting.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_delete, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
        TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
        TextView deleteDelete = popup.findViewById(R.id.deleteDelete);
        deleteTitle.setText("Contact");
        deleteMessage.setText("Hello, I am Harsh Gupta,\nThank you for showing your interest" +
                " in my application, feel free to contact me through email.\n\n");
        deleteDelete.setText("Copy Email");
        popup.findViewById(R.id.cancelDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        deleteDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied mail", "www.jontyg90@gmail.com");
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Setting.this, "Mail copied to clipboard.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void changeName(View view) {
        LayoutInflater inflater = (Setting.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_add_person, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        EditText newPersonName = popup.findViewById(R.id.newPersonName);
        newPersonName.setHint("enter your name");
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        newPersonName.requestFocus();
        newPersonName.setText(accountPersonName.getText());
        newPersonName.setSelection(accountPersonName.getText().length());
        popup.findViewById(R.id.cancelAddPerson).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                popupWindow.dismiss();
            }
        });
        popup.findViewById(R.id.savePerson).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = newPersonName.getText().toString().trim().replaceAll("[^a-zA-Z0-9 .]", "");
                PopupMessage popupMessage;
                if (name.length() < 3) {
                    popupMessage = new PopupMessage("Name", "Name too short.", Setting.this);
                    popupMessage.show(view);
                } else if (name.length() > 20) {
                    popupMessage = new PopupMessage("Name", "Name too long.", Setting.this);
                    popupMessage.show(view);
                } else {
                    if (name.compareTo(accountPersonName.getText().toString()) != 0) {
                        editor.putString("PersonName", name);
                        editor.commit();
                        database.getReference().child(auth.getUid()).child("SetName").setValue(name);
                        accountPersonName.setText(name);
                        updateImage(name.charAt(0));
                    }
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    popupWindow.dismiss();
                }
            }
        });
    }

    public void fingerPrintToggle(View view) {
        PopupMessage popupMessage;
        if (fingerPrintStatus.getText().toString().compareTo("Disable touch ID") == 0) {
            editor.putBoolean("Activate Biometric", false);
            fingerPrintStatus.setText("Enable touch ID");
            popupMessage = new PopupMessage("Touch ID", "Touch Id has been disabled, " +
                    "you will be required MPIN for verification from now on.", this);
        } else {
            editor.putBoolean("Activate Biometric", true);
            fingerPrintStatus.setText("Disable touch ID");
            popupMessage = new PopupMessage("Touch ID", "Touch Id has been enabled, " +
                    "you can use fingerprint for verification from now on.", this);
        }
        popupMessage.show(view);
        editor.commit();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Setting.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}