package com.example.bombavafrontmovil.network; // Ajusta el paquete si es necesario

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Pon aquí la URL base de tu servidor backend.
    // Si pruebas en el emulador de Android Studio y tu backend está en tu PC local, usa "http://10.0.2.2:tu_puerto/"
    private static final String BASE_URL = "http://10.0.2.2:3000/";
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}