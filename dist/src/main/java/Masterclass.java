import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.*;
import java.util.*;

//Master is for accepting connections
public class Masterclass implements Master {
    private RealMatrix dataset,P,C,X,Y;
    private ServerSocket socketprovider;
    private int connectionID = 0;
    private int clientConnectionID = 0;
    private ArrayList<WorkerHandler> connections = new ArrayList<WorkerHandler>();
    private ArrayList<ClientHandler> clientConnections = new ArrayList<ClientHandler>();
    private ArrayList<String> Clients = new ArrayList<String>();
    private HashMap<Object,Long> memoryRank = new HashMap<Object, Long>();
    private int MAX_WORKERS = 6;
    private int k = 20;
    private HashMap<Object,RealMatrix> resultsX = new HashMap<>();
    private HashMap<Object,RealMatrix> resultsY = new HashMap<>();
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static JsonObject mainJsonObject;
    private static JsonReader json;

    public void initialize(){
        String fileName = "src/main/java/input_matrix_no_zeros.csv";
        String jsonPois = "src/main/java/POIs.json";
        JsonPoiParser poisParser = null;
        try {
            poisParser = new JsonPoiParser(getJsonObjectFromPath(jsonPois).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                out.flush();
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
                        for(int epoch=0; epoch<1; epoch++) {

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
                            System.out.println("The cost function is: " + NewError);
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
                    ClientHandler sc = new ClientHandler(s,this,764, in, out);
             //       int crnUserID = 764;
                 //   System.out.println(crnUserID);
                    clientConnections.add(sc);
                    Object name = sc.getData();
                    String temp = (String) name;
                    int userNumber = Integer.parseInt(temp);
                   // int us = Integer.parseInt(((int)name));
                    sc.sendData("Welcome! " + (String)name);
                    Clients.add((String)name);
                    Object numOfPois = sc.getData();
                    String stringifiedNumOfPois = (String)numOfPois;
                    HashMap<Integer, Double> hmap = new HashMap<>();
                    for(int i=0; i<Y.getRowDimension(); i++){ //for each poi
                        double crnRes = calculateScore(userNumber, i);
                        hmap.put(i,crnRes);
                    }
                    HashMap map = sortByValues(hmap);
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

                    HashMap<Integer,Poi> finalPois = matchingPois(results,poisParser);
             //       sc.sendData(results);
                    sc.sendData(finalPois);
                }
            } catch (IOException | InterruptedException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }

        private static HashMap<Integer,Poi> matchingPois(HashMap<Integer,Double> resultsMap,JsonPoiParser poisParser){
            HashMap<Integer,Poi> finalPois = new HashMap<>();
            for(int keyVaule : resultsMap.keySet())
            {
         //       System.out.println(keyVaule);
                for(int tempKey : poisParser.getPoisMap().keySet()){
                    if(keyVaule == tempKey){
                        finalPois.put(keyVaule,poisParser.getSpecificPoi(keyVaule));
                    }
                }
            }

  /*          for(int tt : finalPois.keySet())
                System.out.println("\n  new " + tt );*/

            return  finalPois;
        }


    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, (Comparator) (o1, o2) -> ((Comparable) ((Map.Entry) (o2)).getValue())
                .compareTo(((Map.Entry) (o1)).getValue()));

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
                .sorted(Map.Entry.<Object,Long>comparingByValue().reversed());

        int loadperWorkerX = X.getRowDimension()/MAX_WORKERS;
        int loadWorkerModX = X.getRowDimension()%MAX_WORKERS;
        int startX = 0;
        int endX=loadperWorkerX;
        distributeXMatrixToWorkers(startX,endX,loadperWorkerX,loadWorkerModX);
    }

    public void sendWorkY(){

        //Ranking by memory
        memoryRank.entrySet().stream()
                .sorted(Map.Entry.<Object,Long>comparingByValue().reversed());

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
        JDKRandomGenerator random = new JDKRandomGenerator();
        random.setSeed(1);

        for(int i=0; i<X.getRowDimension(); i++){
            for(int j=0; j<X.getColumnDimension(); j++){
                this.X.setEntry(i,j,random.nextDouble());
            }
        }

        for(int i=0; i<Y.getRowDimension(); i++){
            for(int j=0; j<Y.getColumnDimension(); j++) {
                this.Y.setEntry(i, j, random.nextDouble());
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
        double l = 0.1;
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
            NormForUser = NormForUser + Math.pow(X.getRowMatrix(user).getFrobeniusNorm(), 2);

        }
        for (int item = 0; item < P.getColumnDimension(); item++) {
            NormForItem = NormForItem + Math.pow(Y.getRowMatrix(item).getFrobeniusNorm(), 2);
        }
        TotalNorm = NormForUser + NormForItem;
        return TotalNorm;
    }

    public static JsonObject getJsonObjectFromPath(String jsonPath){
        try {
            json = Json.createReader(new FileReader(jsonPath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mainJsonObject = json.readObject();
        json.close();


        return mainJsonObject;

    }

    ArrayList<WorkerHandler> getConnections(){
        return connections;
    }

    ArrayList<WorkerHandler> setConnections( ArrayList<WorkerHandler> connections){
        return this.connections = connections;
    }

    public static void main(String args[]){
        new Masterclass().initialize();
    }
}