package com.example.mydraw;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private DrawingView canvasView;
    private DatabaseHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("고급 그림판");

        // 액션바 색상 변경
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor("#87CEEB"))); // 하늘색
        }

        canvasView = findViewById(R.id.drawingView);
        database = new DatabaseHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_mode) {
            displayModeDialog();
        } else if (itemId == R.id.action_color) {
            displayColorDialog();
        } else if (itemId == R.id.action_save) {
            performSave();
        } else if (itemId == R.id.action_load) {
            startActivity(new Intent(this, GalleryActivity.class));
        } else if (itemId == R.id.action_clear) {
            canvasView.clear();
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayModeDialog() {
        String[] modeNames = {"직선", "원형", "사각형"};
        DrawingView.DrawMode[] modeValues = {
                DrawingView.DrawMode.LINE,
                DrawingView.DrawMode.CIRCLE,
                DrawingView.DrawMode.RECTANGLE
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("도형 선택");
        builder.setItems(modeNames, (dialog, which) -> canvasView.setDrawMode(modeValues[which]));
        builder.show();
    }

    private void displayColorDialog() {
        String[] colorNames = {"검정색", "빨강색", "파랑색", "초록색"};
        int[] colorCodes = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("색 선택");
        builder.setItems(colorNames, (dialog, which) -> canvasView.setColor(colorCodes[which]));
        builder.show();
    }

    private void performSave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("그림 저장하기");

        final EditText nameInput = new EditText(this);
        builder.setView(nameInput);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String drawingName = nameInput.getText().toString();
            if (drawingName.isEmpty()) drawingName = "작품_" + System.currentTimeMillis();

            final String finalName = drawingName;
            new Thread(() -> {
                long result = database.saveDrawing(finalName, canvasView.getBitmap());
                runOnUiThread(() -> Toast.makeText(this,
                        result != -1 ? "저장 성공" : "저장 실패", Toast.LENGTH_SHORT).show());
            }).start();
        });
        builder.show();
    }
}