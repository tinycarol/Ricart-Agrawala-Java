package pl.edu.elka.raj;

import com.google.gson.Gson;
import pl.edu.elka.models.Message;
import pl.edu.elka.models.Node;
import pl.edu.elka.util.IdGenerator;
import pl.edu.elka.util.Log;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carol on 20/04/2016.
 */
public class NetworkController {
    private static TCPServer server;
    private static TCPClient client;
    public static Map<String, Node> nodes;

    public NetworkController(){
        server = new TCPServer(Integer.parseInt(Main.propertiesManager.getProperty("port")));
        client = new TCPClient(getParent());
        nodes = Collections.synchronizedMap(new HashMap<String, Node>());
    }

    public static void start(){
        client = new TCPClient(getParent());
        Thread serverThread = new Thread(server);
        serverThread.start();
        Thread clientThread = new Thread(client);
        clientThread.start();
    }

    public static void restartClient(){
        client = new TCPClient(getParent());
        Thread clientThread = new Thread(client);
        clientThread.start();

    }

    private static int getIndex() {
        int result = 0;
        String[] nodes = Main.propertiesManager.getArray("nodes");
        String location = Main.propertiesManager.getProperty("address")+":"+Main.propertiesManager.getProperty("port");
        for(int i = 0; i<nodes.length;i++){
            if (location.equals(nodes[i])) {
                return i;
            }
        };
        return -1;
    }

    private static String getParent() {
        int index = getIndex();
        int parentIndex = index;
        String[] nodes = Main.propertiesManager.getArray("nodes");
        do
            parentIndex = IdGenerator.getRandomInt(0,nodes.length-1);
        while (parentIndex == index);
        return nodes[parentIndex];
    };

    public static void processMessage(Node node, String line) throws Exception{
        Message message = new Gson().fromJson(line, Message.class);
        if(message.getType()!=Message.Type.HANDSHAKE && !message.getTo().equals(Main.pid)){
            Log.LogEvent(Log.SUBTYPE.ROUTING, "Not for me :(");
            broadcast(message, node);
            return;
        }
        Log.LogEvent(Log.SUBTYPE.ROUTING, "Message:"+message.toString());
        switch(message.getType()){
            case HANDSHAKE:{
                node.setPid(message.getFrom());
                if(client.getServer().getPid()==null || !node.getPid().equals(client.getServer().getPid())){
                    nodes.put(node.getPid(),node);
                }
                if(message.getTo() == null){
                    BufferedWriter toNode = new BufferedWriter(new OutputStreamWriter(node.getSocket().getOutputStream()));
                    toNode.write(new Gson().toJson(new Message(Message.Type.HANDSHAKE, Main.pid, node.getPid(), null)));
                    toNode.flush();
                }else{
                    String[] allNodes = Main.propertiesManager.getArray("nodes");
                    if(allNodes.length>2 && message.getFrom().equals(client.getServer().getPid()) && nodes.containsKey(message.getFrom())){
                        Log.LogWarning(Log.SUBTYPE.ROUTING, "Loop detected");
                        nodes.get(message.getFrom()).getSocket().close();
                        node.getSocket().close();
                    }
                }
                return;
            }

            default: return;
        }

    }

    public static void broadcast(Message message, Node excluded){

    }
}
