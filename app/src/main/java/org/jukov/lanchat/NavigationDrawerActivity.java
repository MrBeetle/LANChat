package org.jukov.lanchat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.jukov.lanchat.adapter.ChatAdapter;
import org.jukov.lanchat.service.ServiceHelper;

import static org.jukov.lanchat.service.ServiceHelper.IntentConstants.PEOPLE_AROUND_ACTION;
import static org.jukov.lanchat.service.ServiceHelper.IntentConstants.EXTRA_PEOPLE_AROUND;

/**
 * Created by jukov on 21.04.2016.
 */
public abstract class NavigationDrawerActivity extends BaseActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle actionBarDrawerToggle;
    View navigationDrawerHeaderView;
    TextView textViewMode;

    int currentNavigationId;

    ChatAdapter chatAdapter;

    private BroadcastReceiver broadcastReceiverStatus;

    protected int peopleAround = 0;

    public abstract boolean onNavigationItemSelected(MenuItem item);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiverStatus);
    }

    @Override
    public void onBackPressed() {
        Log.d(getClass().getSimpleName(), "onBackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        if (drawer != null)
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
                return;
            }
        super.onBackPressed();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(getClass().getSimpleName(), "onSharedPreferenceChanged() " + key);
        switch (key) {
            case "name":
                String name = sharedPreferences.getString("name", getString(R.string.default_name));
                TextView textView = (TextView) navigationDrawerHeaderView.findViewById(R.id.textViewName);
                ServiceHelper.changeName(this, name);
                textView.setText(getString(R.string.nav_header_hello, name));
                break;
        }
    }

    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }

    protected void initViews() {
        super.initViews();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                InputMethodManager inputMethodManager = (InputMethodManager)
                        NavigationDrawerActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                //noinspection ConstantConditions
                inputMethodManager.hideSoftInputFromWindow(
                        NavigationDrawerActivity.this.getCurrentFocus().getWindowToken(), 0);
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        navigationView = (NavigationView) findViewById(R.id.navigationView);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationDrawerHeaderView = navigationView.getHeaderView(0);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        TextView textView = (TextView) navigationDrawerHeaderView.findViewById(R.id.textViewName);
        textView.setText(getString(R.string.nav_header_hello, sharedPreferences.getString("name", getString(R.string.default_name))));

        textViewMode = (TextView) navigationDrawerHeaderView.findViewById(R.id.textViewPeopleAround);
        textViewMode.setText(getString(R.string.nav_header_people_around, peopleAround));
    }

    void initBroadcastReceiver() {
        broadcastReceiverStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                if (intent.getAction().equals(PEOPLE_AROUND_ACTION)) {
                    peopleAround = intent.getIntExtra(EXTRA_PEOPLE_AROUND, -1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewMode.setText(getString(R.string.nav_header_people_around, peopleAround));
                        }
                    });
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PEOPLE_AROUND_ACTION);
        registerReceiver(broadcastReceiverStatus, intentFilter);
    }

}