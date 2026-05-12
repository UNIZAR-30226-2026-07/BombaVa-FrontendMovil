package com.example.bombavafrontmovil.network;

import com.example.bombavafrontmovil.BuildConfig;
import android.util.Log;
import java.net.URISyntaxException;
import java.util.Collections;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SOCKET";
    private static final String SERVER_URL = BuildConfig.SOCKET_URL;

    private static SocketManager instance;
    private Socket mSocket;
    private String ultimoToken;

    //  Almacenamiento temporal para atrapar el tablero
    private Object cachedStartInfo = null;

    public Object popCachedStartInfo() {
        Object temp = cachedStartInfo;
        cachedStartInfo = null; // Lo vaciamos tras recogerlo
        return temp;
    }

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public synchronized void conectar(String token) {
        try {
            if (mSocket != null && mSocket.connected() && token != null && token.equals(ultimoToken)) {
                return;
            }

            if (mSocket != null) {
                try {
                    mSocket.off();
                    mSocket.disconnect();
                    mSocket.close();
                } catch (Exception e) { e.printStackTrace(); }
            }

            IO.Options options = new IO.Options();
            options.extraHeaders = Collections.singletonMap("Authorization", Collections.singletonList("Bearer " + token));

            ultimoToken = token;
            mSocket = IO.socket(SERVER_URL, options);


            // Escuchamos el evento desde el mismo instante en que se crea el socket.
            // Así es imposible que se pierda, esté la pantalla que esté abierta.
            mSocket.on("match:startInfo", args -> {
                if (args != null && args.length > 0) {
                    Log.e("DEBUG_CARRERA", "¡TRAMPA GLOBAL ACTIVADA! Paquete match:startInfo capturado antes de tiempo.");
                    cachedStartInfo = args[0];
                }
            });

            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Conectado al servidor"));
            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "Error de conexión"));

            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "URI inválida del socket", e);
        }
    }

    public synchronized Socket getSocket() { return mSocket; }
    public synchronized boolean estaConectado() { return mSocket != null && mSocket.connected(); }

    public synchronized void desconectar() {
        if (mSocket != null) {
            try {
                mSocket.off();
                mSocket.disconnect();
                mSocket.close();
                cachedStartInfo = null; // Limpiar caché al desconectar
            } catch (Exception e) { e.printStackTrace(); }
            mSocket = null;
        }
    }
}