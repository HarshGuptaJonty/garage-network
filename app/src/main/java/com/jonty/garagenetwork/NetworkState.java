package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jonty.garagenetwork.PopupHandler.PopupMessage;

public class NetworkState extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_state);
    }

    public void reload(View view) {
        if (isNetworkConnected()) {
            Intent intent = new Intent(this, VerifyPin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            PopupMessage popupMessage = new PopupMessage("No network", "Failed to " +
                    "reconnect to network, please check your internet connection.", this);
            popupMessage.show(view);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) NetworkState.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}