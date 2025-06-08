package com.example.bingo;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Board extends BaseActivity {

    private GridLayout gridLayout;
    private final int gridSize = 5;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        // Get views for grid and no-board message layout and button
        gridLayout = findViewById(R.id.gridLayout);
        View noBoardLayout = findViewById(R.id.noBoardLayout);
        Button goToEntryButton = findViewById(R.id.goToEntryButton);

        // Handle incoming intent and persist if present
        Intent intent = getIntent();
        String boardName = intent.getStringExtra("boardName");

        SharedPreferences prefs = getSharedPreferences("BingoPrefs", MODE_PRIVATE);

        if (boardName != null) {
            // Save board name to prefs
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("lastActiveBoardName", boardName);
            editor.apply();
        } else {
            // No boardName from intent, try to load from prefs
            boardName = prefs.getString("lastActiveBoardName", null);
        }

        //Checking for updated text
        boolean resetBoard = getIntent().getBooleanExtra("resetBoard", false);
        if (resetBoard && boardName != null) {
            clearSavedBoardState(boardName);
        }

        //Updating the Board Title and populating squares
        TextView boardNameTextView = findViewById(R.id.boardNameTextView);
        if (boardName != null) {
            boardNameTextView.setText(boardName);
            populateBoardFromDatabase(boardName);
        }

        //Backup popup for new users where boardName does not exist
        if (boardName == null) {
            gridLayout.setVisibility(View.GONE);
            noBoardLayout.setVisibility(View.VISIBLE);
        } else {
            gridLayout.setVisibility(View.VISIBLE);
            noBoardLayout.setVisibility(View.GONE);
        }

        // Setup button to navigate to entry activity
        goToEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent entryIntent = new Intent(Board.this, BoardEntries.class); // Replace EntryActivity.class with your actual entry activity
                startActivity(entryIntent);
                finish(); // optional: close this activity
            }
        });

        setupDrawer(R.id.drawerLayout, R.id.navigationView);

        // Initialize ScaleGestureDetector
        scaleGestureDetector = new ScaleGestureDetector(this, new PinchZoomListener());

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Pass all touch event to ScaleGestureDetector for pinch-to-zoom
        scaleGestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    // Pinch-to-zoom functionality
    private class PinchZoomListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Clamp zoom between 0.5x and 3.0x
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));

            gridLayout.setScaleX(scaleFactor);
            gridLayout.setScaleY(scaleFactor);

            return true;
        }
    }

    // Toggle action when the hamburger icon is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Populates board with entries
    private void populateBoardFromDatabase(String boardName) {
        SharedPreferences prefs = getSharedPreferences("BingoPrefs", MODE_PRIVATE);
        BingoDatabaseHelper dbHelper = new BingoDatabaseHelper(this);

        List<String> savedTexts = new ArrayList<>();
        boolean hasSavedData = false;

        // Check if saved texts exist for this board's cells
        for (int i = 0; i < 25; i++) {
            String savedText = prefs.getString("board_" + boardName + "_cell_" + i + "_text", null);
            if (savedText != null) {
                hasSavedData = true;
                break;
            }
        }

        List<String> selectedEntries = new ArrayList<>();

        if (hasSavedData) {
            // Load saved texts
            for (int i = 0; i < 25; i++) {
                String text = prefs.getString("board_" + boardName + "_cell_" + i + "_text", "");
                selectedEntries.add(text);
            }
        } else {
            // No saved data, load from database and save texts
            List<String> allEntries = dbHelper.getEntryTextsByBoardName(boardName);
            if (allEntries.isEmpty()) {
                Toast.makeText(this, "No entries found for this board", Toast.LENGTH_SHORT).show();
                return;
            }

            Collections.shuffle(allEntries);
            if (allEntries.size() >= 25) {
                selectedEntries = allEntries.subList(0, 25);
            } else {
                selectedEntries.addAll(allEntries);
                Random random = new Random();
                while (selectedEntries.size() < 25) {
                    selectedEntries.add(allEntries.get(random.nextInt(allEntries.size())));
                }
            }

            // Save these entries texts to prefs
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = 0; i < 25; i++) {
                editor.putString("board_" + boardName + "_cell_" + i + "_text", selectedEntries.get(i));
                editor.putBoolean("board_" + boardName + "_cell_" + i + "_activated", false); // reset activation state
            }
            editor.apply();
        }

        // Now build the buttons with saved/loaded data
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(5);
        gridLayout.setRowCount(5);

        for (int i = 0; i < 25; i++) {
            final String text = selectedEntries.get(i);

            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText(text);
            button.setGravity(Gravity.CENTER);
            button.setPadding(5, 5, 5, 5);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                button.setAutoSizeTextTypeUniformWithConfiguration(
                        8, 18, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            }

            GradientDrawable drawable = new GradientDrawable();
            drawable.setStroke(2, Color.BLACK);

            boolean isActivated = prefs.getBoolean("board_" + boardName + "_cell_" + i + "_activated", false);
            if (isActivated) {
                drawable.setColor(Color.rgb(130, 255, 130)); // light green
            } else {
                drawable.setColor(Color.rgb(255, 255, 255)); // white
            }
            button.setBackground(drawable);
            button.setTag(isActivated);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / 5, 1f);
            params.columnSpec = GridLayout.spec(i % 5, 1f);
            params.width = 0;
            params.height = 0;
            params.setMargins(0, 0, 0, 0);
            button.setLayoutParams(params);

            final int cellIndex = i;

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean currentState = (Boolean) button.getTag();
                    if (currentState == null) currentState = false;

                    boolean newState = !currentState;

                    if (newState) {
                        drawable.setColor(Color.rgb(130, 255, 130)); // light green
                    } else {
                        drawable.setColor(Color.rgb(255, 255, 255)); // white
                    }
                    button.setBackground(drawable);
                    button.setTag(newState);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("board_" + boardName + "_cell_" + cellIndex + "_activated", newState);
                    editor.apply();

                    checkIfAllButtonsActivatedFromPrefs();
                }
            });

            gridLayout.addView(button);
        }
    }

    private void clearSavedBoardState(String boardName) {
        SharedPreferences prefs = getSharedPreferences("BingoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int i = 0; i < 25; i++) {
            editor.remove("board_" + boardName + "_cell_" + i + "_text");
            editor.remove("board_" + boardName + "_cell_" + i + "_activated");
        }

        editor.apply();
    }

    private void checkIfAllButtonsActivatedFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("BingoPrefs", MODE_PRIVATE);
        String boardName = prefs.getString("lastActiveBoardName", null);

        if (boardName == null) return;

        for (int i = 0; i < 25; i++) {
            boolean isActive = prefs.getBoolean("board_" + boardName + "_cell_" + i + "_activated", false);
            if (!isActive) return; // One is still inactive
        }

        showCongratulationsDialog(); // All 25 are active!
    }


    private void showCongratulationsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You’ve completed the board.")
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
