package Project;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterClass implements Master {
    ServerSocket providerSocket;
    Socket connection = null;

    public void initialize() {
        try {
            /* Create Server Socket */
            providerSocket = new ServerSocket(10001, 10);//mexri 10 exyphretei mpainei sto initial buffer
            while (true) {
                /* Accept the connection */
                connection = providerSocket.accept();
                System.out.println("Got a new connection...");
                /* Handle the request */
                Thread t = new ActionsForClients(connection);
                t.start();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static void main(String args[]){
        new MasterClass().initialize();
    }
}
