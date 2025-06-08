package com.example.bingo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BoardEntries extends BaseActivity {

    private BingoDatabaseHelper dbHelper;
    private TextView currentlySelectedBoard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_entries);

        setupDrawer(R.id.drawerLayout, R.id.navigationView);

        dbHelper = new BingoDatabaseHelper(this);

        LinearLayout boardListContainer = findViewById(R.id.boardListContainer);
        ImageButton addBoardButton = findViewById(R.id.addBoardButton);
        LinearLayout entryList = findViewById(R.id.entryList);
        ImageButton addEntryButton = findViewById(R.id.addEntryButton);
        Button saveEntriesButton = findViewById(R.id.saveEntriesButton);
        Button deleteBoardButton = findViewById(R.id.deleteBoardButton);
        Button activateBoardButton = findViewById(R.id.activateBoardButton);

        loadBoards(boardListContainer, addBoardButton);

        //Activate Board Button
        activateBoardButton.setOnClickListener(v -> {
            if (currentlySelectedBoard == null) return;

            String boardName = currentlySelectedBoard.getText().toString();

            // Pass only the board name to Board activity
            Intent intent = new Intent(BoardEntries.this, Board.class);
            intent.putExtra("boardName", boardName);
            intent.putExtra("resetBoard", true);
            startActivity(intent);
        });

        //Delete Board Button
        deleteBoardButton.setOnClickListener(v -> {
            if (currentlySelectedBoard == null) return;

            String boardName = currentlySelectedBoard.getText().toString();

            new AlertDialog.Builder(this)
                    .setTitle("Delete Board?")
                    .setMessage("Are you sure you want to delete \"" + boardName + "\"? This cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Step 1: Find board ID
                        long boardId = -1;
                        Cursor cursor = dbHelper.getAllBoards();
                        while (cursor.moveToNext()) {
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                            if (name.equals(boardName)) {
                                boardId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                                break;
                            }
                        }
                        cursor.close();

                        if (boardId != -1) {
                            // Step 2: Delete the board (entries will cascade delete)
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete("boards", "id = ?", new String[]{String.valueOf(boardId)});

                            // Step 3: Refresh UI
                            currentlySelectedBoard = null;
                            LinearLayout newBoardListContainer = findViewById(R.id.boardListContainer);
                            loadBoards(newBoardListContainer, findViewById(R.id.addBoardButton));
                            LinearLayout newEntryList = findViewById(R.id.entryList);
                            newEntryList.removeAllViews();

                            Toast.makeText(this, "Board deleted", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        //Update entries button
        saveEntriesButton.setOnClickListener(v -> {
            if (currentlySelectedBoard == null) return;

            new AlertDialog.Builder(this)
                    .setTitle("Save Changes?")
                    .setMessage("Are you sure you want to save these entries? This will overwrite any existing entries.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        String boardName = currentlySelectedBoard.getText().toString();

                        // Step 1: Find board ID
                        long boardId = -1;
                        Cursor cursor = dbHelper.getAllBoards();
                        while (cursor.moveToNext()) {
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                            if (name.equals(boardName)) {
                                boardId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                                break;
                            }
                        }
                        cursor.close();

                        if (boardId == -1) return;

                        // Step 2: Delete old entries
                        dbHelper.deleteEntriesForBoard(boardId);

                        // Step 3: Save non-empty fields
                        LinearLayout finalEntryList = findViewById(R.id.entryList);
                        for (int i = 0; i < finalEntryList.getChildCount(); i++) {
                            if (finalEntryList.getChildAt(i) instanceof EditText) {
                                EditText entryField = (EditText) finalEntryList.getChildAt(i);
                                String text = entryField.getText().toString().trim();
                                if (!text.isEmpty()) {
                                    dbHelper.addEntry(boardId, text);
                                }
                            }
                        }

                        // Optional feedback
                        Toast.makeText(this, "Entries updated!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        //Add entry button
        addEntryButton.setOnClickListener(v -> {
            ContextThemeWrapper newContext = new ContextThemeWrapper(this, R.style.NoUnderlineEditText);
            EditText entryField = new EditText(newContext);
            entryField.setHint("New Entry");
            entryField.setBackgroundResource(R.drawable.edittext_border);
            entryField.setPadding(16, 16, 16, 16);
            entryField.setTextColor(getResources().getColor(android.R.color.black));
            entryField.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            entryList.addView(entryField);
        });

        //Add board button
        addBoardButton.setOnClickListener(v -> {
            if (currentlySelectedBoard != null) {
                currentlySelectedBoard.setBackgroundResource(android.R.drawable.list_selector_background);
            }

            // Use default context here, no special style needed for AlertDialog input
            EditText input = new EditText(this);

            new AlertDialog.Builder(this)
                    .setTitle("New Board")
                    .setMessage("Enter board name:")
                    .setView(input)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String boardName = input.getText().toString().trim();
                        if (!boardName.isEmpty()) {
                            long boardId = dbHelper.addBoard(boardName);
                            loadBoards(boardListContainer, addBoardButton);

                            // Highlight the newly added board
                            for (int i = 0; i < boardListContainer.getChildCount(); i++) {
                                if (boardListContainer.getChildAt(i) instanceof TextView) {
                                    TextView tv = (TextView) boardListContainer.getChildAt(i);
                                    if (tv.getText().toString().equals(boardName)) {
                                        tv.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                                        currentlySelectedBoard = tv;
                                        break;
                                    }
                                }
                            }

                            entryList.removeAllViews();

                            ContextThemeWrapper newContext = new ContextThemeWrapper(this, R.style.NoUnderlineEditText);
                            for (int i = 0; i < 5; i++) {
                                EditText entryField = new EditText(newContext);
                                entryField.setHint("New Entry" );
                                entryField.setBackgroundResource(R.drawable.edittext_border);
                                entryField.setPadding(16, 16, 16, 16);
                                entryField.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                entryList.addView(entryField);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadBoards(LinearLayout boardListContainer, ImageButton addBoardButton) {
        boardListContainer.removeAllViews();

        Cursor cursor = dbHelper.getAllBoards();
        while (cursor.moveToNext()) {
            long boardId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String boardName = cursor.getString(cursor.getColumnIndexOrThrow("name"));

            TextView boardView = new TextView(this);
            boardView.setText(boardName);
            boardView.setPadding(8, 8, 8, 8);
            boardView.setTextColor(Color.BLACK);
            boardView.setBackgroundResource(android.R.drawable.list_selector_background);
            boardView.setClickable(true);

            boardView.setOnClickListener(v -> {
                if (currentlySelectedBoard != null) {
                    currentlySelectedBoard.setBackgroundResource(android.R.drawable.list_selector_background);
                }

                boardView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                currentlySelectedBoard = boardView;

                LinearLayout entryList = findViewById(R.id.entryList);
                entryList.removeAllViews();

                Cursor entriesCursor = dbHelper.getEntriesForBoard(boardId);
                int count = 0;

                ContextThemeWrapper newContext = new ContextThemeWrapper(this, R.style.NoUnderlineEditText);

                while (entriesCursor.moveToNext()) {
                    String entryText = entriesCursor.getString(entriesCursor.getColumnIndexOrThrow("text"));

                    EditText entryField = new EditText(newContext);
                    entryField.setText(entryText);
                    entryField.setBackgroundResource(R.drawable.edittext_border);
                    entryField.setPadding(16, 16, 16, 16);
                    entryField.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    entryList.addView(entryField);
                    count++;
                }
                entriesCursor.close();

                for (int i = count; i < 5; i++) {
                    EditText entryField = new EditText(newContext);
                    entryField.setHint("New Entry" );
                    entryField.setBackgroundResource(R.drawable.edittext_border);
                    entryField.setPadding(16, 16, 16, 16);
                    entryField.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    entryList.addView(entryField);
                }
            });

            boardListContainer.addView(boardView);
        }
        cursor.close();

        boardListContainer.addView(addBoardButton);
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
