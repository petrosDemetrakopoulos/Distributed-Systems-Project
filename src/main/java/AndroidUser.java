import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class AndroidUser extends Thread implements AndroidClient, Runnable {
    String username = "";
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    Socket requestSocket = null;
    String clientNo = "";
    Thread t1 = null;
    Thread t2 = null;
    public void initializeAndroidClient(){
        /* Create socket for contacting the server on port 4321*/
        try {
            requestSocket = new Socket("localhost", 4321);

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
                    StringTokenizer tokenizer = new StringTokenizer((String)message);
                    tokenizer.nextToken();
                    clientNo = tokenizer.nextToken();
                    System.out.println(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t1.run();
          //  listenFromServer(requestSocket);
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
               // requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void sendNumberOfPois(Integer numOfPois){
        try {
          //  out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject(username);
            out.flush();
            out.writeObject(numOfPois);
            out.flush();
        }  catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
// finally {
//            try {
//                out.close();
//                requestSocket.close();
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        }
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
                 //  sendNumberOfPois(5);
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

  //  public void listenFromServer(Socket connection) {

   // }
    public static void main(String args[]){
        new AndroidUser().initializeAndroidClient();
    }
}
