package pl.edu.elka.raj;

import com.google.gson.Gson;
import pl.edu.elka.models.Message;
import pl.edu.elka.models.Node;
import pl.edu.elka.util.IdGenerator;
import pl.edu.elka.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by carol on 20/04/2016.
 */
public class NetworkController {
    private static TCPServer server;
    private static TCPClient client;
    public static Map<String, Node> clients;
    private static boolean pendingElection = true;
    private static Timer timer;
    private static String coordinator;
    private static boolean isTimerRunning;

    public NetworkController() {
        server = new TCPServer(Integer.parseInt(Main.propertiesManager.getProperty("port")));
        client = new TCPClient(getParent());
        clients = Collections.synchronizedMap(new HashMap<String, Node>());
        timer = new Timer();
        isTimerRunning = false;
    }

    public static void start() {
        client = new TCPClient(getParent());
        Thread serverThread = new Thread(server);
        serverThread.start();
        Thread clientThread = new Thread(client);
        clientThread.start();
    }

    public static void restartClient() {
        if (client.getServer() != null) {
            client.getServer().close();
        }
        client = new TCPClient(getParent());
        Thread clientThread = new Thread(client);
        clientThread.start();

    }

    private static int getIndex() {
        int result = 0;
        String[] nodes = Main.propertiesManager.getArray("nodes");
        String location = Main.propertiesManager.getProperty("address") + ":" + Main.propertiesManager.getProperty("port");
        for (int i = 0; i < nodes.length; i++) {
            if (location.equals(nodes[i])) {
                return i;
            }
        }
        ;
        return -1;
    }

    private static String getParent() {
        int index = getIndex();
        int parentIndex = index;
        String[] nodes = Main.propertiesManager.getArray("nodes");
        do
            parentIndex = IdGenerator.getRandomInt(0, nodes.length - 1);
        while (parentIndex == index);
        return nodes[parentIndex];
    }

    ;

    public static void processMessage(Node node, String line) throws Exception {
        Message message = new Gson().fromJson(line, Message.class);
        if (message == null || message.getTtl() < 1 || message.getFrom().equals(Main.propertiesManager.getProperty("pid"))) {
            return;
        } else {
            message.setTtl(message.getTtl() - 1);
        }
        if (message.getType() != Message.Type.HANDSHAKE && !message.getTo().equals(Main.pid)) {
            Log.LogEvent(Log.SUBTYPE.ROUTING, "Received message for someone else, passing it along");
            send(message, node);
            if (!message.getTo().equals("-1")) {
                return;
            }
        }
        Log.LogEvent(Log.SUBTYPE.ROUTING, "Message received: " + message.toString());
        String[] allNodes = Main.propertiesManager.getArray("nodes");
        switch (message.getType()) {
            case HANDSHAKE: {
                node.setPid(message.getFrom());
                // The client of the server I'm connected to is trying to connect to my server
                if (message.getFrom().equals(client.getServer().getPid())) {
                    Log.LogWarning(Log.SUBTYPE.ROUTING, "Loop detected");
                    restartClient();
                    node.getSocket().close();
                } else {
                    clients.put(message.getFrom(), node);
                    Message ackHandshakeMsg = new Message(Message.Type.ACKHANDSHAKE, Main.pid, node.getPid(), null);
                    node.write(ackHandshakeMsg);
                }
                break;
            }
            case ACKHANDSHAKE: {
                // The client of the server I'm connected to is trying to connect to my server
                if (clients.containsKey(message.getFrom())) {
                    Log.LogWarning(Log.SUBTYPE.ROUTING, "Loop detected");
                    restartClient();
                    node.getSocket().close();
                } else {
                    client.getServer().setPid(message.getFrom());
                    election();
                }
                break;
            }
            case ELECTION: {
                if (pendingElection) return;
                // send OK
                Message answerMessage = new Message(Message.Type.ANSWER, Main.pid, node.getPid(), null);
                send(answerMessage, null);
                // continue election
                election();
                break;
            }
            case ANSWER: {
                // stop election
                pendingElection = false;
                Log.LogEvent(Log.SUBTYPE.ELECTION, "Received OK, stopping election");
                break;
            }
            case COORDINATOR: {
                if(Integer.parseInt(message.getFrom())>Integer.parseInt(Main.propertiesManager.getProperty("pid"))){
                    election();
                    return;
                }
                // stop election, we have coordinator
                pendingElection = false;
                coordinator = message.getFrom();
                Log.LogEvent(Log.SUBTYPE.ELECTION, "Received coordinator, node #" + coordinator + " is the coordinator");
                break;
            }
            default:
                return;
        }

    }

    public static void election() {
        if(isTimerRunning){
            timer.cancel();
        }
        coordinator = "-1";
        pendingElection = true;
        Log.LogEvent(Log.SUBTYPE.ELECTION, "Election time!");
        for (int i = 0; i < Integer.parseInt(Main.propertiesManager.getProperty("pid")); i++) {
            Message electionMsg = new Message(Message.Type.ELECTION, Main.pid, i + "", null);
            send(electionMsg, null);
        }
        isTimerRunning = true;
        timer.schedule(new TimerTask() {
            public void run() {
                isTimerRunning = false;
                if (!pendingElection) {
                    return;
                }
                Message coordinatorMsg = new Message(Message.Type.COORDINATOR, Main.pid, "-1", null);
                send(coordinatorMsg, null);
                Log.LogEvent(Log.SUBTYPE.ELECTION, "No response, I am the coordinator");
                pendingElection = false;
                coordinator = Main.propertiesManager.getProperty("pid");
            }
        }, Integer.parseInt(Main.propertiesManager.getProperty("electionTimeout")));

    }

    public static void send(Message message, Node excluded) {
        for (Node node : clients.values()) {
            if (!node.equals(excluded)) {
                try {
                    node.write(message);
                } catch (Exception e) {
                    Log.LogError(Log.SUBTYPE.SYSTEM, e.getMessage());
                }
            }
        }
        if (client.getServer()!=null && !client.getServer().equals(excluded)) {
            try {
                client.getServer().write(message);
            } catch (Exception e) {
                Log.LogError(Log.SUBTYPE.SYSTEM, e.getMessage());
            }
        }

    }
}
