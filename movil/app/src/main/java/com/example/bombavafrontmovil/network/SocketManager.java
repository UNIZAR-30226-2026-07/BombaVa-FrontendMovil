package com.example.bombavafrontmovil.network;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.util.Collections;

public class SocketManager {
    private static SocketManager instance;
    private Socket mSocket;
    // URL de tu servidor backend en el emulador
    private static final String SERVER_URL = "http://10.0.2.2:3000";

    private SocketManager() {}

    public static SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void conectar(String token) {
        if (mSocket != null && mSocket.connected()) return;

        try {
            IO.Options options = new IO.Options();
            // Enviamos el token para que el backend sepa quién es
            options.extraHeaders = Collections.singletonMap("Authorization", Collections.singletonList("Bearer " + token));

            mSocket = IO.socket(SERVER_URL, options);
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d("SOCKET", "Conectado al servidor"));
            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e("SOCKET", "Error de conexión"));

            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void desconectar() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}