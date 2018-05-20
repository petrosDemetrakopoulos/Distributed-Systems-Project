

import java.net.*;
import java.io.*;


public class ClientHandler extends Thread implements Runnable{
    private Socket connection;
    private Masterclass server;
    ServerSocket serverSocket = null;
    ObjectInputStream in;
    ObjectOutputStream out;

    public ClientHandler(Socket connection, Masterclass server, ObjectInputStream in, ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.connection = connection;
        this.server = server;

    }

    public void sendData(Object rd) throws IOException {
        this.out.writeObject(rd);
        this.out.flush();
    }

    public Object getData(){
        try {
            return this.in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        String status = "Client_";
        try{
            this.out.writeObject(status);
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.connection.setKeepAlive(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
