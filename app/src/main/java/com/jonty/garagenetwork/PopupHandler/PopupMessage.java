package com.jonty.garagenetwork.PopupHandler;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.jonty.garagenetwork.R;

public class PopupMessage {
    private String title, message;
    private Context context;

    public PopupMessage(String Title, String Message, Context Context) {
        title = Title;
        message = Message;
        context = Context;
    }

    public void show(View view) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View popup = inflater.inflate(R.layout.popup_message, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popup, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        TextView messageTitle = popup.findViewById(R.id.messageTitle);
        TextView messageMessage = popup.findViewById(R.id.messageMessage);

        messageTitle.setText(title);
        messageMessage.setText(message + "\n\n");

        popup.findViewById(R.id.messageOkay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        popup.findViewById(R.id.fadeBG_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }
}
