package com.jonty.garagenetwork;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.FirebaseDatabase;
import com.jonty.garagenetwork.Model.Seller;
import com.squareup.picasso.Picasso;

public class GarageDetails extends AppCompatActivity {
    private Seller obj;
    private String path;
    private Button deleteGarage;
    private TextView ownerName, country, state, address, district, pincode, price, size, phone1, phone2, phone3;
    private LinearLayout phoneLayout2, phoneLayout3;
    private ImageView galleryImages;
    private ImageButton previousImage,nextImage;
    private int currentImage = 0,imageSize;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage_details);

        galleryImages = findViewById(R.id.galleryImages);
        ownerName = findViewById(R.id.ownerName);
        country = findViewById(R.id.country);
        state = findViewById(R.id.state);
        address = findViewById(R.id.address);
        district = findViewById(R.id.district);
        pincode = findViewById(R.id.pincode);
        price = findViewById(R.id.price);
        size = findViewById(R.id.size);
        phone1 = findViewById(R.id.phone1);
        phone2 = findViewById(R.id.phone2);
        phone3 = findViewById(R.id.phone3);
        phoneLayout2 = findViewById(R.id.phoneLayout2);
        phoneLayout3 = findViewById(R.id.phoneLayout3);
        previousImage = findViewById(R.id.previousImage);
        nextImage = findViewById(R.id.nextImage);
        deleteGarage = findViewById(R.id.deleteGarage);

        obj = (Seller) getIntent().getSerializableExtra("Details");
        path = getIntent().getStringExtra("Path");
        database=FirebaseDatabase.getInstance();

        if(path.compareTo("Owner")==0){
            deleteGarage.setVisibility(View.VISIBLE);
        }

        Picasso.get().load(obj.getImageLinks().get(0)).into(galleryImages);
        ownerName.setText(obj.getName());
        country.setText(obj.getCountry());
        state.setText(obj.getState());
        address.setText(obj.getAddress());
        district.setText(obj.getDistrict());
        pincode.setText(obj.getPinCode() + "");
        String type = "";
        if (obj.getType() == 0)
            type = ", for rent";
        else
            type = ", for sell";
        price.setText(obj.getPrice() + type);
        size.setText(obj.getHeight() + " X " + obj.getWidth());
        phone1.setText(obj.getPhoneNumber().get(0) + "");
        if (obj.getPhoneNumber().size() == 2)
            phone2.setText(obj.getPhoneNumber().get(1) + "");
        else
            phoneLayout2.setVisibility(View.GONE);
        if (obj.getPhoneNumber().size() == 3) {
            phone2.setText(obj.getPhoneNumber().get(1) + "");
            phoneLayout2.setVisibility(View.VISIBLE);
            phone3.setText(obj.getPhoneNumber().get(2) + "");
        }else
            phoneLayout3.setVisibility(View.GONE);
        imageSize=obj.getImageLinks().size();

        if(imageSize==1){
            previousImage.setVisibility(View.GONE);
            nextImage.setVisibility(View.GONE);
        }
    }

    public void previous(View view) {
        if(currentImage==0){
            Picasso.get().load(obj.getImageLinks().get(imageSize-1)).into(galleryImages);
            currentImage=imageSize-1;
        } else
            Picasso.get().load(obj.getImageLinks().get(--currentImage)).into(galleryImages);
    }

    public void next(View view) {
        if(currentImage==imageSize-1){
            Picasso.get().load(obj.getImageLinks().get(0)).into(galleryImages);
            currentImage=0;
        } else
            Picasso.get().load(obj.getImageLinks().get(++currentImage)).into(galleryImages);
    }

    public void call(View view) {
        int tag = Integer.parseInt(view.getTag().toString());
        Intent intent = new Intent(Intent.ACTION_DIAL);
        if (tag == 1) {
            intent.setData(Uri.parse("tel:" + phone1.getText().toString()));
        } else if (tag == 2) {
            intent.setData(Uri.parse("tel:" + phone2.getText().toString()));
        } else {
            intent.setData(Uri.parse("tel:" + phone3.getText().toString()));
        }
        startActivity(intent);
    }

    public void deleteGarage(View view){
        LayoutInflater inflater = (GarageDetails.this).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_delete, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.CENTER, 0, 0);

        TextView deleteTitle = popup.findViewById(R.id.deleteTitle);
        TextView deleteMessage = popup.findViewById(R.id.deleteMessage);
        TextView deleteDelete = popup.findViewById(R.id.deleteDelete);

        deleteTitle.setText("Remove garage from list?");
        deleteMessage.setText("Removing garage from list will remove its existence and cannot be restored?");
        deleteDelete.setText("Delete");

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
                database.getReference().child("Seller").child(obj.getKey()).removeValue();
                GarageDetails.super.onBackPressed();
            }
        });
    }
}