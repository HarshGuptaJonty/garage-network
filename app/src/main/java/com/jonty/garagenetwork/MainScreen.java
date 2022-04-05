package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.jonty.garagenetwork.Model.Seller;
import com.jonty.garagenetwork.adapter.AdapterGarage;

import java.util.ArrayList;
import java.util.List;

public class MainScreen extends AppCompatActivity {

    private TextView profile;
    private String lastSearch, path;
    private boolean searched;
    private ListView garageList;
    private ImageButton addGarageIcon, searchGarageIcon, settingIcon;
    private EditText searchGarageText;
    private InputMethodManager inputMethodManager;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth auth;
    private AdapterView.OnItemClickListener originalClick, searchClick;
    private Runnable runnable;
    private Handler handler;
    private LinearLayout noResultFound1;
    private List<Seller> sellers, searchedSellers;
    private List<String> names,storageList,databaseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        profile = findViewById(R.id.profile);
        garageList = findViewById(R.id.garageList);
        addGarageIcon = findViewById(R.id.addGarageIcon);
        searchGarageText = findViewById(R.id.searchGarageText);
        searchGarageIcon = findViewById(R.id.searchGarageIcon);
        settingIcon = findViewById(R.id.settingIcon);
        noResultFound1 = findViewById(R.id.noResultFound1);

        lastSearch = "";
        searched = false;
        path = getIntent().getStringExtra("Path");
        if (path.compareTo("Owner") == 0) {
            profile.setText("Seller Section");
            addGarageIcon.setVisibility(View.VISIBLE);
            searchGarageIcon.setVisibility(View.INVISIBLE);
            settingIcon.setVisibility(View.GONE);
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        sellers = new ArrayList<>();
        searchedSellers = new ArrayList<>();
        names = new ArrayList<>();
        storageList = new ArrayList<>();
        databaseList = new ArrayList<>();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSearchList();
                handler.postDelayed(this, 1000);
            }
        };

        originalClick = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(MainScreen.this, GarageDetails.class);
                intent.putExtra("Details", sellers.get(position));
                intent.putExtra("Path", path);
                startActivity(intent);
            }
        };

        searchClick = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(MainScreen.this, GarageDetails.class);
                intent.putExtra("Details", searchedSellers.get(position));
                intent.putExtra("Path", path);
                startActivity(intent);
            }
        };
        if (path.compareTo("Owner") == 0)
            database.getReference().child("Seller").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    sellers.clear();
                    names.clear();
                    for (DataSnapshot seller : snapshot.getChildren()) {
                        Seller obj = seller.getValue(Seller.class);
                        for(String link:obj.getImageLinks())
                            databaseList.add(link);
                        if (obj.getAuthKey().compareTo(auth.getUid()) != 0)
                            continue;
                        obj.setKey(seller.getKey());
                        sellers.add(obj);
                        names.add(obj.getName());
                    }
                    if (sellers.size() == 0)
                        noResultFound1.setVisibility(View.VISIBLE);
                    else
                        noResultFound1.setVisibility(View.INVISIBLE);
                    AdapterGarage originalAdapter = new AdapterGarage(MainScreen.this, sellers, names);
                    garageList.setAdapter(originalAdapter);
                    garageList.setOnItemClickListener(originalClick);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        else
            database.getReference().child("Seller").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    sellers.clear();
                    names.clear();
                    for (DataSnapshot seller : snapshot.getChildren()) {
                        Seller obj = seller.getValue(Seller.class);
                        for(String link:obj.getImageLinks())
                            databaseList.add(link);
                        if (obj.getAuthKey().compareTo(auth.getUid()) == 0)
                            continue;
                        obj.setKey(seller.getKey());
                        sellers.add(obj);
                        names.add(obj.getName());
                    }
                    if (sellers.size() == 0)
                        noResultFound1.setVisibility(View.VISIBLE);
                    else
                        noResultFound1.setVisibility(View.INVISIBLE);
                    AdapterGarage originalAdapter = new AdapterGarage(MainScreen.this, sellers, names);
                    garageList.setAdapter(originalAdapter);
                    garageList.setOnItemClickListener(originalClick);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        storageReference.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for(StorageReference item: listResult.getItems())
                    item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            storageList.add(uri.toString());
                        }
                    });
            }
        });
    }

    public void goToSetting(View view) {
        Intent intent = new Intent(this, Setting.class);
        startActivity(intent);
    }

    public void addGarage(View view) {
        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, AddGarage.class);
        startActivity(intent);
    }

    public void searchGarage(View view) {
        clearStorage();
        if (searchGarageText.getVisibility() == View.INVISIBLE) {
            //activate search
            searchGarageIcon.setImageResource(R.drawable.ic_baseline_search_off_24);
            profile.setVisibility(View.INVISIBLE);
            searchGarageText.setVisibility(View.VISIBLE);
            searchGarageText.setText("");
            searchGarageText.requestFocus();
            inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            handler.post(runnable);
        } else {
            //deactivate search
            searchGarageIcon.setImageResource(R.drawable.ic_baseline_search_24);
            profile.setVisibility(View.VISIBLE);
            searchGarageText.setVisibility(View.INVISIBLE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            handler.removeCallbacks(runnable);
            if (searched) {
                AdapterGarage originalAdapter = new AdapterGarage(MainScreen.this, sellers, names);
                garageList.setAdapter(originalAdapter);
                garageList.setOnItemClickListener(originalClick);
                searched = false;
            }
            if (noResultFound1.getVisibility() == View.VISIBLE)
                noResultFound1.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        clearStorage();
        if (searchGarageText.getVisibility() != View.INVISIBLE)
            searchGarage(findViewById(android.R.id.content).getRootView());
        else if (path.compareTo("Buyer") == 0) {
            LayoutInflater inflater = (MainScreen.this).getLayoutInflater();
            View popup = inflater.inflate(R.layout.popup_delete, null);
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.MATCH_PARENT;
            final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
            popupWindow.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.CENTER, 0, 0);

            TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
            TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
            TextView deleteDelete = popup.findViewById(R.id.deleteDelete);

            deleteTitle.setText("Exit application");
            deleteMessage.setText("Exit application?");
            deleteDelete.setText("Exit");

            popup.findViewById(R.id.cancelDelete).setOnClickListener(view -> popupWindow.dismiss());
            popup.findViewById(R.id.fadeBG_delete).setOnClickListener(view -> popupWindow.dismiss());
            deleteDelete.setOnClickListener(view -> MainScreen.super.onBackPressed());
        } else
            super.onBackPressed();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) MainScreen.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }

    private void updateSearchList() {
        final String searchText = searchGarageText.getText().toString().toLowerCase().trim();
        if (searchText.compareTo(lastSearch) != 0) {
            searchedSellers.clear();
            names.clear();
            for (Seller arr : sellers) {
                if (arr.getName().toLowerCase().contains(searchText) ||
                        arr.getAddress().toLowerCase().contains(searchText) ||
                        arr.getCountry().toLowerCase().contains(searchText) ||
                        arr.getState().toLowerCase().contains(searchText) ||
                        arr.getDistrict().toLowerCase().contains(searchText)) {
                    searchedSellers.add(arr);
                    names.add(arr.getName());
                }
            }
            searched = true;
            lastSearch = searchText;
            if (searchedSellers.isEmpty()) {
                noResultFound1.setVisibility(View.VISIBLE);
                garageList.setVisibility(View.INVISIBLE);
                return;
            } else {
                noResultFound1.setVisibility(View.INVISIBLE);
                garageList.setVisibility(View.VISIBLE);
            }

            AdapterGarage searchAdapter = new AdapterGarage(MainScreen.this, searchedSellers, names);
            garageList.setAdapter(searchAdapter);
            garageList.setOnItemClickListener(searchClick);
        }
    }

    private void clearStorage() {
        for(String link:storageList)
            if(!databaseList.contains(link)){
                Log.d("myapp","Deleted: "+link);
                storage.getReferenceFromUrl(link).delete();
            }
    }
}