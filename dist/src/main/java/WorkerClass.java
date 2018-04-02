import org.apache.commons.math3.analysis.function.Inverse;
import org.apache.commons.math3.linear.*;

import java.io.*;
import java.net.*;

public class WorkerClass implements Worker {
    private int availableProcessors;
    private long availableMemory;
    private static RealMatrix P;
    private static RealMatrix Cmatrix;
    private RealMatrix X;
    private RealMatrix Y;
    private RealMatrix Cu, Ci;
    private static String status;
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
            in = new ObjectInputStream(requestSocket.getInputStream());
            //out.writeObject(status);
            //out.flush();
            out.writeObject(availableProcessors);
            out.flush();
            out.writeObject(availableMemory);
            out.flush();
            status = (String)in.readObject();
            System.out.println("Worker status: " + status);
            P = (RealMatrix) in.readObject();
            Cmatrix = (RealMatrix) in.readObject();
            System.out.println("Waiting for work");
            X = (RealMatrix) in.readObject();
            Xstart = (int) in.readObject();
            Xend = (int) in.readObject();
            Y = (RealMatrix) in.readObject();
            Ystart = (int) in.readObject();
            Yend = (int) in.readObject();
            System.out.println("Starting calculations of X and Y");
        }catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
        train(X,Y);
        System.out.println("Calculations done...sending results to master!!!");
        sendResultsToMaster(in,out);
    }

    public int getAvailableProcessors() {
        return this.availableProcessors;
    }

    public long getAvailableMemory() {
        return this.availableMemory;
    }

    public RealMatrix getCu() {
        return Cu;
    }

    public RealMatrix getCi() {
        return Ci;
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
    public void calculateCuMatrix(int user, RealMatrix realMatrix) {
        double[] diag_elements= new double[Y.getRowDimension()];
        for(int i=0; i < Y.getRowDimension(); i++){
            diag_elements[i] = realMatrix.getEntry(user,i);
        }
        Cu = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    //diagwnios pinakas diastasewn mxm(osoi einai oi users) pou sthn diagwnio exei tis protimiseis olwn twn users gia to item i
    public void calculateCiMatrix(int item, RealMatrix realMatrix) {
        //System.out.println("dimensions of realmatrix " + realMatrix.getRowDimension() + " " +realMatrix.getColumnDimension());
        double[] diag_elements= new double[X.getRowDimension()];
        for(int i=0; i < X.getRowDimension(); i++){
            diag_elements[i] = realMatrix.getEntry(i,item);
        }
        Ci = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    public RealMatrix calculate_x_u(int user, RealMatrix realMatrixY, RealMatrix realMatrixCu) {
        //TO x_u EINAI GIA KA8E XRHSTH u!!!
        double l = 0.01;
        double[] pu_data = new double[realMatrixY.getRowDimension()];
        RealMatrix Ytranspose = realMatrixY.transpose();
        RealMatrix product1 = Ytranspose.multiply(realMatrixCu);
        RealMatrix product2 = product1.multiply(realMatrixY);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixY.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = product2.add(regularization);
        RealMatrix Inverse = new QRDecomposition(inverseTerm).getSolver().getInverse();
        RealMatrix multiplication2 = Ytranspose.multiply(realMatrixCu);
        for(int i=0; i<realMatrixY.getRowDimension(); i++){
            pu_data[i] = P.getEntry(user,i);
        }
        RealMatrix pu = MatrixUtils.createColumnRealMatrix(pu_data);//ftiaxnei to p(u)
        RealMatrix multiplication3 = multiplication2.multiply(pu);
        RealMatrix x_u = Inverse.multiply(multiplication3);
        return x_u;
    }


    public RealMatrix calculate_y_i(int item, RealMatrix realMatrixX, RealMatrix realMatrixCi) {
        double l = 0.01;
        double[] pi_data = new double[realMatrixX.getRowDimension()];
        RealMatrix Xtranspose = realMatrixX.transpose();
        //System.out.println("X transpose dimensions: " + Xtranspose.getRowDimension() + " " + Xtranspose.getColumnDimension());
        //System.out.println("ci transpose dimensions: " + realMatrixCi.getRowDimension() + " " + realMatrixCi.getColumnDimension());
        RealMatrix product1 = Xtranspose.multiply(realMatrixCi);
        RealMatrix product2 = product1.multiply(realMatrixX);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixX.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = product2.add(regularization);
        RealMatrix Inverse = new QRDecomposition(inverseTerm).getSolver().getInverse();
        //System.out.println(Inverse.getRowDimension() + " " + Inverse.getColumnDimension());
        RealMatrix multiplication2 = Xtranspose.multiply(realMatrixCi);
        for(int i=0; i<realMatrixX.getRowDimension(); i++){
            pi_data[i] = P.getEntry(i,item);
        }
        RealMatrix pi = MatrixUtils.createColumnRealMatrix(pi_data);//ftiaxnei to p(u)
        //System.out.println(pu.getRowDimension() + " " + pu.getColumnDimension());
        RealMatrix multiplication3 = multiplication2.multiply(pi);
        RealMatrix y_i = Inverse.multiply(multiplication3);

        return y_i;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public void train(RealMatrix X,RealMatrix Y){
        for (int sweep = 0; sweep < 1; sweep++) {
            //first we will compute all the user factors!!
            //for each user
            int Xuser=0;
            for (int user = Xstart; user < Xend; user++) {
                calculateCuMatrix(user, Cmatrix);
                X.setRowMatrix(Xuser, calculate_x_u(user, Y, getCu()).transpose());
                Xuser++;
            }
            //we will compute all the item factors!!
            //for each item
            int poi = 0;
            for (int item = Ystart; item < Yend; item++) {
                calculateCiMatrix(item, Cmatrix);
                Y.setRowMatrix(poi, calculate_y_i(item, X, getCi()).transpose());
                poi++;
            }
        }
    }

    public void sendResultsToMaster(ObjectInputStream in,ObjectOutputStream out){
        try{
            boolean haveResults = true;
            out.writeObject(haveResults);
            out.flush();
            out.writeObject(status);
            out.flush();
            out.writeObject(X);
            out.flush();
            out.writeObject(Y);
        } catch (IOException e) {
            e.printStackTrace();
        }/*finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }*/
    }


    public static void main(String args[]) {
        new WorkerClass().initialize();
    }
}