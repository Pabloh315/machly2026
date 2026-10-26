package com.machly.app.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.machly.app.R;
import com.machly.app.db.DbHelper;
import com.machly.app.models.User;
import com.machly.app.receivers.ConnectivityReceiver;
import com.machly.app.sync.SyncManager;
import com.machly.app.ui.adapters.UserAdapter;
import com.machly.app.utils.NetworkUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private DbHelper dbHelper;
    private ConnectivityReceiver connectivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DbHelper(this);
        rvUsers = findViewById(R.id.rvUsers);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty = findViewById(R.id.tvEmpty);
        FloatingActionButton fabAddUser = findViewById(R.id.fabAddUser);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(this);
        rvUsers.setAdapter(adapter);

        loadUsersFromDb();

        if (NetworkUtils.isOnline(this)) {
            syncData();
        }

        swipeRefresh.setOnRefreshListener(this::syncData);

        fabAddUser.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditUserActivity.class));
        });

        connectivityReceiver = new ConnectivityReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsersFromDb();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityReceiver);
    }

    private void loadUsersFromDb() {
        List<User> users = dbHelper.getAllUsers();
        if (users.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
            adapter.setUsers(users);
        }
    }

    private void syncData() {
        swipeRefresh.setRefreshing(true);
        SyncManager.getInstance().fetchAndSaveUsers(this, new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    loadUsersFromDb();
                    Toast.makeText(MainActivity.this, "Sync complete", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Sync failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
