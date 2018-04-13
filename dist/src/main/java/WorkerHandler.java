import org.apache.commons.math3.linear.RealMatrix;

import java.net.*;
import java.io.*;


public class WorkerHandler extends Thread implements Runnable{
    private Socket connection;
    private MasterclassNEW server;
    int id;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public WorkerHandler(Socket connection, MasterclassNEW server,int id,ObjectInputStream in,ObjectOutputStream out) throws IOException {
        this.in = in;
        this.out = out;
        this.out.flush();
        this.connection = connection;
        this.server = server;
        this.id = id;
    }

    public void sendData(Object object) throws IOException {
        this.out.writeObject(object);
        this.out.flush();
    }

    public void sendPayload(RealMatrix payload,int i,int j) throws IOException {
        this.out.writeObject(payload);
        this.out.flush();
        this.out.writeInt(i);
        this.out.flush();
        this.out.writeInt(j);
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

    public void readResultsForY(){
        String name;
        RealMatrix Y;
        try{
            name = (String) this.in.readObject();
            System.out.println("Hi this is worker: " + name + " and i am sending you my results for Yslice!!");
            Y = (RealMatrix) this.in.readObject();
            server.setResultsY(name,Y);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readResultsForX(){
        String name;
        RealMatrix X;
        try{
            name = (String) this.in.readObject();
            System.out.println("Hi this is worker: " + name + " and i am sending you my results for Xslice!!");
            X = (RealMatrix) this.in.readObject();
            server.setResultsX(name,X);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    public void kill(int i){
        try {
            System.out.println("Closing communication with Worker_"+ i);
            this.out.close();
            this.in.close();
            this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sleep(WorkerHandler sc) throws InterruptedException {
        sc.sleep(1000);
    }

    @Override
    public void run() {
        System.out.println("Accepted worker: " + id);
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