package com.machly.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.machly.app.R;
import com.machly.app.db.DbHelper;
import com.machly.app.models.User;

public class UserDetailActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvRole;
    private MaterialButton btnEdit;
    private DbHelper dbHelper;
    private int userIdLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        dbHelper = new DbHelper(this);

        tvName = findViewById(R.id.tvDetailName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvRole = findViewById(R.id.tvDetailRole);
        btnEdit = findViewById(R.id.btnEdit);

        userIdLocal = getIntent().getIntExtra("user_id_local", -1);
        if (userIdLocal != -1) {
            loadUser();
        }

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditUserActivity.class);
            intent.putExtra("user_id_local", userIdLocal);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userIdLocal != -1) {
            loadUser();
        }
    }

    private void loadUser() {
        User user = dbHelper.getUserByIdLocal(userIdLocal);
        if (user != null) {
            tvName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            tvRole.setText(user.getRoleName());
        }
    }
}
