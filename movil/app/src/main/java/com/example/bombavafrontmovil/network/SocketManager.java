package com.example.bombavafrontmovil.network;

import android.util.Log;

import java.net.URISyntaxException;
import java.util.Collections;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SOCKET";
    private static final String SERVER_URL = "http://10.0.2.2:3000";

    private static SocketManager instance;
    private Socket mSocket;
    private String ultimoToken;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public synchronized void conectar(String token) {
        try {
            // Si ya hay un socket conectado con el mismo token, reutilizamos
            if (mSocket != null && mSocket.connected() && token != null && token.equals(ultimoToken)) {
                Log.d(TAG, "Socket ya conectado, se reutiliza");
                return;
            }

            // Si había un socket anterior, lo destruimos por completo
            if (mSocket != null) {
                Log.d(TAG, "Destruyendo socket anterior antes de reconectar");
                try {
                    mSocket.off();
                    mSocket.disconnect();
                    mSocket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error cerrando socket anterior", e);
                }
                mSocket = null;
            }

            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.extraHeaders = Collections.singletonMap(
                    "Authorization",
                    Collections.singletonList("Bearer " + token)
            );

            ultimoToken = token;
            mSocket = IO.socket(SERVER_URL, options);

            mSocket.on(Socket.EVENT_CONNECT, args ->
                    Log.d(TAG, "Conectado al servidor")
            );

            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                if (args != null && args.length > 0 && args[0] != null) {
                    Log.e(TAG, "Error de conexión: " + args[0]);
                } else {
                    Log.e(TAG, "Error de conexión");
                }
            });

            mSocket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d(TAG, "Socket desconectado")
            );

            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "URI inválida del socket", e);
        }
    }

    public synchronized Socket getSocket() {
        return mSocket;
    }

    public synchronized boolean estaConectado() {
        return mSocket != null && mSocket.connected();
    }

    public synchronized void desconectar() {
        if (mSocket != null) {
            try {
                mSocket.off();
                mSocket.disconnect();
                mSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error al desconectar socket", e);
            } finally {
                mSocket = null;
            }
        }
    }
}