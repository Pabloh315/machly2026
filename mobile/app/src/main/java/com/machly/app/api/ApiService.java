package com.machly.app.api;

import com.machly.app.models.Booking;
import com.machly.app.models.BookingRequest;
import com.machly.app.models.DashboardStats;
import com.machly.app.models.Equipment;
import com.machly.app.models.EquipmentPage;
import com.machly.app.models.EquipmentRequest;
import com.machly.app.models.FavoriteResponse;
import com.machly.app.models.HealthResponse;
import com.machly.app.models.LoginRequest;
import com.machly.app.models.LoginResponse;
import com.machly.app.models.RegisterRequest;
import com.machly.app.models.StatusUpdateRequest;
import com.machly.app.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("health")
    Call<HealthResponse> health();

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest req);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest req);

    @GET("auth/me")
    Call<User> me();

    @GET("equipment")
    Call<EquipmentPage> getEquipment(@Query("search") String search,
                                     @Query("type") String type,
                                     @Query("city") String city,
                                     @Query("page") int page,
                                     @Query("limit") int limit);

    @GET("equipment/{id}")
    Call<Equipment> getEquipment(@Path("id") int id);

    @POST("equipment")
    Call<Equipment> createEquipment(@Body EquipmentRequest request);

    @PUT("equipment/{id}")
    Call<Equipment> updateEquipment(@Path("id") int id, @Body EquipmentRequest request);

    @GET("my/equipment")
    Call<List<Equipment>> myEquipment();

    @POST("bookings")
    Call<Booking> createBooking(@Body BookingRequest request);

    @GET("bookings")
    Call<List<Booking>> getBookings(@Query("scope") String scope);

    @PATCH("bookings/{id}/status")
    Call<Booking> updateBookingStatus(@Path("id") int id, @Body StatusUpdateRequest request);

    @GET("favorites")
    Call<List<Equipment>> getFavorites();

    @POST("favorites/{equipmentId}")
    Call<FavoriteResponse> addFavorite(@Path("equipmentId") int equipmentId);

    @DELETE("favorites/{equipmentId}")
    Call<Void> removeFavorite(@Path("equipmentId") int equipmentId);

    @GET("dashboard")
    Call<DashboardStats> dashboard();
}
