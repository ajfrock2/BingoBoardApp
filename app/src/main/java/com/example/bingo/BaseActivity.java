package com.example.bingo;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    /**
     * Call this in your activity’s onCreate() *after* setContentView()
     */
    protected void setupDrawer(int drawerLayoutId, int navViewId) {
        drawerLayout = findViewById(drawerLayoutId);
        navigationView = findViewById(navViewId);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();

            int id = item.getItemId();

            if (id == R.id.menu_board && !(this instanceof Board)) {
                startActivity(new Intent(this, Board.class));
                return true;
            }
            else if (id == R.id.menu_entries && !(this instanceof BoardEntries)) {
                startActivity(new Intent(this, BoardEntries.class));
                return true;
            }

            return false;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}