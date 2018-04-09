import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.MathUtils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;

//Master is for accepting connections
public class MasterclassNEW implements Master {
    private RealMatrix dataset,P,C,X,Y;
    private ServerSocket socketprovider;
    private int connectionID = 0;
    private int clientConnectionID = 0;
    private ArrayList<WorkerHandler> connections = new ArrayList<WorkerHandler>();
    private ArrayList<ClientHandler> clientConnections = new ArrayList<ClientHandler>();
    private ArrayList<String> Clients = new ArrayList<String>();
    private HashMap<Object,Long> memoryRank = new HashMap<Object, Long>();
    private int MAX_WORKERS = 3;
    private int k = 100;
    private HashMap<Object,RealMatrix> resultsX = new HashMap<>();
    private HashMap<Object,RealMatrix> resultsY = new HashMap<>();
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public void initialize(){
        String fileName = "src/main/java/input_matrix_no_zeros.csv";
        DatasetReader reader = new DatasetReader();
        dataset = reader.DatasetReader(fileName);
        C = MatrixUtils.createRealMatrix(dataset.getRowDimension(),dataset.getColumnDimension());
        P = MatrixUtils.createRealMatrix(dataset.getRowDimension(),dataset.getColumnDimension());
        calculateCMatrix(dataset);
        calculatePMatrix(dataset);
        createXY();
        System.out.println("Server started...");
        try {
            socketprovider = new ServerSocket(10001,10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //while true runs waits for workers/clients to connect
        while(true){
            try {
                Socket s =  socketprovider.accept();
                in = new ObjectInputStream(s.getInputStream());
                out = new ObjectOutputStream(s.getOutputStream());
                Object type = in.readObject();
                if(type.equals("worker")){
                    System.out.println("We have a new worker connection...");
                    WorkerHandler sc = new WorkerHandler(s,this,connectionID++,in ,out);
                    sc.start();
                    connections.add(sc);
                    Object cores = sc.getData();
                    Object memory = sc.getData();
                    sc.sleep(sc);
                    System.out.println("Cores :" + cores);
                    System.out.println("Memory :" + memory);
                    memoryRank.put(sc.id,(Long)memory);
                    connections.get(connectionID-1).sendData(P.copy());
                    connections.get(connectionID-1).sendData(C.copy());
                    if(connectionID==MAX_WORKERS){
                        double error = 0.0;
                        double NewError,TotalError;
                        int haveWork;
                        boolean threshold = false;
                        for(int epoch=0; epoch<16; epoch++) {

                            //FOR MATRIX X
                            sendWorkX();
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                int finalI = i;
                                Thread t1 = new Thread(() -> {
                                    boolean haveResults = (boolean) connections.get(finalI).getData();
                                    if (haveResults) {
                                        connections.get(finalI).readResultsForX();
                                    }
                                });
                                t1.start();
                                t1.join();
                            }
                            System.out.println("Will reconstruct X and then distribute Y!!!");
                            int Xstart = 0;
                            String name;
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                name = "Worker_" + i;
                                RealMatrix tempX = resultsX.get(name);
                                for (int row = 0; row < tempX.getRowDimension(); row++) {
                                    X.setRowMatrix(Xstart, tempX.getRowMatrix(row));
                                    Xstart++;
                                }
                            }

                            //FOR MATRIX Y
                            sendWorkY();
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                int finalI = i;
                                Thread t1 = new Thread(() -> {
                                    boolean haveResults = (boolean) connections.get(finalI).getData();
                                    if (haveResults) {
                                        connections.get(finalI).readResultsForY();
                                    }
                                });
                                t1.start();
                                t1.join();
                            }
                            System.out.println("Will reconstruct Y and then run error calculations!!!");
                            int Ystart = 0;
                            String name1;
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                name1 = "Worker_" + i;
                                RealMatrix tempY = resultsY.get(name1);
                                for (int row = 0; row < tempY.getRowDimension(); row++) {
                                    Y.setRowMatrix(Ystart, tempY.getRowMatrix(row));
                                    Ystart++;
                                }
                            }

                            //RUN ERROR CALCULATION
                            NewError = calculateError();
                            TotalError = Math.abs(error - NewError);
                            System.out.println("The error is: " + TotalError);
                            if(TotalError > 0.01){
                                error = NewError;
                                haveWork = 1;
                                for (int i = 0; i < MAX_WORKERS; i++) {
                                    connections.get(i).sendData(haveWork);
                                }
                                System.out.println("We will continue training!!!");
                            }else{
                                threshold = true;
                                break;
                            }
                            System.out.println("Epoch is: " + epoch);
                        }
                        if(threshold){
                            System.out.println("We reached the threshhold...training will stop...");
                            haveWork = 0;
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                connections.get(i).sendData(haveWork);
                                connections.get(i).kill(i);
                            }
                            try {
                                in.close();
                                out.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }else{
                            System.out.println("Epochs are over...");
                            haveWork = 0;
                            for (int i = 0; i < MAX_WORKERS; i++) {
                                connections.get(i).sendData(haveWork);
                                connections.get(i).kill(i);
                            }
                            try {
                                in.close();
                                out.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }else if(type.equals("user")){
                    System.out.println("We have a new client connection...");
                    ClientHandler sc = new ClientHandler(s,this,clientConnectionID++, in, out);
                    int crnUserID = clientConnectionID;
                    System.out.println(crnUserID);
                    clientConnections.add(sc);
                    Object name = sc.getData();
                    sc.sendData("Welcome! " + (String)name);
                    Clients.add((String)name);
                    Object numOfPois = sc.getData();
                    String stringifiedNumOfPois = (String)numOfPois;
                    ArrayList<Double> scoresForPois = new ArrayList<>();
                    ArrayList<Integer> poisIds = new ArrayList<>();
                    HashMap<Integer, Double> hmap = new HashMap<Integer, Double>();
                    for(int i=0; i<Y.getRowDimension(); i++){ //for each poi
                        double crnRes = calculateScore(crnUserID, i);
                        hmap.put(i,crnRes);
                    }
                    Map<Integer, Double> map = sortByValues(hmap);
                    System.out.println("After Sorting:");
                    Set set2 = map.entrySet();
                    Iterator iterator2 = set2.iterator();
                    while(iterator2.hasNext()) {
                        Map.Entry me2 = (Map.Entry)iterator2.next();
                        System.out.print(me2.getKey() + ": ");
                        System.out.println(me2.getValue());
                    }
                    iterator2 = set2.iterator();
                    HashMap<Integer, Double> results = new HashMap<Integer, Double>();
                    Integer count = 0;
                    while(iterator2.hasNext()) {
                        if(count < Integer.parseInt(stringifiedNumOfPois)){
                            Map.Entry me2 = (Map.Entry)iterator2.next();
                            results.put((Integer)me2.getKey(),(Double)me2.getValue());
                            System.out.print(me2.getKey() + ": ");
                            System.out.println(me2.getValue());
                            count++;
                        } else {
                            break;
                        }
                    }
                    sc.sendData(results);
                }
            } catch (IOException | InterruptedException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }
    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    public void setResultsX(Object name,RealMatrix temp){
        resultsX.put(name,temp);
    }

    public void setResultsY(Object name,RealMatrix temp){
        resultsY.put(name,temp);
    }

    public void sendWorkX(){
        //Ranking by memory
        memoryRank.entrySet().stream()
                .sorted(Map.Entry.<Object,Long>comparingByValue().reversed())
                .forEach(System.out::println);

        int loadperWorkerX = X.getRowDimension()/MAX_WORKERS;
        int loadWorkerModX = X.getRowDimension()%MAX_WORKERS;
        int startX = 0;
        int endX=loadperWorkerX;
        distributeXMatrixToWorkers(startX,endX,loadperWorkerX,loadWorkerModX);
    }

    public void sendWorkY(){
        //Ranking by memory
        memoryRank.entrySet().stream()
                .sorted(Map.Entry.<Object,Long>comparingByValue().reversed())
                .forEach(System.out::println);

        int loadperWorkerY = Y.getRowDimension()/MAX_WORKERS;
        int loadWorkerModY = Y.getRowDimension()%MAX_WORKERS;
        int startY = 0;
        int endY=loadperWorkerY;
        distributeYMatrixToWorkers(startY,endY,loadperWorkerY,loadWorkerModY);
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
        X = MatrixUtils.createRealMatrix(dataset.getRowDimension(),k);
        Y = MatrixUtils.createRealMatrix(dataset.getColumnDimension(),k);
        Random random = new Random();

        for(int i=0; i<Y.getRowDimension(); i++){
            for(int j=0; j<k; j++) {
                Y.setEntry(i, j, random.nextDouble());
            }
        }
    }

    public void distributeXMatrixToWorkers(int startX, int endX,int loadperWorkerX,int loadWorkerModX) {
        for (int i = 0; i < MAX_WORKERS; i++) {
            if (i != MAX_WORKERS-1) {
                RealMatrix sliceX = MatrixUtils.createRealMatrix(loadperWorkerX, k);
                for (int q = 0; q < sliceX.getRowDimension(); q++) {
                    for (int m = 0; m < sliceX.getColumnDimension(); m++){
                        sliceX.setEntry(q, m, X.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendData(Y.copy());
                    connections.get(i).sendPayload(sliceX, startX, endX);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startX = endX;
                endX += loadperWorkerX;
            }else{
                RealMatrix sliceX = MatrixUtils.createRealMatrix(X.getRowDimension() - startX, k);
                for (int q = 0; q < sliceX.getRowDimension(); q++) {
                    for (int m = 0; m < sliceX.getColumnDimension(); m++) {
                        sliceX.setEntry(q, m, X.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendData(Y.copy());
                    connections.get(i).sendPayload(sliceX, startX, endX + loadWorkerModX);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void distributeYMatrixToWorkers(int startY, int endY,int loadperWorkerY,int loadWorkerModY ){
        for (int i = 0; i < MAX_WORKERS; i++) {
            if (i != MAX_WORKERS-1) {
                RealMatrix sliceY = MatrixUtils.createRealMatrix(loadperWorkerY, k);
                for (int q = 0; q < sliceY.getRowDimension(); q++) {
                    for (int m = 0; m < sliceY.getColumnDimension(); m++) {
                        sliceY.setEntry(q, m, Y.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendData(X.copy());
                    connections.get(i).sendPayload(sliceY, startY, endY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startY = endY;
                endY += loadperWorkerY;
            }else{
                RealMatrix sliceY = MatrixUtils.createRealMatrix(Y.getRowDimension()-startY, k);
                for (int q = 0; q < sliceY.getRowDimension(); q++) {
                    for (int m = 0; m < sliceY.getColumnDimension(); m++) {
                        sliceY.setEntry(q, m, Y.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendData(X.copy());
                    connections.get(i).sendPayload(sliceY, startY, endY + loadWorkerModY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public double calculateError() {
        double ModelPrediction, RealPrediction, CmatrixPred, MeanSquaredError, Difference, Regularization;
        double Error = 0.0;
        double TotalError;
        double l = 0.01;
        double temp;
        for (int user = 0; user < P.getRowDimension(); user++) {
            for (int item = 0; item < P.getColumnDimension(); item++) {
                ModelPrediction = X.getRowMatrix(user).multiply(Y.getRowMatrix(item).transpose()).getEntry(0, 0);
                RealPrediction = P.getEntry(user, item);
                CmatrixPred = C.getEntry(user, item);
                Difference = RealPrediction - ModelPrediction;
                temp = Math.pow(Difference, 2);
                MeanSquaredError = CmatrixPred * temp;
                Error = Error + MeanSquaredError;
            }
        }
        Regularization = calculateRegularization();
        TotalError = Error + l * Regularization;
        return TotalError;
    }

    public double calculateScore(int user, int poi) {
        RealMatrix y_i = Y.getRowMatrix(poi);
        RealMatrix x_u = X.getRowMatrix(user);
        double p_u_i = x_u.multiply(y_i.transpose()).getEntry(0,0);
        return p_u_i;
    }

    public double calculateRegularization() {
        double TotalNorm;
        double NormForUser = 0;
        double NormForItem = 0;
        for (int user = 0; user < P.getRowDimension(); user++) {
            NormForUser = NormForUser + Math.pow(X.getRowMatrix(user).getNorm(), 2);

        }
        for (int item = 0; item < P.getColumnDimension(); item++) {
            NormForItem = NormForItem + Math.pow(Y.getRowMatrix(item).getNorm(), 2);
        }
        TotalNorm = NormForUser + NormForItem;
        return TotalNorm;
    }

    ArrayList<WorkerHandler> getConnections(){
        return connections;
    }

    ArrayList<WorkerHandler> setConnections( ArrayList<WorkerHandler> connections){
        return this.connections = connections;
    }

    public static void main(String args[]){
        new MasterclassNEW().initialize();
    }
}