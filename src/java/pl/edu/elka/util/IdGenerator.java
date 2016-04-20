package pl.edu.elka.util;

/**
 * Created by carol on 20/04/2016.
 */
public class IdGenerator {

    public static String generate(){
        String pickFrom = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789=-+#%&";
        String id = "";
        for (int i = 0; i < 8; i++) {
            id += pickFrom.charAt(getRandomInt(0,59));
        }
        return id;

    }

    public static int getRandomInt(int min, int max){
        return (int)Math.floor(Math.random()*(max-min+1))+min;
    }
}
