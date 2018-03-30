import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.MathUtils;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterClass extends Thread implements Master,Serializable {
    ObjectInputStream in;
    ObjectOutputStream out;
    ServerSocket providerSocket;
    Socket connection = null;
    private RealMatrix dataset,P,C,X,Y;
    int numberOfConnections=0;
    int workersNo=0;
    int clientsNo=0;
    ArrayList<Object> Workers = new ArrayList<Object>();//keep number and name of workers
    ArrayList<Object> Clients = new ArrayList<Object>();//keep number and name of clients connected
    Map<Object,Object> sourcesCore = new HashMap<Object, Object>();
    Map<Object,Object> sourcesMemory = new HashMap<Object,Object>();


    public void initialize() {
        String fileName = "src/main/java/input_matrix_no_zeros.csv";
        DatasetReader reader = new DatasetReader();
        dataset = reader.DatasetReader(fileName);
        C = MatrixUtils.createRealMatrix(dataset.getRowDimension(),dataset.getColumnDimension());
        P = MatrixUtils.createRealMatrix(dataset.getRowDimension(),dataset.getColumnDimension());
        calculateCMatrix(dataset);
        calculatePMatrix(dataset);
        createXY();

        try {
            /* Create Server Socket */
            providerSocket = new ServerSocket(10001, 10);//mexri 10 exyphretei mpainei sto initial buffer
            while (true) {

                /* Accept the connection */
                connection = providerSocket.accept();

                System.out.println("Got a new connection...");

                /* Handle the request */
                Thread t1 = new Thread(()->{
                    numberOfConnections++;
                    System.out.println("Connection number: " + numberOfConnections);
                    try {
                        requestHandler(connection);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
                t1.start();
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

    public void calculateCMatrix(RealMatrix realMatrix) {
        int a = 40;
        for(int i=0; i < C.getRowDimension(); i++){
            for(int j=0; j < C.getColumnDimension(); j++){
                C.setEntry(i,j, 1 + a* realMatrix.getEntry(i,j));
            }
        }
    }

    public void calculatePMatrix(RealMatrix realMatrix) {
        for(int i=0; i < P.getRowDimension(); i++){
            for(int j=0; j < P.getColumnDimension(); j++){
                if(realMatrix.getEntry(i,j) > 0){
                    P.setEntry(i,j,1);
                }else{
                    P.setEntry(i,j,0);
                }
            }
        }
    }

    public void createXY(){
        int k=100;
        X = MatrixUtils.createRealMatrix(dataset.getRowDimension(),k);
        Y = MatrixUtils.createRealMatrix(dataset.getColumnDimension(),k);
        Random randomgen = new Random();

        for(int i=0; i<X.getRowDimension(); i++){
            for(int j=0; j<k; j++) {
                if (Math.random() < 0.5) {
                    X.setEntry(i, j, 0);
                }else{
                    X.setEntry(i, j, 100 * randomgen.nextDouble());
                }
            }
        }

        for(int i=0; i<Y.getRowDimension(); i++){
            for(int j=0; j<k; j++) {
                if (Math.random() < 0.5) {
                    Y.setEntry(i, j, 0);
                }else{
                    Y.setEntry(i, j, 100 * randomgen.nextDouble());
                }
            }
        }


    }



    public void requestHandler(Socket connection) throws IOException, ClassNotFoundException {
        out = new ObjectOutputStream(connection.getOutputStream());
        in = new ObjectInputStream(connection.getInputStream());
        Object status = in.readObject();
        System.out.println("Connected status: " + status);
        if(status.equals("worker")){
            Object numberOfCores = in.readObject();
            Object availableMemory = in.readObject();
            //Passing pinakes C,P to workers
            out.writeObject(P);
            out.flush();
            out.writeObject(C);
            out.flush();
            workersNo++;
            Object gigamem = availableMemory;
            double mem = ((Number) gigamem).doubleValue();
            Object name = "Worker_"+workersNo;
            Workers.add(name);
            System.out.println(name + " has number of cores: " +numberOfCores+ " and available memory(GB): " + mem/(1024*1024*1024));
            sourcesCore.put(name,numberOfCores);
            sourcesMemory.put(name,availableMemory);

        }else if(status.equals("client")){
            clientsNo++;
            Object name = "Client_"+clientsNo;
            Clients.add(name);
            System.out.println(name + " connected as android client");
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
