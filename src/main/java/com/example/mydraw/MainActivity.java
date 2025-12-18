package com.example.mydraw;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private DrawingView drawingView;
    private DatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawingView = findViewById(R.id.drawingView);
        dbHelper = new DatabaseHelper(this);
        setupImagePicker();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            drawingView.setBackgroundImage(bitmap);
                            Toast.makeText(this, "이미지를 불러왔습니다", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "모드");
        menu.add(0, 2, 0, "색상");
        menu.add(0, 3, 0, "굵기");
        menu.add(0, 4, 0, "저장");
        menu.add(0, 5, 0, "지우기");
        menu.add(0, 6, 0, "불러오기");
        menu.add(0, 7, 0, "이미지 가져오기");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 1) {
            showDrawModeDialog();
        } else if (id == 2) {
            showColorPicker();
        } else if (id == 3) {
            showStrokePicker();
        } else if (id == 4) {
            saveDrawing();
        } else if (id == 5) {
            clearCanvas();
        } else if (id == 6) {
            openGallery();
        } else if (id == 7) {
            importImage();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDrawModeDialog() {
        String[] modes = {"자유 곡선", "직선", "원", "타원", "사각형", "삼각형"};
        DrawingView.DrawMode[] drawModes = {
                DrawingView.DrawMode.FREE_DRAW, DrawingView.DrawMode.LINE,
                DrawingView.DrawMode.CIRCLE, DrawingView.DrawMode.OVAL,
                DrawingView.DrawMode.RECTANGLE, DrawingView.DrawMode.TRIANGLE
        };
        int currentIndex = 0;
        for (int i = 0; i < drawModes.length; i++) {
            if (drawingView.getDrawMode() == drawModes[i]) {
                currentIndex = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("그리기 모드 선택");
        builder.setSingleChoiceItems(modes, currentIndex, (dialog, which) -> {
            drawingView.setDrawMode(drawModes[which]);
            Toast.makeText(this, modes[which] + " 모드", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void showColorPicker() {
        String[] colors = {"검정", "빨강", "파랑", "초록", "노랑", "보라"};
        int[] colorValues = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("색상 선택");
        builder.setItems(colors, (dialog, which) -> {
            drawingView.setColor(colorValues[which]);
            Toast.makeText(this, colors[which] + " 선택됨", Toast.LENGTH_SHORT).show();
        });
        builder.create().show();
    }

    private void showStrokePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("선 굵기 조절");
        final SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(50);
        seekBar.setProgress(10);
        seekBar.setPadding(50, 20, 50, 20);
        final TextView textView = new TextView(this);
        textView.setText("굵기: 10");
        textView.setPadding(50, 20, 50, 20);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(textView);
        layout.addView(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1;
                textView.setText("굵기: " + progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        builder.setView(layout);
        builder.setPositiveButton("확인", (dialog, which) -> {
            int stroke = seekBar.getProgress();
            if (stroke < 1) stroke = 1;
            drawingView.setStrokeWidth(stroke);
            Toast.makeText(this, "굵기: " + stroke, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);
        builder.create().show();
    }

    private void saveDrawing() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("그림 저장");
        final EditText input = new EditText(this);
        input.setHint("그림 이름을 입력하세요");
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);
        builder.setPositiveButton("저장", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                name = "그림_" + System.currentTimeMillis();
            }
            Bitmap bitmap = drawingView.getBitmap();
            long id = dbHelper.saveDrawing(name, bitmap);
            if (id != -1) {
                Toast.makeText(this, "저장 완료: " + name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("취소", null);
        builder.create().show();
    }

    private void clearCanvas() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("전체 지우기");
        builder.setMessage("정말 지우시겠습니까?");
        builder.setPositiveButton("지우기", (dialog, which) -> {
            drawingView.clear();
            Toast.makeText(this, "지웠습니다", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);
        builder.create().show();
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    private void importImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}