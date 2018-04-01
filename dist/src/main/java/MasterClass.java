import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterClass extends Thread implements Master {
    ObjectInputStream in;
    ObjectOutputStream out;
    ServerSocket providerSocket;
    Socket connection = null;
    private RealMatrix R,P,C;
    int numberOfConnections=0;
    int workersNo=0;
    int clientsNo=0;
    Object name;
    ArrayList<Object> Workers = new ArrayList<Object>();//keep number and name of workers
    ArrayList<Object> Clients = new ArrayList<Object>();//keep number and name of clients connected
    Map<Object,Object> sourcesCore = new HashMap<Object, Object>();
    Map<Object,Object> sourcesMemory = new HashMap<Object,Object>();


    public void initialize() {
        System.out.println("Waiting for connections!\n");

        try {
            /* Create Server Socket */
            providerSocket = new ServerSocket(4321, 10);//mexri 10 exyphretei mpainei sto initial buffer
            while (true) {

                /* Accept the connection */
                connection = providerSocket.accept();

                /* Handle the request */
                Thread t1 = new Thread(()->{
                    System.out.println("Got a new connection...");

                    numberOfConnections++;
                    System.out.println("Connection number: " + numberOfConnections);
                    try {
                        requestHandler(connection);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                t1.run();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        Rank(sourcesCore);
        Rank(sourcesMemory);
    }


    public void requestHandler(Socket connection) throws IOException, ClassNotFoundException {
        out = new ObjectOutputStream(connection.getOutputStream());
        in = new ObjectInputStream(connection.getInputStream());
        Object status = in.readObject();

        System.out.println("Connected status: " + status);
        if(status.equals("worker")){
            Object numberOfCores = in.readObject();
            Object availableMemory = in.readObject();
            workersNo++;
            Object gigamem = availableMemory;
            double mem = ((Number) gigamem).doubleValue();
            name = "Worker_"+workersNo;
            Workers.add(name);
            System.out.println(name + " has " +numberOfCores+ " cores and available memory(GB): " + mem/(1024*1024*1024));
            sourcesCore.put(name,numberOfCores);
            sourcesMemory.put(name,availableMemory);
        }else if(status.equals("user")){
            clientsNo++;
            name = "Client_"+clientsNo;
            Clients.add(name);
            System.out.println(name + " connected as android client");
            Object clientName = in.readObject();
            out.writeObject("Welcome! " + clientName);
            out.flush();
        }
     //   Object mer = in.readObject();
      //  System.out.println("++" + (String)mer + "++");

        if(status.equals("user")) {
            //** reading user query **//
            Object menuOption = in.readObject();
            System.out.println("***" + (int) menuOption + "***");
            if ((int) menuOption < 4) {
                Object username = in.readObject();
                Object queryOfPois = in.readObject();
                System.out.println("***" + "USER : " + (String)username + ", wants " + (int)queryOfPois + " Pois " + "***");
            }
            //** end of user query **//

            //** logging out a user **//
            Object loggingOUTmsg = in.readObject();
            StringTokenizer st = new StringTokenizer((String) loggingOUTmsg);
            String first = st.nextToken();
            while (!first.equals("LOGOUT")) {
                System.out.println(first);
                loggingOUTmsg = in.readObject();
                st = new StringTokenizer((String) loggingOUTmsg);
                first = st.nextToken();
            }
            String kind = st.nextToken();
            if (kind.equals("USER")) {
                String un = st.nextToken();
                Clients.remove(un);
                clientsNo--;
                numberOfConnections--;
                System.out.println("\nUser :  " + name + " just logged out!\n");
            }
            //** end of logging out a user **//
        }

    }

    public void Rank(Map<Object, Object> source){

        Map<Object, Object> map = sortByValues(source);

        Set set2 = map.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry)iterator2.next();
            System.out.print(me2.getKey() + ": ");
            System.out.println(me2.getValue());
        }
    }

    private static HashMap sortByValues(Map<Object, Object> map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        //Copying the sorted list
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }



    public static void main(String args[]){
        new MasterClass().initialize();
    }
}
