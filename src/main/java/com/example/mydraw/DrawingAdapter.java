package com.example.mydraw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class DrawingAdapter extends ArrayAdapter<Drawing> {
    private Context appContext;
    private List<Drawing> itemList;

    public DrawingAdapter(Context context, List<Drawing> drawings) {
        super(context, R.layout.item_drawing, drawings);
        this.appContext = context;
        this.itemList = drawings;
    }

    @NonNull
    @Override
    public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup container) {
        if (convertView == null) {
            convertView = LayoutInflater.from(appContext).inflate(R.layout.item_drawing, container, false);
        }

        Drawing artwork = itemList.get(pos);
        ImageView preview = convertView.findViewById(R.id.thumbnail);
        TextView titleText = convertView.findViewById(R.id.textName);
        TextView dateText = convertView.findViewById(R.id.textDate);

        preview.setImageBitmap(artwork.getBitmap());
        titleText.setText(artwork.getName());
        dateText.setText(artwork.getFormattedDate());

        return convertView;
    }
}