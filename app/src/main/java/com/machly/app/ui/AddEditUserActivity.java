package com.machly.app.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.machly.app.R;
import com.machly.app.db.DbHelper;
import com.machly.app.models.Role;
import com.machly.app.models.User;
import com.machly.app.utils.BCryptUtils;
import com.machly.app.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class AddEditUserActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spinnerRole;
    private MaterialButton btnSave;
    private DbHelper dbHelper;
    private int userIdLocal = -1;
    private List<Role> roles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_user);

        dbHelper = new DbHelper(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSave = findViewById(R.id.btnSave);

        loadRoles();

        userIdLocal = getIntent().getIntExtra("user_id_local", -1);
        if (userIdLocal != -1) {
            loadUserForEdit();
        }

        btnSave.setOnClickListener(v -> saveUser());
    }

    private void loadRoles() {
        roles = dbHelper.getAllRoles();
        List<String> roleNames = new ArrayList<>();
        for (Role r : roles) {
            roleNames.add(r.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadUserForEdit() {
        User user = dbHelper.getUserByIdLocal(userIdLocal);
        if (user != null) {
            etName.setText(user.getFullName());
            etEmail.setText(user.getEmail());
            etEmail.setEnabled(false); // Can't change email usually as it's ID
            
            // Set spinner selection
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getId() == user.getRoleIdLocal()) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        }
    }

    private void saveUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        User user;
        if (userIdLocal != -1) {
            user = dbHelper.getUserByIdLocal(userIdLocal);
        } else {
            user = new User();
        }

        user.setFullName(name);
        user.setEmail(email);
        
        int selectedRolePos = spinnerRole.getSelectedItemPosition();
        if (selectedRolePos >= 0 && selectedRolePos < roles.size()) {
            user.setRoleIdLocal(roles.get(selectedRolePos).getId());
        }

        if (!password.isEmpty()) {
            user.setPasswordHash(BCryptUtils.hashPassword(password));
        } else if (userIdLocal == -1) {
            Toast.makeText(this, "Password required for new user", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: If online, call API to save user
        if (NetworkUtils.isOnline(this)) {
            // Call API create/update
            // For now, just save locally
        }

        dbHelper.insertOrUpdateUser(user);
        Toast.makeText(this, "User saved locally", Toast.LENGTH_SHORT).show();
        finish();
    }
}
