package com.example.mydraw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import java.util.List;

public class GalleryPagerAdapter extends PagerAdapter {
    private Context appContext;
    private List<Drawing> itemList;

    public GalleryPagerAdapter(Context context, List<Drawing> drawings) {
        this.appContext = context;
        this.itemList = drawings;
    }

    @Override
    public int getCount() { return itemList.size(); }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object obj) {
        return view == obj;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int pos) {
        View pageView = LayoutInflater.from(appContext).inflate(R.layout.item_pager, container, false);
        
        ImageView displayImage = pageView.findViewById(R.id.imageView);
        TextView titleText = pageView.findViewById(R.id.textName);
        TextView dateText = pageView.findViewById(R.id.textDate);
        
        Drawing artwork = itemList.get(pos);
        displayImage.setImageBitmap(artwork.getBitmap());
        titleText.setText(artwork.getName());
        dateText.setText(artwork.getFormattedDate());
        
        container.addView(pageView);
        return pageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int pos, @NonNull Object obj) {
        container.removeView((View) obj);
    }
}
