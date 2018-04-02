import org.apache.commons.math3.linear.RealMatrix;

import java.net.*;
import java.io.*;
import java.util.HashMap;


public class WorkerHandler extends Thread implements Runnable{
    private Socket connection;
    private MasterclassNEW server;
    int id = 0;
    ServerSocket serverSocket = null;
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean shouldRun = true;
    HashMap<Object,RealMatrix> resultsX = new HashMap<>();
    HashMap<Object,RealMatrix> resultsY = new HashMap<>();

    public WorkerHandler(Socket connection, MasterclassNEW server,int id) {
        try {
            in = new ObjectInputStream(connection.getInputStream());
            out = new ObjectOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            return in.readObject();
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
            name = (String) in.readObject();
            System.out.println("Hi this is worker: " + name + " and i am sending you my results!!");
            X = (RealMatrix) in.readObject();
            Y = (RealMatrix) in.readObject();
            resultsX.put(name,X);
            resultsY.put(name,Y);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Object, RealMatrix> getResultsY() {
        return resultsY;
    }

    public HashMap<Object, RealMatrix> getResultsX() {
        return resultsX;
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
        while(shouldRun){
        }
    }

}