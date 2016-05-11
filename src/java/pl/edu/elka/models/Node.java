package pl.edu.elka.models;

import pl.edu.elka.util.Log;

import java.net.Socket;

/**
 * Created by carol on 20/04/2016.
 */
public class Node {
    private String pid;
    private Socket socket;

    public Node(String pid, Socket socket, boolean isServer){
        this.pid = pid;
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getPid() {

        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
    
    public void close(){
        try {
            socket.getInputStream().close();
            socket.getOutputStream().close();
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        } catch (Exception e){
            Log.LogError(Log.SUBTYPE.SYSTEM, "Problem closing socket. Message: "+e.getMessage());
        }
    }
}
