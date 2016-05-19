package pl.edu.elka.models;

import pl.edu.elka.raj.Main;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by carol on 15/04/2016.
 */
public class Message implements Comparable<Message> {

    public int compareTo(Message o) {
        if(o.getTimestamp().after(timestamp)){
            return -1;
        }else if(o.getTimestamp().before(timestamp)){
            return 1;
        }else{
            return 0;
        }
    }

    public enum Type{REQUEST, REPLY, HANDSHAKE, ACKHANDSHAKE, ELECTION, ANSWER, COORDINATOR, WANTED, ACCEPT};
    private Type type;
    private Date timestamp;
    private String from;
    private String to;
    private String info;
    private int ttl;

    @Override
    public String toString() {
        return "Type: " +type.toString() +". Timestamp: "+timestamp.toString() +". To: "+to+". From: "+from;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + from.hashCode();
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        Message another = (Message)o;
        return o != null && to.equals(another.getTo()) && from.equals(another.getFrom()) && timestamp.equals(another.getTimestamp()) && type == another.getType();
    }

    public Message(Type type, String from, String to, String info){
        this.type = type;
        this.from = from;
        this.to = to;
        this.info = info;
        this.timestamp = new Date();
        this.ttl = Main.nodes.length;

    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

}
