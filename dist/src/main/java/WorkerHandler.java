import org.apache.commons.math3.linear.RealMatrix;

import java.net.*;
import java.io.*;


public class WorkerHandler extends Thread implements Runnable{

    private Socket connection;
    private MasterclassNEW server;
    int id = 0;
    ServerSocket serverSocket = null;
    ObjectInputStream in;
    ObjectOutputStream out;
    boolean shouldRun = true;

    public WorkerHandler(Socket connection, MasterclassNEW server,int id) {
        try {
            in = new ObjectInputStream(connection.getInputStream());
            out = new ObjectOutputStream(connection.getOutputStream());
            System.out.println("mphika");
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



    @Override
    public void run() {
        System.out.println("Accepted client: " + id);
        //try {


           while(shouldRun){
               String status = "Status_"+id;
                /*while(in.available()==0){
                    try {
                        Thread.sleep(1);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
            }
            //in.close();
            //out.close();
        /*} catch (IOException e) {
            e.printStackTrace();
            System.out.println("Connection probably lost...");
        }
*/
    }

}
