package pl.edu.elka.raj;

import pl.edu.elka.models.Node;
import pl.edu.elka.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Represents a client connected to the TCP server and manages its lifecycle
 *
 * @author Gregorio
 *
 */
public class TCPClientWorker implements Runnable
{
    private Node client;

    public TCPClientWorker(Node client)
    {
        this.client = client;
    }

    /*
     * Implementation of the run method that handles client communication and
     * connection/disconnection.
     *
     * @see java.lang.Runnable#run()
     */

    public void run()
    {
        while (!client.getSocket().isClosed())
        {
            try
            {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
                        client.getSocket().getInputStream()));
                String data = inFromClient.readLine();
                if (data == null) {
                    return;
                }
                NetworkController.processMessage(client, data);
            }
            catch (Exception e)
            {
                try
                {
                    client.getSocket().close();
                }
                catch (Exception ex)
                {
                }
                Log.LogError(Log.SUBTYPE.SYSTEM, "Client disconnected");
            }
        }

        NetworkController.clients.remove(client.getPid());
    }

}
