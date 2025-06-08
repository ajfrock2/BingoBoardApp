package com.example.bingo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BingoDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bingo.db";
    private static final int DATABASE_VERSION = 1;

    public BingoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the boards table with an auto-incrementing primary key and a name field
        db.execSQL(
                "CREATE TABLE boards (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL)"
        );

        // Create the entries table, linking each entry to a board via board_id foreign key
        db.execSQL(
                "CREATE TABLE entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "board_id INTEGER NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "FOREIGN KEY(board_id) REFERENCES boards(id) ON DELETE CASCADE)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop existing tables and recreate when upgrading database version
        db.execSQL("DROP TABLE IF EXISTS entries");
        db.execSQL("DROP TABLE IF EXISTS boards");
        onCreate(db);
    }

    /**
     * Adds a new board with the given name.
     * @param boardName The name/title of the board.
     * @return The row ID of the newly inserted board, or -1 if an error occurred.
     */
    public long addBoard(String boardName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", boardName);

        // Insert the new board and return its ID
        return db.insert("boards", null, values);
    }

    /**
     * Adds a new entry (task) linked to a specific board.
     * @param boardId The ID of the board this entry belongs to.
     * @param entryText The text content of the entry/task.
     * @return The row ID of the newly inserted entry, or -1 if an error occurred.
     */
    public long addEntry(long boardId, String entryText) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("board_id", boardId);
        values.put("text", entryText);

        // Insert the new entry and return its ID
        return db.insert("entries", null, values);
    }


    //Returns all the associated entries of a passed in board
    public List<String> getEntryTextsByBoardName(String boardName) {
        List<String> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Step 1: Get board ID from name
        Cursor boardCursor = db.rawQuery("SELECT id FROM boards WHERE name = ?", new String[]{boardName});
        if (boardCursor.moveToFirst()) {
            long boardId = boardCursor.getLong(0);
            boardCursor.close();

            // Step 2: Fetch all text entries for that board ID
            Cursor entryCursor = db.rawQuery("SELECT text FROM entries WHERE board_id = ?", new String[]{String.valueOf(boardId)});
            while (entryCursor.moveToNext()) {
                entries.add(entryCursor.getString(0));
            }
            entryCursor.close();
        } else {
            boardCursor.close();
        }

        return entries;
    }

    /**
     * Deletes all entries associated with a specific board.
     * Useful for clearing and updating all tasks of a board.
     * @param boardId The ID of the board whose entries should be deleted.
     * @return The number of rows deleted.
     */
    public int deleteEntriesForBoard(long boardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("entries", "board_id = ?", new String[]{String.valueOf(boardId)});
    }

    /**
     * Retrieves a Cursor over all boards.
     * Useful for populating UI elements like a sidebar list of boards.
     * @return Cursor containing all board rows.
     */
    public Cursor getAllBoards() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("boards", null, null, null, null, null, null);
    }

    /**
     * Retrieves a Cursor over all entries/tasks for a given board.
     * Useful for populating the right side grid or list with the board's tasks.
     * @param boardId The ID of the board whose entries should be fetched.
     * @return Cursor containing all entries for the specified board.
     */
    public Cursor getEntriesForBoard(long boardId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("entries", null, "board_id = ?", new String[]{String.valueOf(boardId)}, null, null, null);
    }
}
