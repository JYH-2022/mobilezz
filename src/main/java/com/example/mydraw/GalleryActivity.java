package com.example.mydraw;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private List<Drawing> drawings;
    private ListView listView;
    private DrawingListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new ListView(this);
        setContentView(listView);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("저장된 그림");
        }

        dbHelper = new DatabaseHelper(this);
        loadDrawings();
    }

    private void loadDrawings() {
        new Thread(() -> {
            drawings = dbHelper.getAllDrawings();
            runOnUiThread(() -> {
                if (drawings.isEmpty()) {
                    Toast.makeText(this, "저장된 그림이 없습니다", Toast.LENGTH_SHORT).show();
                } else {
                    setupListView();
                }
            });
        }).start();
    }

    private void setupListView() {
        adapter = new DrawingListAdapter();
        listView.setAdapter(adapter);
        listView.setDividerHeight(1);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showOptionsDialog(position);
            return true;
        });
    }

    private void showOptionsDialog(int position) {
        Drawing drawing = drawings.get(position);
        String[] options = {"삭제"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(drawing.getName());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                deleteDrawing(position);
            }
        });
        builder.create().show();
    }

    private void deleteDrawing(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("삭제 확인");
        builder.setMessage("정말 삭제하시겠습니까?");
        builder.setPositiveButton("삭제", (dialog, which) -> {
            Drawing drawing = drawings.get(position);
            new Thread(() -> {
                dbHelper.deleteDrawing(drawing.getId());
                runOnUiThread(() -> {
                    drawings.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        builder.setNegativeButton("취소", null);
        builder.create().show();
    }

    private class DrawingListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return drawings.size();
        }

        @Override
        public Object getItem(int position) {
            return drawings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return drawings.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.activity_list_item, parent, false);
            }

            ImageView imageView = convertView.findViewById(android.R.id.icon);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            Drawing drawing = drawings.get(position);
            imageView.setImageBitmap(drawing.getBitmap());
            imageView.getLayoutParams().width = 200;
            imageView.getLayoutParams().height = 200;
            text1.setText(drawing.getName());
            text2.setText(drawing.getFormattedDate());

            return convertView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrawings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}