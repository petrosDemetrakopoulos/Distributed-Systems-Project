import java.io.*;
import java.net.*;
import java.util.*;
 
public class Server {
 
    public static void main(String args[]) {
        new Server().openServer();
    }
   
    /* Define the socket that receives requests */
	ServerSocket providerSocket;
	Socket connection = null;
	
 
    /* Define the socket that is used to handle the connection */
   
    void openServer() {
        try {
 
            /* Create Server Socket */
			providerSocket = new ServerSocket(4321,10);//mexri 10 exyphretei mpainei sto initial buffer 
			
            while (true) {
                /* Accept the connection */
				connection = providerSocket.accept();
                System.out.println("Got a new connection...");



                
                /* Handle the request */

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
}