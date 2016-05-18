package pl.edu.elka.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Gregorio
 * Utility class. Creates beautiful log lines with an unified format. Admits different log types and subtypes (type of event,
 * subsystem that generated the event). By default, normal events show up in the server console and get stored in the database,
 * but it also allows you to log hidden things that only get stored in the database and
 * are not shown in the server console. So evil.
 */
public class Log {

    public enum TYPE { ERROR, EVENT, WARNING }
    public enum SUBTYPE { ROUTING, SYSTEM, PROPERTIES, CLIENTSOCKET, ELECTION }

    public static void LogEvent(SUBTYPE subtype, String message)
    {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println("[" + timeStamp + "]" + "[" + TYPE.EVENT.toString() + "][" + subtype.toString() + "] " + message);
    }

    public static void LogWarning(SUBTYPE subtype, String message)
    {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println("[" + timeStamp + "]" + "[" + TYPE.WARNING.toString() + "][" + subtype.toString() + "] " + message);
    }

    public static void LogError(SUBTYPE subtype, String message)
    {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println("[" + timeStamp + "]" + "[" + TYPE.ERROR.toString() + "][" + subtype.toString() + "] " + message);
    }

}