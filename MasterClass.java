import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterClass implements Master {
    ServerSocket providerSocket;
    Socket connection = null;
    private RealMatrix R,P,C;
    int numberOfConnections=1;

    Map<Object,Object> sourcesCore = new HashMap<Object, Object>();
    Map<Object,Object> sourcesMemory = new HashMap<Object,Object>();
    ArrayList<Object> clients = new ArrayList<Object>();
    Vector<Object> finalRank = new Vector<Object>();

    public void initialize() {

        try {
            /* Create Server Socket */
            providerSocket = new ServerSocket(10001, 10);//mexri 10 exyphretei mpainei sto initial buffer
            while (true) {
                /* Accept the connection */
                connection = providerSocket.accept();
                System.out.println("Got a new connection...");
                /* Handle the request */
                requestHandler(connection);
                numberOfConnections++;

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void requestHandler(Socket connection) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
        while(true){
            Object numberOfCores = in.readObject();
            Object availiableMemory = in.readObject();
            Object gigamem = availiableMemory;
            double mem = ((Number) gigamem).doubleValue();
            System.out.println("Client "+numberOfConnections+" has number of cores: "+ numberOfCores+" and available memory(GB): "+ mem/(1024*1024*1024));
            Object name = "Client_"+numberOfConnections;
            clients.add(name);
            sourcesCore.put(name,numberOfCores);
            sourcesMemory.put(name,availiableMemory);
            Rank(sourcesCore,sourcesMemory);
        }
    }

    public void Rank(Map<Object, Object> sourceCPU, Map<Object, Object> sourceMEM){

        Map<Object, Object> map1 = sortByValues(sourceCPU);
        Map<Object, Object> map2 = sortByValues(sourceMEM);

        //CPU SORTING
        Set set1 = map1.entrySet();
        Iterator iterator1 = set1.iterator();
        while(iterator1.hasNext()) {
            Map.Entry cpu = (Map.Entry)iterator1.next();
            System.out.print(cpu.getKey() + ": ");
            System.out.println(cpu.getValue());
        }

        //MEMORY SORTING
        Set set2 = map2.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry memory = (Map.Entry)iterator2.next();
            System.out.print(memory.getKey() + ": ");
            System.out.println(memory.getValue());
        }

       // List<Object> indexes1 = new ArrayList<Object>(map1.keySet());
       // List<Object> indexes2 = new ArrayList<Object>(map2.keySet());

    }

    //overided comperator
    private static HashMap sortByValues(Map<Object, Object> map) {
        List list = new LinkedList(map.entrySet());
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
