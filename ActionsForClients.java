import java.io.*;
import java.net.*;
 
public class ActionsForClients extends Thread/**/ {
ObjectInputStream in;
ObjectOutputStream out;
 
    public ActionsForClients(Socket connection) {
        try {
                /*
                *
                *
                *
                */
				out = new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
                while(true){
                Object message = in.readObject();
                System.out.println("Message received:" + message);
            }

 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    public void run() {
        try {
                /*
                *
                *
                *
                */
			//	Object a = in.readObject();
			//	Object b = in.readObject();
 
				out.writeObject("OK");
				out.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}