import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class AndroidUser extends Thread implements AndroidClient, Runnable {
    String username = "";
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    Socket requestSocket = null;
    String clientNo = "";
    Thread t1 = null;
    Thread t2 = null;
    public void initializeAndroidClient(){
        try {
            requestSocket = new Socket("localhost", 10001);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject("user");
            out.flush();

            System.out.println("Please enter a username : ");
            Scanner scanner = new Scanner(System.in);
            username = scanner.next();
            out.writeObject(username);
            out.flush();
            t1 = new Thread(()->{
                try {
                    in = new ObjectInputStream(requestSocket.getInputStream());
                    Object message = in.readObject();
                    System.out.println(message);
                    StringTokenizer tokenizer = new StringTokenizer((String)message,"_ ");
                    tokenizer.nextToken();
                    clientNo = tokenizer.nextToken();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t1.run();
            menu();
        }  catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                System.out.println("LOGGING OUT");
                out.writeObject("LOGOUT USER " + clientNo);
                out.flush();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void sendNumberOfPois(Integer numOfPois){
        try {

            out.writeObject(numOfPois.toString());
            out.flush();
            Object res = in.readObject();
            HashMap<Integer, Double> results = (HashMap<Integer, Double>)res;
            Iterator it = results.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry me2 = (Map.Entry)it.next();
                System.out.print(me2.getKey() + ": ");
                System.out.println(me2.getValue());
            }

        }  catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }

    }

    public void menu(){

        while (true){
            System.out.println("Please select from the options below :");
            System.out.println("1) Give me the 5 top POIs");
            System.out.println("2) Give me the 10 top POIs");
            System.out.println("3) Give me the 15 top POIs");
            System.out.println("4) Exit");
            Scanner scanner = new Scanner(System.in);
            String inp = scanner.next();
            int selection = -1;
            while(true) {
                try {
                    selection = Integer.parseInt(inp.trim());
                    break;
                } catch (Exception e) {
                    System.out.println("Enter a valid number :");
                    menu();
                    inp = scanner.next();
                }
            }
            switch (selection){
                case 4:
                    System.exit(0);
                case 1:
                   sendNumberOfPois(5);
                   return;
                case 2:
                    sendNumberOfPois(10);
                    return;
                case 3:
                    sendNumberOfPois(15);
                    return;
                    default:
                        System.out.println("Please enter a valid selection");
            }
        }

    }


    public static void main(String args[]){
        new AndroidUser().initializeAndroidClient();
    }
}
