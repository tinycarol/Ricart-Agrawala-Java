package pl.edu.elka.raj;

import pl.edu.elka.util.IdGenerator;
import pl.edu.elka.util.PropertiesManager;

/**
 * Created by carol on 15/04/2016.
 */
public class Main {
    public static PropertiesManager propertiesManager;
    public static NetworkController networkController;
    public static String pid;

    public static void main(String[] args){
        pid = IdGenerator.generate();
        propertiesManager = new PropertiesManager();
        networkController = new NetworkController();
        networkController.start();
    }

}
