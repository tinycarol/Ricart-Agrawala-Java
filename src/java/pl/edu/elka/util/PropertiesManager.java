package pl.edu.elka.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesManager
{
    private static Properties properties;
    private static InputStream input;

    public PropertiesManager()
    {
        properties = new Properties();

        try
        {

            input = new FileInputStream("./src/config/config.properties");
            properties.load(input);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getProperty(String key)
    {
        try
        {
            return properties.getProperty(key);
        }
        catch (Exception e)
        {
            Log.LogError(Log.SUBTYPE.PROPERTIES, "Error leyendo propiedad: " + e.getMessage());
            return null;
        }
    }

    public static String[] getArray(String key){
        try{
            return properties.getProperty(key).split(";");
        }catch (Exception e)
        {
            Log.LogError(Log.SUBTYPE.PROPERTIES, "Error leyendo propiedad: " + e.getMessage());
            return null;
        }

    }
}