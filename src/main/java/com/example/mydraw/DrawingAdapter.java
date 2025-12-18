package com.example.mydraw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class DrawingAdapter extends BaseAdapter {
    private Context appContext;
    private List<Drawing> itemList;

    public DrawingAdapter(Context context, List<Drawing> drawings) {
        this.appContext = context;
        this.itemList = drawings;
    }

    @Override
    public int getCount() { return itemList.size(); }

    @Override
    public Object getItem(int pos) { return itemList.get(pos); }

    @Override
    public long getItemId(int pos) { return itemList.get(pos).getId(); }

    @Override
    public View getView(int pos, View recycledView, ViewGroup container) {
        if (recycledView == null) {
            recycledView = LayoutInflater.from(appContext).inflate(R.layout.item_drawing, container, false);
        }
        
        Drawing artwork = itemList.get(pos);
        ImageView preview = recycledView.findViewById(R.id.thumbnail);
        TextView titleText = recycledView.findViewById(R.id.textName);
        TextView dateText = recycledView.findViewById(R.id.textDate);
        
        preview.setImageBitmap(artwork.getBitmap());
        titleText.setText(artwork.getName());
        dateText.setText(artwork.getFormattedDate());
        
        return recycledView;
    }
}
