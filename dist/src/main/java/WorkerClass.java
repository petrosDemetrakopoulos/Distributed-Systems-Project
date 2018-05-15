import org.apache.commons.math3.analysis.function.Inverse;
import org.apache.commons.math3.linear.*;

import java.io.*;
import java.net.*;

public class WorkerClass implements Worker{
    private int availableProcessors;
    private long availableMemory;
    private static RealMatrix P;
    private static RealMatrix Cmatrix;
    private RealMatrix X;
    private RealMatrix Y;
    private static String status;
    private static String type = "worker";
    private int Xstart,Xend,Ystart,Yend;

    public WorkerClass(String status){
        this.status=status;
    }

    public WorkerClass(){}

    public void initialize() {
        Socket requestSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        availableProcessors = Runtime.getRuntime().availableProcessors();
        availableMemory = Runtime.getRuntime().freeMemory();
        try{
            requestSocket = new Socket("localhost",10001);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            out.writeObject(type);
            out.flush();
            out.writeObject(availableProcessors);
            out.flush();
            out.writeObject(availableMemory);
            out.flush();
            status = (String) in.readObject();
            System.out.println("Worker status: " + status);
            P = (RealMatrix) in.readObject();
            Cmatrix = (RealMatrix) in.readObject();

            //FOR Xslice
            System.out.println("Waiting for Xslice...");
            Y = (RealMatrix) in.readObject();
            RealMatrix Xslice = (RealMatrix) in.readObject();
            Xstart = in.readInt();
            Xend = in.readInt();
            System.out.println("Xstart : " + Xstart + " Xend : " + Xend);
            System.out.println("Starting calculations for Xslice...");
            trainX(Xslice,Y);
            System.out.println("Calculations done for Xslice...sending results to master!!!");
            sendResultsToMasterForX(in,out,Xslice);

            //FOR Yslice
            System.out.println("Waiting for Yslice...");
            X = (RealMatrix) in.readObject();
            RealMatrix Yslice = (RealMatrix) in.readObject();
            Ystart = in.readInt();
            Yend = in.readInt();
            System.out.println("Ystart : " + Ystart + " Yend : " + Yend);
            System.out.println("Starting calculations for Yslice...");
            trainY(Yslice,X);
            System.out.println("Calculations done for Yslice...sending results to master!!!");
            sendResultsToMasterForY(in,out,Yslice);
        }catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
        System.out.println("Waiting for new work... :(");
        int haveWork;
        try {
            while(true){
                haveWork = (int) in.readObject();
                if(haveWork==1){

                    //FOR Xslice
                    System.out.println("Waiting for Xslice...");
                    Y = (RealMatrix) in.readObject();
                    RealMatrix Xslice = (RealMatrix) in.readObject();
                    Xstart = in.readInt();
                    Xend = in.readInt();
                    System.out.println("Xstart : " + Xstart + " Xend : " + Xend);
                    System.out.println("Starting calculations for Xslice...");
                    trainX(Xslice,Y);
                    System.out.println("Calculations done for Xslice...sending results to master!!!");
                    sendResultsToMasterForX(in,out,Xslice);

                    //FOR Yslice
                    System.out.println("Waiting for Yslice...");
                    X = (RealMatrix) in.readObject();
                    RealMatrix Yslice = (RealMatrix) in.readObject();
                    Ystart = in.readInt();
                    Yend = in.readInt();
                    System.out.println("Ystart : " + Ystart + " Yend : " + Yend);
                    System.out.println("Starting calculations for Yslice...");
                    trainY(Yslice,X);
                    System.out.println("Calculations done for Yslice...sending results to master!!!");
                    sendResultsToMasterForY(in,out,Yslice);
                    System.out.println("Waiting for new work... :(");
                }else{
                    break;
                }
            }
            System.out.println("Training is done!!!");
        } catch (IOException | ClassCastException e) {
            System.out.println("Epochs are over...Training is done!!!");
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }finally {
            try {
                System.out.println("Closing communication...Bye!!");
                in.close();
                out.close();
                requestSocket.close();
            }catch(IOException e1){
                e1.printStackTrace();
            }
        }
    }

    public int getAvailableProcessors() {
        return this.availableProcessors;
    }

    public long getAvailableMemory() {
        return this.availableMemory;
    }

    public RealMatrix getP() {
        return P;
    }

    public RealMatrix getCmatrix() {
        return Cmatrix;
    }

    public RealMatrix getX() {
       return X;
    }

    public RealMatrix getY() {
       return Y;
    }

    public RealMatrix preCalculateXX(RealMatrix realMatrix) {
        RealMatrix Xtranspose = realMatrix.transpose();
        return Xtranspose.multiply(realMatrix);
    }

    public RealMatrix preCalculateYY(RealMatrix realMatrix) {
        RealMatrix Ytranspose = realMatrix.transpose();
        return Ytranspose.multiply(realMatrix);
    }

    //diagwnios pinakas diastasewn nxn (osa einai ta items) pou sthn diagwnio exei tis protimiseis tou xrhsth u
    public RealMatrix calculateCuMatrix(int user, RealMatrix realMatrix) {
        double[] diag_elements = realMatrix.getRow(user);
        RealMatrix Cu = MatrixUtils.createRealDiagonalMatrix(diag_elements);
        return Cu;
    }

    //diagwnios pinakas diastasewn mxm(osoi einai oi users) pou sthn diagwnio exei tis protimiseis olwn twn users gia to item i
    public RealMatrix calculateCiMatrix(int item, RealMatrix realMatrix) {
        double[] diag_elements= realMatrix.getColumn(item);
        RealMatrix Ci = MatrixUtils.createRealDiagonalMatrix(diag_elements);
        return Ci;
    }

    public RealMatrix calculate_x_u(int user, RealMatrix realMatrixY, RealMatrix YtY) {
        //TO x_u EINAI GIA KA8E XRHSTH u!!!
        double l = 0.1;
        RealMatrix realMatrixCu = calculateCuMatrix(user,Cmatrix);
        RealMatrix Ytranspose = realMatrixY.transpose();
        RealMatrix IdentityMatrix1 = MatrixUtils.createRealIdentityMatrix(realMatrixCu.getColumnDimension());
        RealMatrix subtract = realMatrixCu.subtract(IdentityMatrix1);
        RealMatrix product = Ytranspose.multiply(subtract);
        RealMatrix product1 = product.multiply(realMatrixY);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixY.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix addition1 = YtY.add(product1);
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = addition1.add(regularization);
        RealMatrix Inverse = new LUDecomposition(inverseTerm).getSolver().getInverse();
        RealMatrix multiplication2 = Inverse.multiply(Ytranspose);
        RealMatrix pu = MatrixUtils.createColumnRealMatrix(P.getRow(user));//ftiaxnei to p(u)
        RealMatrix multiplication3 = multiplication2.multiply(realMatrixCu);
        RealMatrix x_u = multiplication3.multiply(pu);
        return x_u;
    }


    public RealMatrix calculate_y_i(int item, RealMatrix realMatrixX, RealMatrix XtX) {
        double l = 0.1;
        RealMatrix realMatrixCi = calculateCiMatrix(item,Cmatrix);
        RealMatrix Xtranspose = realMatrixX.transpose();
        RealMatrix IdentityMatrix1 = MatrixUtils.createRealIdentityMatrix(realMatrixCi.getColumnDimension());
        RealMatrix subtract = realMatrixCi.subtract(IdentityMatrix1);
        RealMatrix product = Xtranspose.multiply(subtract);
        RealMatrix product1 = product.multiply(realMatrixX);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixX.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix addition1 = XtX.add(product1);
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = addition1.add(regularization);
        RealMatrix Inverse = new LUDecomposition(inverseTerm).getSolver().getInverse();
        RealMatrix multiplication2 = Inverse.multiply(Xtranspose);
        RealMatrix pi = MatrixUtils.createColumnRealMatrix(P.getColumn(item));//ftiaxnei to p(u)
        RealMatrix multiplication3 = multiplication2.multiply(realMatrixCi);
        RealMatrix y_i = multiplication3.multiply(pi);
        return y_i;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public void trainX(RealMatrix Xslice,RealMatrix Y){
        //first we will compute all the user factors!!
        //for each user
        RealMatrix YtY = preCalculateYY(Y);
        int Xuser=0;
        for (int user = Xstart; user < Xend; user++) {
            Xslice.setRowMatrix(Xuser, calculate_x_u(user, Y,YtY).transpose());
            Xuser++;
        }
    }

    public void trainY(RealMatrix Yslice,RealMatrix X){
        //we will compute all the item factors!!
        //for each item
        RealMatrix XtX = preCalculateXX(X);
        int poi = 0;
        for (int item = Ystart; item < Yend; item++) {
            Yslice.setRowMatrix(poi, calculate_y_i(item, X,XtX).transpose());
            poi++;
        }
    }

    public void sendResultsToMasterForX(ObjectInputStream in,ObjectOutputStream out,RealMatrix Xslice){
        try{
            Object haveResults = true;
            out.writeObject(haveResults);
            out.flush();
            out.writeObject(status);
            out.flush();
            out.writeObject(Xslice.copy());
            out.flush();
            System.out.println("Sending is done!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendResultsToMasterForY(ObjectInputStream in,ObjectOutputStream out,RealMatrix Yslice){
        try{
            Object haveResults = true;
            out.writeObject(haveResults);
            out.flush();
            out.writeObject(status);
            out.flush();
            out.writeObject(Yslice.copy());
            out.flush();
            System.out.println("Sending is done!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new WorkerClass().initialize();
    }
}