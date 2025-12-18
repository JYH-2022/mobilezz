package com.example.mydraw;

import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private ViewPager imagePager;
    private GridView imageGrid;
    private DatabaseHelper database;
    private List<Drawing> artworks;
    private DrawingAdapter gridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("작품 보기");
            // 액션바 색상 변경
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor("#87CEEB"))); // 하늘색
        }

        database = new DatabaseHelper(this);
        imagePager = findViewById(R.id.viewPager);
        imageGrid = findViewById(R.id.gridView);

        loadArtworks();
    }

    private void loadArtworks() {
        new Thread(() -> {
            artworks = database.getAllDrawings();
            runOnUiThread(() -> {
                if (artworks.isEmpty()) {
                    Toast.makeText(this, "저장된 작품이 없습니다", Toast.LENGTH_SHORT).show();
                } else {
                    initializeViews();
                }
            });
        }).start();
    }

    private void initializeViews() {
        GalleryPagerAdapter pagerAdapter = new GalleryPagerAdapter(this, artworks);
        imagePager.setAdapter(pagerAdapter);

        gridAdapter = new DrawingAdapter(this, artworks);
        imageGrid.setAdapter(gridAdapter);

        imageGrid.setOnItemClickListener((parent, view, pos, id) -> imagePager.setCurrentItem(pos));

        imageGrid.setOnItemLongClickListener((parent, view, pos, id) -> {
            confirmDelete(pos);
            return true;
        });
    }

    private void confirmDelete(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("작품 삭제");
        builder.setMessage("정말 삭제하시겠습니까?");

        builder.setPositiveButton("삭제", (dialog, which) -> {
            Drawing artwork = artworks.get(position);
            new Thread(() -> {
                database.deleteDrawing(artwork.getId());
                runOnUiThread(() -> {
                    artworks.remove(position);
                    gridAdapter.notifyDataSetChanged();
                    imagePager.getAdapter().notifyDataSetChanged();
                    Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) database.close();
    }
}