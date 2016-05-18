package pl.edu.elka.raj;

import com.google.gson.Gson;
import javafx.scene.chart.PieChart;
import pl.edu.elka.models.Message;
import pl.edu.elka.models.Node;
import pl.edu.elka.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by carol on 20/04/2016.
 */
public class TCPClient implements Runnable {

    private String address;
    private int port;
    private Socket clientSocket;
    private Node server;

    public TCPClient(String server) {
        String[] serverArray = server.split(":");
        address = serverArray[0];
        port = Integer.parseInt(serverArray[1]);

    }

    public Node getServer() {
        return server;
    }

    public void setServer(Node server) {
        this.server = server;
    }

    public void run() {
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(address, port), 1000);
            server = new Node(null, clientSocket, true);
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            Message messageSent = new Message(Message.Type.HANDSHAKE, Main.pid, null, null);
            Log.LogEvent(Log.SUBTYPE.ROUTING, "Message sent: "+messageSent);
            outToServer.write(new Gson().toJson(messageSent));
            outToServer.flush();
            while (!clientSocket.isClosed()) {
                NetworkController.processMessage(server, inFromServer.readLine());
            }
        } catch (Exception e) {
            Log.LogError(Log.SUBTYPE.CLIENTSOCKET, "Message: " + e.getMessage());
        } finally {
            NetworkController.restartClient();
        }
    }
}
