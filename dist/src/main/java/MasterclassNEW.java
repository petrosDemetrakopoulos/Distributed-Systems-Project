import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.MathUtils;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;

//Master is for accepting connections

public class MasterclassNEW implements Master,Serializable {
    private RealMatrix dataset,P,C,X,Y;
    private ServerSocket socketprovider;
    private boolean shouldRun = true;
    private int connectionID =0;
    ArrayList<WorkerHandler> connections = new ArrayList<WorkerHandler>();
    HashMap<Object,Long> memoryRank = new HashMap<Object, Long>();
    private int MAX_WORKERS = 6;
    int k=100;
    boolean established = false;

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
        while(connectionID<MAX_WORKERS){
            try {

                Socket s =  socketprovider.accept();
                System.out.println("We have a new worker connection...");
                WorkerHandler sc = new WorkerHandler(s,this,connectionID++);
                sc.start();
                connections.add(sc);
                Object cores = sc.getData();
                Object memory = sc.getData();
                System.out.println("Cores :" + cores);
                System.out.println("Memory :" + memory);
                memoryRank.put(sc.id,(Long)memory);
                connections.get(connectionID-1).sendData(P);
                connections.get(connectionID-1).sendData(C);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //Ranking by memory
        memoryRank.entrySet().stream()
                .sorted(Map.Entry.<Object,Long>comparingByValue().reversed())
                .forEach(System.out::println);


        int loadperWorkerX = X.getRowDimension()/MAX_WORKERS;
        int loadperWorkerY = Y.getRowDimension()/MAX_WORKERS;
        int loadWorkerModX = X.getRowDimension()%MAX_WORKERS;
        int loadWorkerModY = Y.getRowDimension()%MAX_WORKERS;
        int startX = 0;
        int endX=loadperWorkerX;
        int startY = 0;
        int endY=loadperWorkerY;

        distributeXMatrixToWorkers(startX,endX,loadperWorkerX,loadWorkerModX);
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
        Random randomgen = new Random();

        for(int i=0; i<Y.getRowDimension(); i++){
            for(int j=0; j<k; j++) {

                Y.setEntry(i, j, randomgen.nextDouble());

            }
        }

    }

    public void distributeXMatrixToWorkers(int startX, int endX,int loadperWorkerX,int loadWorkerModX) {
        for (int i = 0; i < MAX_WORKERS; i++) {
            if (i != MAX_WORKERS) {
                RealMatrix sliceX = MatrixUtils.createRealMatrix(loadperWorkerX, k);

                for (int q = 0; q < sliceX.getRowDimension(); q++) {
                    for (int m = 0; m < sliceX.getColumnDimension(); m++) {
                        sliceX.setEntry(q, m, X.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendPayload(sliceX, startX, endX);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startX = endX + 1;
                endX += loadperWorkerX;
            }else{
                RealMatrix sliceX = MatrixUtils.createRealMatrix(loadWorkerModX, k);

                for (int q = 0; q < sliceX.getRowDimension(); q++) {
                    for (int m = 0; m < sliceX.getColumnDimension(); m++) {
                        sliceX.setEntry(q, m, X.getEntry(q, m));
                    }
                }
                try {
                    connections.get(i).sendPayload(sliceX, startX, endX);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void distributeYMatrixToWorkers(int startY, int endY,int loadperWorkerY,int loadWorkerModY ){
        for (int i = 0; i < MAX_WORKERS - 1; i++) {
            if (i != MAX_WORKERS) {
                RealMatrix sliceY = MatrixUtils.createRealMatrix(loadperWorkerY, k);

                for (int q = 0; q < sliceY.getRowDimension(); q++) {
                    for (int m = 0; m < sliceY.getColumnDimension(); m++) {
                        sliceY.setEntry(q, m, X.getEntry(q, m));
                    }
                }

                try {
                    connections.get(i).sendPayload(sliceY, startY, endY);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startY = endY + 1;
                endY += loadperWorkerY;
            }else{
                RealMatrix sliceY = MatrixUtils.createRealMatrix(loadWorkerModY, k);

                for (int q = 0; q < sliceY.getRowDimension(); q++) {
                    for (int m = 0; m < sliceY.getColumnDimension(); m++) {
                        sliceY.setEntry(q, m, X.getEntry(q, m));
                    }
                }

                try {
                    connections.get(i).sendPayload(sliceY, startY, endY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

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





