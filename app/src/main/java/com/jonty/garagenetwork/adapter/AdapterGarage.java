package com.jonty.garagenetwork.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jonty.garagenetwork.Model.Seller;
import com.jonty.garagenetwork.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AdapterGarage extends ArrayAdapter<String> {

    private List<Seller> sellerList;
    private Context context;

    public AdapterGarage(Context context, List<Seller> sellerList, List<String> PersonNames) {
        super(context, R.layout.card_garage, PersonNames);
        this.context=context;
        this.sellerList=sellerList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater=((Activity)context).getLayoutInflater();
        View card=inflater.inflate(R.layout.card_garage,parent,false);
        ImageView garagePhoto=card.findViewById(R.id.garagePhoto);
        TextView sellerName=card.findViewById(R.id.sellerName);
        TextView sellerLocation=card.findViewById(R.id.sellerLocation);
        TextView garageSize=card.findViewById(R.id.garageSize);
        TextView sellerAmount=card.findViewById(R.id.sellerAmount);

        Picasso.get().load(sellerList.get(position).getImageLinks().get(0)).into(garagePhoto);
        sellerName.setText(sellerList.get(position).getName());
        sellerLocation.setText(sellerList.get(position).getAddress());
        garageSize.setText(sellerList.get(position).getHeight()+" x "+sellerList.get(position).getWidth());
        sellerAmount.setText(sellerList.get(position).getPrice()+"");
        return card;
    }
}
