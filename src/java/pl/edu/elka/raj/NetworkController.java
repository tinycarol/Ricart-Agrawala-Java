package pl.edu.elka.raj;

import com.google.gson.Gson;
import pl.edu.elka.models.Message;
import pl.edu.elka.models.Node;
import pl.edu.elka.util.IdGenerator;
import pl.edu.elka.util.Log;
import pl.edu.elka.util.PropertiesManager;

import java.util.*;

/**
 * Created by carol on 20/04/2016.
 */
public class NetworkController {
    private static TCPServer server;
    private static TCPClient client;
    public static Map<String, Node> clients;
    private static volatile boolean pendingElection = true;
    private static Timer electionTimer;
    private static boolean isElectionTimerRunning;
    private static String coordinator;
    private enum STATE {RELEASED, WANTED, HELD}

    private static STATE state;
    private static PriorityQueue<Message> messages;
    private static HashSet<String> answered;
    private static Timer criticalSectionTimer;
    private static boolean isCSTimerRunning;
    private static Timer requestCSTimer;
    private static boolean isReqCSTimerRunning;
    private static Date lastRequested;
    private static Timer wantedTimeout;
    private static boolean isWantedTimeoutRunning;
    private static long coordOffset;

    private static Thread clientThread;
    private static Thread serverThread;

    public NetworkController() {
        server = new TCPServer(Integer.parseInt(PropertiesManager.getProperty("port")));
        client = new TCPClient(getParent());
        clients = Collections.synchronizedMap(new HashMap<String, Node>());
        electionTimer = new Timer();
        isElectionTimerRunning = false;
        criticalSectionTimer = new Timer();
        isCSTimerRunning = false;
        state = STATE.RELEASED;
        messages = new PriorityQueue<Message>();
        answered = new HashSet<String>();
        requestCSTimer = new Timer();
        isReqCSTimerRunning = false;
        wantedTimeout = new Timer();
        isWantedTimeoutRunning = false;
        lastRequested = new Date();
    }

    public static void start() {
        client = new TCPClient(getParent());
        serverThread = new Thread(server);
        serverThread.start();
        clientThread = new Thread(client);
        clientThread.start();
    }

    public static void restartClient() {
        try {
            client.getServer().close();
        } catch(Exception e){
            Log.LogError(Log.SUBTYPE.SYSTEM, "Error closing client: " + e.getMessage());
        }
        client = new TCPClient(getParent());
        clientThread = new Thread(client);
        clientThread.start();
    }

    private static int getIndex() {
        int result = 0;
        String[] nodes = PropertiesManager.getArray("nodes");
        String location = PropertiesManager.getProperty("address") + ":" + PropertiesManager.getProperty("port");
        for (int i = 0; i < nodes.length; i++) {
            if (location.equals(nodes[i])) {
                return i;
            }
        }
        return -1;
    }

    private static String getParent() {
        int index = getIndex();
        int parentIndex = index;
        String[] nodes = PropertiesManager.getArray("nodes");
        do
            parentIndex = IdGenerator.getRandomInt(0, nodes.length - 1);
        while (parentIndex == index);
        return nodes[parentIndex];
    }

    public static void processMessage(Node node, String line) throws Exception {
        Message message = new Gson().fromJson(line, Message.class);
        if (message == null || message.getTtl() < 1 || message.getFrom().equals(Main.pid)) {
            return;
        } else {
            message.setTtl(message.getTtl() - 1);
        }
        if (message.getType() != Message.Type.HANDSHAKE && !message.getTo().equals(Main.pid)) {
            // Not for me
            send(message, node);
            if (!message.getTo().equals("-1")) {
                return;
            }
        }
        switch (message.getType()) {
            case HANDSHAKE: {
                node.setPid(message.getFrom());
                // The client of the server I'm connected to is trying to connect to my server
                if (client.getServer() != null && message.getFrom().equals(client.getServer().getPid())) {
                    Log.LogWarning(Log.SUBTYPE.ROUTING, "Loop detected");
                    client.getServer().close();
                    node.getSocket().close();
                } else {
                    clients.put(message.getFrom(), node);
                    Message ackHandshakeMsg = new Message(Message.Type.ACKHANDSHAKE, Main.pid, message.getFrom(), null);
                    node.write(ackHandshakeMsg);
                }
                break;
            }
            case ACKHANDSHAKE: {
                // The client of the server I'm connected to is trying to connect to my server
                if (clients.containsKey(message.getFrom())) {
                    Log.LogWarning(Log.SUBTYPE.ROUTING, "Loop detected");
                    client.getServer().close();
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
                Message answerMessage = new Message(Message.Type.ANSWER, Main.pid, message.getFrom(), null);
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
                if(Integer.parseInt(message.getFrom())>Integer.parseInt(Main.pid)){
                    election();
                    return;
                }
                coordOffset =(new Date()).getTime() - message.getTimestamp().getTime();
                // stop election, we have coordinator   11
                pendingElection = false;
                coordinator = message.getFrom();
                leaveCriticalSection();
                Log.LogEvent(Log.SUBTYPE.ELECTION, "Received coordinator, node #" + coordinator + " is the coordinator. Forcing myself out of critical section.");
                requestCriticalSection((int)(Math.random()*Integer.parseInt(PropertiesManager.getProperty("maxCSDelay"))));
                break;
            }
            case WANTED: {;
                if(state == STATE.RELEASED || (lastRequested.after(message.getTimestamp()))){
                    Message response = new Message(Message.Type.ACCEPT, Main.pid, message.getFrom(), null);
                    send(response, null);
                }else{
                    messages.add(message);
                }
                break;
            }
            case ACCEPT:{
                if(state != STATE.WANTED) return;
                answered.add(message.getFrom());
                if(PropertiesManager.getArray("nodes")!=null && answered.size() == Main.nodes.length-1){
                    answered.clear();
                    enterCriticalSection();
                }
                break;
            }
            default:
                return;
        }
    }

    private static void enterCriticalSection() {
        if(pendingElection) return;
        state = STATE.HELD;
        checkTimers();
        isCSTimerRunning = true;
        Log.LogEvent(Log.SUBTYPE.CRITICALSECTION, "Entered critical section");
        criticalSectionTimer.schedule(new TimerTask() {
            public void run() {
                isCSTimerRunning = false;
                leaveCriticalSection();
                requestCriticalSection((int)Math.floor(Math.random()* Integer.parseInt(PropertiesManager.getProperty("maxCSDelay"))));
            }
        }, Integer.parseInt(PropertiesManager.getProperty("criticalSectionTimeout")));
    }

    private static void leaveCriticalSection() {
        Log.LogEvent(Log.SUBTYPE.CRITICALSECTION, "Leaving critical section");
        state = STATE.RELEASED;
        while(!messages.isEmpty()){
            Message pendingMessage = messages.remove();
            Message releasedMessage = new Message(Message.Type.ACCEPT, Main.pid, pendingMessage.getFrom(), null);
            send(releasedMessage, null);
        }
        messages.clear();
    }

    private static void requestCriticalSection(int delay){
        if(pendingElection) return;
        checkTimers();
        isReqCSTimerRunning = true;
        requestCSTimer.schedule(new TimerTask() {
            public void run() {
                Log.LogEvent(Log.SUBTYPE.CRITICALSECTION, "Requesting access to critical section");
                answered.clear();
                isReqCSTimerRunning = false;
                state = STATE.WANTED;
                Message wantedMessage = new Message(Message.Type.WANTED, Main.pid, "-1", null);
                lastRequested = new Date(wantedMessage.getTimestamp().getTime()-coordOffset);
                wantedMessage.setTimestamp(lastRequested);
                send(wantedMessage, null);
                isWantedTimeoutRunning = true;
                wantedTimeout.schedule(new TimerTask() {
                    public void run() {
                        isWantedTimeoutRunning = false;
                        Log.LogWarning(Log.SUBTYPE.CRITICALSECTION, "Token lost, starting election");
                        election();
                    }
                }, 15000* Main.nodes.length);
            }
        }, delay);
    }

    public static void election() {
        state = STATE.RELEASED;
        checkTimers();
        coordinator = "-1";
        pendingElection = true;
        Log.LogEvent(Log.SUBTYPE.ELECTION, "Election time! Forcing out of critical section");
        for (int i = 0; i < Integer.parseInt(Main.pid); i++) {
            Message electionMsg = new Message(Message.Type.ELECTION, Main.pid, i + "", null);
            send(electionMsg, null);
        }
        isElectionTimerRunning = true;
        electionTimer.schedule(new TimerTask() {
            public void run() {
                isElectionTimerRunning = false;
                if (!pendingElection) return;
                Message coordinatorMsg = new Message(Message.Type.COORDINATOR, Main.pid, "-1", null);
                send(coordinatorMsg, null);
                Log.LogEvent(Log.SUBTYPE.ELECTION, "No response, I am the coordinator");
                pendingElection = false;
                coordinator = Main.pid;
                requestCriticalSection(0);
            }
        }, Integer.parseInt(PropertiesManager.getProperty("electionTimeout")));

    }

    private static void checkTimers() {
        if(isElectionTimerRunning) {
            electionTimer.cancel();
            electionTimer = new Timer();
            isElectionTimerRunning = false;
        }
        if(isCSTimerRunning) {
            criticalSectionTimer.cancel();
            criticalSectionTimer = new Timer();
            isCSTimerRunning = false;
        }
        if(isReqCSTimerRunning) {
            requestCSTimer.cancel();
            requestCSTimer = new Timer();
            isReqCSTimerRunning = false;
        }
        if(isWantedTimeoutRunning){
            wantedTimeout.cancel();
            wantedTimeout = new Timer();
            isWantedTimeoutRunning = false;
        }
    }

    public static void send(Message message, Node excluded) {
        for (Node node : clients.values()) {
            if (!node.equals(excluded)) {
                try {
                    node.write(message);
                } catch (Exception e) {
                    Log.LogError(Log.SUBTYPE.SYSTEM, "Error writing to socket: " + e.getMessage());
                }
            }
        }
        if (client.getServer()!=null && !client.getServer().equals(excluded)) {
            try {
                client.getServer().write(message);
            } catch (Exception e) {
                NetworkController.restartClient();
                Log.LogError(Log.SUBTYPE.CLIENTSOCKET, "Error writing to socket: " + e.getMessage());
            }
        }
    }
}
