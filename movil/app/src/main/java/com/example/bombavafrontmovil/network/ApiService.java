package com.example.bombavafrontmovil.network;

import com.example.bombavafrontmovil.models.*;
import java.util.List;

import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ApiService {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/auth/me")
    Call<User> obtenerPerfil(@Header("Authorization") String token);

    @GET("/api/inventory/ships")
    Call<List<UserShip>> obtenerInventarioBarcos(@Header("Authorization") String token);

    @PATCH("/api/inventory/ships/{shipId}/equip")
    Call<UserShip> equiparArma(@Path("shipId") String shipId, @Header("Authorization") String token, @Body EquipWeaponRequest request);

    @POST("/api/inventory/decks")
    Call<Void> crearMazoFlota(@Header("Authorization") String token, @Body FleetConfigRequest request);

    @PATCH("/api/inventory/decks/{id}/activate")
    Call<Void> activarMazo(@Header("Authorization") String token, @Path("id") String deckId);

    @GET("/api/inventory/decks")
    Call<List<FleetConfigRequest>> obtenerMazo(@Header("Authorization") String token);
}