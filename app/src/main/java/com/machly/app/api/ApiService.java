package com.machly.app.api;

import com.machly.app.models.LoginRequest;
import com.machly.app.models.LoginResponse;
import com.machly.app.models.RoleResponse;
import com.machly.app.models.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest req);

    @GET("api/users")
    Call<List<UserResponse>> getUsers();

    @GET("api/roles")
    Call<List<RoleResponse>> getRoles();

    @GET("api/users/{id}")
    Call<UserResponse> getUser(@Path("id") int id);
}
