

import java.net.*;
import java.io.*;
import java.util.HashMap;


public class ClientHandler extends Thread implements Runnable{
    private Socket connection;
    private MasterclassNEW server;
    int id = 0;
    ServerSocket serverSocket = null;
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean shouldRun = true;



    public ClientHandler(Socket connection, MasterclassNEW server,int id, ObjectInputStream in,ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.connection = connection;
        this.server = server;
        this.id = id;
    }

    public void sendData(Object rd) throws IOException {
        this.out.writeObject(rd);
        this.out.flush();
    }


    public Object getData(){
        try {
            return this.in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public void run() {
        System.out.println("Accepted android client: " + id);
        String status = "Client_"+id;
        try{
            this.out.writeObject(status);
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(shouldRun){
        }
    }

}
