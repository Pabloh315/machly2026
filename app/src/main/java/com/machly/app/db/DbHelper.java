package com.machly.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.machly.app.AppConstants;
import com.machly.app.models.Role;
import com.machly.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    public DbHelper(Context context) {
        super(context, AppConstants.DB_NAME, null, AppConstants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ROLES_TABLE = "CREATE TABLE " + AppConstants.TABLE_ROLES + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "roleIdServer INTEGER,"
                + "name TEXT NOT NULL,"
                + "description TEXT"
                + ")";
        db.execSQL(CREATE_ROLES_TABLE);

        String CREATE_USERS_TABLE = "CREATE TABLE " + AppConstants.TABLE_USERS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "userIdServer INTEGER,"
                + "fullName TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "passwordHash TEXT NOT NULL,"
                + "roleIdLocal INTEGER NOT NULL,"
                + "lastUpdated INTEGER,"
                + "FOREIGN KEY(roleIdLocal) REFERENCES " + AppConstants.TABLE_ROLES + "(id)"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + AppConstants.TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + AppConstants.TABLE_ROLES);
        onCreate(db);
    }

    // Roles
    public long insertOrUpdateRole(Role role) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        if (role.getRoleIdServer() != null) {
            // Try to update by server ID
            ContentValues values = role.toContentValues();
            int rows = db.update(AppConstants.TABLE_ROLES, values, "roleIdServer = ?", new String[]{String.valueOf(role.getRoleIdServer())});
            if (rows > 0) {
                // Fetch local ID
                Cursor cursor = db.query(AppConstants.TABLE_ROLES, new String[]{"id"}, "roleIdServer = ?", new String[]{String.valueOf(role.getRoleIdServer())}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    cursor.close();
                }
                return id;
            }
        }

        // Insert if not updated
        return db.insert(AppConstants.TABLE_ROLES, null, role.toContentValues());
    }

    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + AppConstants.TABLE_ROLES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Role role = new Role();
                role.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                if (!cursor.isNull(cursor.getColumnIndexOrThrow("roleIdServer"))) {
                    role.setRoleIdServer(cursor.getInt(cursor.getColumnIndexOrThrow("roleIdServer")));
                }
                role.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                role.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                roles.add(role);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return roles;
    }

    public void deleteAllRoles() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(AppConstants.TABLE_ROLES, null, null);
    }

    public long getLocalRoleIdByServerId(int serverId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(AppConstants.TABLE_ROLES, new String[]{"id"}, "roleIdServer = ?", new String[]{String.valueOf(serverId)}, null, null, null);
        long id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0);
            }
            cursor.close();
        }
        return id;
    }

    // Users
    public long insertOrUpdateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        // Try to find by email (unique key) first to update local record correctly
        User existing = getUserByEmail(user.getEmail());
        if (existing != null) {
            ContentValues values = user.toContentValues();
            db.update(AppConstants.TABLE_USERS, values, "email = ?", new String[]{user.getEmail()});
            return existing.getId();
        }

        return db.insert(AppConstants.TABLE_USERS, null, user.toContentValues());
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        // Left join to get role name
        String selectQuery = "SELECT u.*, r.name as roleName FROM " + AppConstants.TABLE_USERS + " u " +
                "LEFT JOIN " + AppConstants.TABLE_ROLES + " r ON u.roleIdLocal = r.id";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                if (!cursor.isNull(cursor.getColumnIndexOrThrow("userIdServer"))) {
                    user.setUserIdServer(cursor.getInt(cursor.getColumnIndexOrThrow("userIdServer")));
                }
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow("fullName")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow("passwordHash")));
                user.setRoleIdLocal(cursor.getLong(cursor.getColumnIndexOrThrow("roleIdLocal")));
                user.setLastUpdated(cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdated")));
                user.setRoleName(cursor.getString(cursor.getColumnIndexOrThrow("roleName")));
                users.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return users;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(AppConstants.TABLE_USERS, null, "email = ?", new String[]{email}, null, null, null);
        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                if (!cursor.isNull(cursor.getColumnIndexOrThrow("userIdServer"))) {
                    user.setUserIdServer(cursor.getInt(cursor.getColumnIndexOrThrow("userIdServer")));
                }
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow("fullName")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow("passwordHash")));
                user.setRoleIdLocal(cursor.getLong(cursor.getColumnIndexOrThrow("roleIdLocal")));
                user.setLastUpdated(cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdated")));
            }
            cursor.close();
        }
        return user;
    }

    public User getUserByIdLocal(int idLocal) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Join to get role name
        String query = "SELECT u.*, r.name as roleName FROM " + AppConstants.TABLE_USERS + " u " +
                "LEFT JOIN " + AppConstants.TABLE_ROLES + " r ON u.roleIdLocal = r.id WHERE u.id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idLocal)});
        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                if (!cursor.isNull(cursor.getColumnIndexOrThrow("userIdServer"))) {
                    user.setUserIdServer(cursor.getInt(cursor.getColumnIndexOrThrow("userIdServer")));
                }
                user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow("fullName")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow("passwordHash")));
                user.setRoleIdLocal(cursor.getLong(cursor.getColumnIndexOrThrow("roleIdLocal")));
                user.setLastUpdated(cursor.getLong(cursor.getColumnIndexOrThrow("lastUpdated")));
                user.setRoleName(cursor.getString(cursor.getColumnIndexOrThrow("roleName")));
            }
            cursor.close();
        }
        return user;
    }

    public void deleteAllUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(AppConstants.TABLE_USERS, null, null);
    }
}
