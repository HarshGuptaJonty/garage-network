package com.jonty.garagenetwork;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jonty.garagenetwork.Model.Seller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddGarage extends AppCompatActivity {
    private EditText ownerName, pincode, address, price, height, width, phoneOne, phoneTwo, phoneThree;
    private TextView country, state, district;
    private RadioButton rentButton, sellButton;
    private RequestQueue mRequestQueue;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private int READ_REQUEST_CODE = 1, currentImage, listSize;
    private List<String> imageLinks;
    private List<Uri> uriList;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private ImageView garageImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_garage);

        mRequestQueue = Volley.newRequestQueue(AddGarage.this);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        auth = FirebaseAuth.getInstance();

        imageLinks = new ArrayList<>();
        uriList = new ArrayList<>();
        currentImage = -1;
        listSize = 0;

        ownerName = findViewById(R.id.ownerName);
        pincode = findViewById(R.id.pincode);
        address = findViewById(R.id.address);
        price = findViewById(R.id.price);
        height = findViewById(R.id.height);
        width = findViewById(R.id.width);
        phoneOne = findViewById(R.id.phoneOne);
        garageImage = findViewById(R.id.garageImage);
        phoneTwo = findViewById(R.id.phoneTwo);
        phoneThree = findViewById(R.id.phoneThree);
        country = findViewById(R.id.country);
        state = findViewById(R.id.state);
        district = findViewById(R.id.district);
        rentButton = findViewById(R.id.rentButton);
        sellButton = findViewById(R.id.sellButton);
    }

    public void submit(View view) {
        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        String ownerStr = ownerName.getText().toString().trim();
        String pincodeStr = pincode.getText().toString().trim();
        String addressStr = address.getText().toString().trim();
        String priceStr = price.getText().toString().trim();
        String heightStr = height.getText().toString().trim();
        String widthStr = width.getText().toString().trim();
        String phoneOneStr = phoneOne.getText().toString().trim();
        String phoneTwoStr = phoneTwo.getText().toString().trim();
        String phoneThreeStr = phoneThree.getText().toString().trim();
        String countryStr = country.getText().toString().trim();
        String stateStr = state.getText().toString().trim();
        String districtStr = district.getText().toString().trim();

        if (ownerStr.isEmpty() || pincodeStr.isEmpty() || addressStr.isEmpty() || priceStr.isEmpty() ||
                heightStr.isEmpty() || widthStr.isEmpty() || phoneOneStr.isEmpty()) {
            Toast.makeText(this, "Please fill all the required fields", Toast.LENGTH_SHORT).show();
            return;
        } else if (countryStr.isEmpty() || stateStr.isEmpty() || districtStr.isEmpty()) {
            Toast.makeText(this, "Please check pincode to validate country, state, district", Toast.LENGTH_SHORT).show();
            return;
        } else if (phoneOneStr.length() != 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        } else if (uriList.isEmpty()) {
            Toast.makeText(this, "Please enter atleast one photo", Toast.LENGTH_SHORT).show();
            return;
        }

        int type = 0;
        if (sellButton.isChecked())
            type = 1;

        Seller obj = new Seller(ownerStr, countryStr, stateStr, addressStr, Integer.parseInt(pincodeStr),
                Integer.parseInt(priceStr), Integer.parseInt(heightStr), Integer.parseInt(widthStr), Long.parseLong(phoneOneStr),
                type);
        if (!phoneTwoStr.isEmpty())
            obj.addPhone(Long.parseLong(phoneTwoStr));
        if (!phoneThreeStr.isEmpty())
            obj.addPhone(Long.parseLong(phoneThreeStr));
        obj.setAuthKey(auth.getUid());
        obj.setImageLinks(imageLinks);
        database.getReference().child("Seller").push().setValue(obj);
        onBackPressed();
    }

    public void checkPin(View view) {
        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        String pincodeStr = pincode.getText().toString().trim();
        if (pincodeStr.isEmpty())
            Toast.makeText(this, "Please enter a pin number", Toast.LENGTH_SHORT).show();
        else
            getDataFromPinCode(pincodeStr);
    }

    public void previousImage(View view) {
        if (uriList.size() == 0 || currentImage == -1) {
            Toast.makeText(this, "No more image to show", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentImage == 0) {
            currentImage = listSize - 1;
            garageImage.setImageURI(uriList.get(currentImage));
        } else
            garageImage.setImageURI(uriList.get(--currentImage));
    }

    public void nextImage(View view) {
        if (uriList.size() == 0 || currentImage == -1) {
            Toast.makeText(this, "No more image to show", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentImage == listSize - 1) {
            currentImage = 0;
            garageImage.setImageURI(uriList.get(currentImage));
        } else
            garageImage.setImageURI(uriList.get(++currentImage));
    }

    public void uploadImages(View view) {
        if (!isNetworkConnected()) {
            Intent intent = new Intent(this, NetworkState.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
            if (data.getClipData() != null) {
                int cout = data.getClipData().getItemCount();
                for (int i = 0; i < cout; i++) {
                    Uri imageurl = data.getClipData().getItemAt(i).getUri();
                    uriList.add(imageurl);
                }
            } else {
                Uri imageurl = data.getData();
                uriList.add(imageurl);
            }
            currentImage = 0;
            listSize = uriList.size();
            garageImage.setImageURI(uriList.get(0));

            long currentTimeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            for (int i = 0; i < listSize && i < 10; ++i) {
                String fileName = uriList.get(i).toString();
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                StorageReference imageRef = storageReference.child(currentTimeStamp + " " + fileName);

                imageRef.putFile(uriList.get(i)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                imageLinks.add(uri.toString());
                            }
                        });
                    }
                });
            }
        } else
            Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
    }

    private void getDataFromPinCode(String pinCode) {
        mRequestQueue.getCache().clear();
        String url = "http://www.postalpincode.in/api/pincode/" + pinCode;
        RequestQueue queue = Volley.newRequestQueue(AddGarage.this);
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray postOfficeArray = response.getJSONArray("PostOffice");
                    if (response.getString("Status").equals("Error")) {
                        Toast.makeText(AddGarage.this, "Pin code is not valid.", Toast.LENGTH_SHORT).show();
                    } else {
                        JSONObject obj = postOfficeArray.getJSONObject(0);
                        String districtStr = obj.getString("District");
                        String stateStr = obj.getString("State");
                        String countryStr = obj.getString("Country");

                        district.setText(districtStr);
                        state.setText(stateStr);
                        country.setText(countryStr);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(AddGarage.this, "Pin code is not valid..", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(AddGarage.this, "Pin code is not valid..." + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(objectRequest);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) AddGarage.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return connectivityManager.getActiveNetworkInfo() != null && activeNetworkInfo.isConnected();
    }
}