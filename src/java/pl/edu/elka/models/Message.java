package pl.edu.elka.models;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by carol on 15/04/2016.
 */
public class Message {

    public enum Type{REQUEST, REPLY, HANDSHAKE, ACKHANDSHAKE};
    private Type type;
    private Date timestamp;
    private String from;
    private String to;
    private String info;

    @Override
    public String toString() {
        return "Type: " +type.toString() +". Timestamp: "+timestamp.toString() +". To: "+to+". From: "+from;
    }

    public Message(Type type, String from, String to, String info){
        this.type = type;
        this.from = from;
        this.to = to;
        this.info = info;
        this.timestamp = new Date();
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
