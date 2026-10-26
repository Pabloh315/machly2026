package com.machly.app.sync;

import android.content.Context;
import android.util.Log;

import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.db.DbHelper;
import com.machly.app.models.Role;
import com.machly.app.models.RoleResponse;
import com.machly.app.models.User;
import com.machly.app.models.UserResponse;
import com.machly.app.storage.PrefsManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {
    private static SyncManager instance;
    private static final String TAG = "MachlySync";

    private SyncManager() {}

    public static synchronized SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public void fetchAndSaveUsers(Context context, SyncCallback callback) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        DbHelper dbHelper = new DbHelper(context);
        PrefsManager prefs = new PrefsManager(context);

        // 1. Fetch Roles
        api.getRoles().enqueue(new Callback<List<RoleResponse>>() {
            @Override
            public void onResponse(Call<List<RoleResponse>> call, Response<List<RoleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RoleResponse> roles = response.body();
                    
                    // 2. Fetch Users
                    api.getUsers().enqueue(new Callback<List<UserResponse>>() {
                        @Override
                        public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> responseUsers) {
                            if (responseUsers.isSuccessful() && responseUsers.body() != null) {
                                List<UserResponse> users = responseUsers.body();
                                
                                // Save to DB in transaction
                                new Thread(() -> {
                                    try {
                                        // Save Roles
                                        for (RoleResponse r : roles) {
                                            Role localRole = new Role(r.getRoleId(), r.getName(), r.getDescription());
                                            dbHelper.insertOrUpdateRole(localRole);
                                        }

                                        // Save Users
                                        for (UserResponse u : users) {
                                            long localRoleId = dbHelper.getLocalRoleIdByServerId(u.getRole().getRoleId());
                                            if (localRoleId != -1) {
                                                User localUser = new User();
                                                localUser.setUserIdServer(u.getUserId());
                                                localUser.setFullName(u.getFullName());
                                                localUser.setEmail(u.getEmail());
                                                // If server sends hash, use it. Else keep existing or handle appropriately.
                                                // Assuming server sends hash for sync
                                                localUser.setPasswordHash(u.getPasswordHash() != null ? u.getPasswordHash() : ""); 
                                                localUser.setRoleIdLocal(localRoleId);
                                                localUser.setLastUpdated(u.getLastUpdated());
                                                
                                                dbHelper.insertOrUpdateUser(localUser);
                                            }
                                        }

                                        prefs.setLastSync(System.currentTimeMillis());
                                        Log.d(TAG, "Sync successful");
                                        if (callback != null) callback.onSuccess();

                                    } catch (Exception e) {
                                        Log.e(TAG, "Sync error saving to DB", e);
                                        if (callback != null) callback.onError(e);
                                    }
                                }).start();

                            } else {
                                if (callback != null) callback.onError(new Exception("Failed to fetch users"));
                            }
                        }

                        @Override
                        public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                            if (callback != null) callback.onError(t);
                        }
                    });

                } else {
                    if (callback != null) callback.onError(new Exception("Failed to fetch roles"));
                }
            }

            @Override
            public void onFailure(Call<List<RoleResponse>> call, Throwable t) {
                if (callback != null) callback.onError(t);
            }
        });
    }
}
