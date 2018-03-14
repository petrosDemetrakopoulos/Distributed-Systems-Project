import java.io.*;
import java.net.*;
 
public class Client extends Thread {
    int a, b;
    Client(int a, int b) {
        this.a = a;
        this.b = b;
    }
 
    public void run() {
 
        Socket requestSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
 
        try {
 
            /* Create socket for contacting the server on port 4321*/
			requestSocket = new Socket("localhost",4321);
			
           /* Create the streams to send and receive data from server */
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				in = new ObjectInputStream(requestSocket.getInputStream());
           
            /* Write the two integers */
                out.writeObject("The computer has " + Runtime.getRuntime().availableProcessors() + " cores");
				out.flush();
                out.writeObject("The computer has " + Runtime.getRuntime().freeMemory() + " bytes of memory");
                out.flush();
            /* Print the received result from server */
            System.out.println("Server>" + in.readObject());
 
            
 
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                in.close(); out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
   
    public static void main(String args[]) {
        new Client(10, 5).start();
        // new Client(20, 5).start();
        // new Client(30, 5).start();
        // new Client(40, 5).start();
        // new Client(50, 5).start();
        // new Client(60, 5).start();
        // new Client(70, 5).start();
        // new Client(80, 5).start();
        // new Client(90, 5).start();
        // new Client(100, 5).start();
    }
}