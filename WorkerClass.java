package Project;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class WorkerClass extends Thread implements Worker  {
    private int availableProcessors;
    private long availableMemory;

    public WorkerClass(int availableProcessors,long availableMemory){
        this.availableProcessors = availableProcessors;
        this.availableMemory = availableMemory;
    }

    public WorkerClass(){}

    public void initialize(){
        new WorkerClass(Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().freeMemory()).start();
    }

    public void run(){
        Socket requestSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try{
            requestSocket = new Socket("BAZOUME THN IP TOU SERVER",10001);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            while(true){
                Scanner sc = new Scanner(System.in);
                System.out.println("Write your message: ");
                String message = sc.nextLine();
                out.writeObject(message);
                out.flush();
                System.out.println("Server>" + in.readObject());
            }
        }catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public int getAvailableProcessors(){
        return this.availableProcessors;
    }

    public long getAvailableMemory(){
        return this.availableMemory;
    }

    public static void main(String args[]){
        new WorkerClass().initialize();
    }
}
