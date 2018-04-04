import org.apache.commons.math3.linear.RealMatrix;

import java.net.*;
import java.io.*;


public class WorkerHandler extends Thread implements Runnable{
    private Socket connection;
    private MasterclassNEW server;
    int id = 0;
    ObjectInputStream in;
    ObjectOutputStream out;

    public WorkerHandler(Socket connection, MasterclassNEW server,int id,ObjectInputStream in,ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.connection = connection;
        this.server = server;
        this.id = id;
    }

    public void sendData(RealMatrix matrix) throws IOException {
        this.out.writeObject(matrix);
        this.out.flush();
    }

    public void sendPayload(RealMatrix payload,int i,int j) throws IOException {
        this.out.writeObject(payload);
        this.out.flush();
        this.out.writeObject(i);
        this.out.flush();
        this.out.writeObject(j);
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

    public void readResults(){
        String name;
        RealMatrix X,Y;
        try{
            name = (String) this.in.readObject();
            System.out.println("Hi this is worker: " + name + " and i am sending you my results!!");
            X = (RealMatrix) this.in.readObject();
            Y = (RealMatrix) this.in.readObject();
            server.setResultsX(name,X);
            server.setResultsY(name,Y);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        System.out.println("Accepted client: " + id);
        String status = "Worker_"+id;
        try{
            this.out.writeObject(status);
            this.out.flush();
            System.out.println("Will wait for results!!!");
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