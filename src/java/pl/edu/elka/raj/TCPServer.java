package pl.edu.elka.raj;

import pl.edu.elka.models.Node;
import pl.edu.elka.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple TCP server used to control the robot remotely. Allows multiple and concurrent clients.
 *
 * @author Gregorio
 */
public class TCPServer implements Runnable {
    private ServerSocket socket;
    private boolean active = true;


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public TCPServer(int port) {
        try {
            socket = new ServerSocket(port);
        } catch (Exception e) {
            Log.LogError(Log.SUBTYPE.SYSTEM, "Error creating server: " + e.getMessage());
        }
    }

    /**
     * Writes data to all clients, checking if they're still alive.
     *
     * @param data
     */
    public void write(String data) {
        for (Node client : NetworkController.nodes.values()) {
            if (client.getSocket().isClosed()) {
                NetworkController.nodes.remove(client.getPid());
                continue;
            }
            try {
                DataOutputStream outToClient = new DataOutputStream(client.getSocket().getOutputStream());
                outToClient.writeBytes(data);
            } catch (IOException e) {
                NetworkController.nodes.remove(client.getPid());
                Log.LogError(Log.SUBTYPE.SYSTEM, "Error writing to clients: " + e.getMessage());
            }
        }

    }

    /**
     * Closes the server
     */
    public void close() {
        try {
            socket.close();
            Log.LogError(Log.SUBTYPE.SYSTEM, "TCP server shutting down");
        } catch (IOException e) {
            Log.LogError(Log.SUBTYPE.SYSTEM, "Error stopping server: " + e.getMessage());
        }
    }

    /*
     * Implementation of the run method that listens for connections and launches a worker thread for
     * each client.
     *
     * @see java.lang.Runnable#run()
     */

    public void run() {
        Log.LogEvent(Log.SUBTYPE.SYSTEM, "Server starting");
        while (active) {
            Socket clientSocket = null;
            try {
                clientSocket = socket.accept();
                Node newClient = new Node(null, clientSocket);
                new Thread(new TCPClientWorker(newClient)).start();
                Log.LogEvent(Log.SUBTYPE.SYSTEM, "Client connected to server");
                for (Node client : NetworkController.nodes.values()) {
                    if (client.getSocket().isClosed()) {
                        NetworkController.nodes.remove(client.getPid());
                    }
                }
            } catch (IOException e) {
                Log.LogError(Log.SUBTYPE.SYSTEM, "Connection error: " + e.getMessage());
            }
        }
    }
}