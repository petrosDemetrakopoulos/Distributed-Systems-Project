import org.apache.commons.math3.analysis.function.Inverse;
import org.apache.commons.math3.linear.*;

import java.io.*;
import java.net.*;

public class WorkerClass extends Thread implements Worker {
    private int availableProcessors;
    private long availableMemory;
    private static RealMatrix sparse_m = MatrixUtils.createRealMatrix(200, 200);
    private static RealMatrix Cmatrix = MatrixUtils.createRealMatrix(200, 200);
    private static RealMatrix X = MatrixUtils.createRealMatrix(200, 20);
    private static RealMatrix Y = MatrixUtils.createRealMatrix(200, 20);
    private RealMatrix Cu, Ci;
    private String status = "worker";

    public WorkerClass(int availableProcessors, long availableMemory) {
        this.availableProcessors = availableProcessors;
        this.availableMemory = availableMemory;
    }

    public WorkerClass() {
    }

    public void initialize() {
        new WorkerClass(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().freeMemory()).start();
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 200; j++) {
                if (i % 2 == 0) {
                    sparse_m.setEntry(i, j, 1);
                } else {
                    sparse_m.setEntry(i, j, 0);
                }
            }
        }

        calculateCMatrix(sparse_m);

        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 20; j++) {
                X.setEntry(i, j, sparse_m.getEntry(i, j));

            }
        }
        for (int j = 0; j < 200; j++) {
            for (int i = 0; i < 20; i++) {
                Y.setEntry(j, i, sparse_m.getEntry(j, i));
            }
        }
    }

    public void run(){
        Socket requestSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try{
            requestSocket = new Socket("localhost",10001);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject(status);
            out.flush();
            out.writeObject(availableProcessors);
            out.flush();
            out.writeObject(availableMemory);
            out.flush();
            RealMatrix P = (RealMatrix) in.readObject();
            RealMatrix C = (RealMatrix) in.readObject();


        }catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
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

    public void calculateCMatrix(RealMatrix realMatrix) {

        int a = 40;
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 200; j++) {
                Cmatrix.setEntry(i, j, 1 + a * sparse_m.getEntry(i, j));
                //System.out.println(Cmatrix.getEntry(i,j));
            }
        }
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
        double[] diag_elements = realMatrix.getRow(user);
        Cu = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    //diagwnios pinakas diastasewn mxm(osoi einai oi users) pou sthn diagwnio exei tis protimiseis olwn twn users gia to item i
    public void calculateCiMatrix(int item, RealMatrix realMatrix) {
        double[] diag_elements = realMatrix.getColumn(item);
        Ci = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    public RealMatrix calculate_x_u(int user, RealMatrix realMatrixY, RealMatrix realMatrixCu) {
        //TO x_u EINAI GIA KA8E XRHSTH u!!!
        double l = 0.01;
        RealMatrix Ytranspose = Y.transpose();
        RealMatrix product1 = Ytranspose.multiply(realMatrixCu);
        RealMatrix product2 = product1.multiply(Y);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(20);//ftiaxnei monadiaio pinaka
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = product2.add(regularization);
        RealMatrix Inverse = new QRDecomposition(inverseTerm).getSolver().getInverse();
        //System.out.println(Inverse.getRowDimension() + " " + Inverse.getColumnDimension());
        RealMatrix multiplication2 = Ytranspose.multiply(realMatrixCu);
        double[] pu_data = sparse_m.getRow(user);
        RealMatrix pu = MatrixUtils.createColumnRealMatrix(pu_data);//ftiaxnei to p(u)
        //System.out.println(pu.getRowDimension() + " " + pu.getColumnDimension());
        RealMatrix multiplication3 = multiplication2.multiply(pu);
        RealMatrix x_u = Inverse.multiply(multiplication3);

        return x_u;
    }


    public RealMatrix calculate_y_i(int item, RealMatrix realMatrixX, RealMatrix realMatrixCi) {
        double l = 0.01;
        RealMatrix Xtranspose = X.transpose();
        RealMatrix product1 = Xtranspose.multiply(realMatrixCi);
        RealMatrix product2 = product1.multiply(X);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(20);//ftiaxnei monadiaio pinaka
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = product2.add(regularization);
        RealMatrix Inverse = new QRDecomposition(inverseTerm).getSolver().getInverse();
        //System.out.println(Inverse.getRowDimension() + " " + Inverse.getColumnDimension());
        RealMatrix multiplication2 = Xtranspose.multiply(realMatrixCi);
        double[] pi_data = sparse_m.getColumn(item);
        RealMatrix pi = MatrixUtils.createColumnRealMatrix(pi_data);//ftiaxnei to p(u)
        //System.out.println(pu.getRowDimension() + " " + pu.getColumnDimension());
        RealMatrix multiplication3 = multiplication2.multiply(pi);
        RealMatrix y_i = Inverse.multiply(multiplication3);

        return y_i;
    }


    public double calculateError() {
        double ModelPrediction, RealPrediction, CmatrixPred, MeanSquaredError, Difference, Regularization;
        double Error = 0.0;
        double TotalError = 0.0;
        double l = 0.01;
        for (int user = 0; user < sparse_m.getRowDimension(); user++) {
            for (int item = 0; item < sparse_m.getColumnDimension(); item++) {
                ModelPrediction = X.getRowMatrix(user).multiply(Y.getRowMatrix(item).transpose()).getEntry(0, 0);
                RealPrediction = sparse_m.getEntry(user, item);
                CmatrixPred = Cmatrix.getEntry(user, item);
                Difference = RealPrediction - ModelPrediction;
                MeanSquaredError = CmatrixPred * Math.pow(Difference, 2);
                Error = Error + MeanSquaredError;
            }
        }
        Regularization = calculateRegularization();
        TotalError = TotalError + l * Regularization;
        return TotalError;
    }

    public double calculateRegularization() {
        double TotalNorm;
        double NormForUser = 0;
        double NormForItem = 0;
        for (int user = 0; user < sparse_m.getRowDimension(); user++) {
            NormForUser = NormForUser + Math.pow(X.getRowMatrix(user).getNorm(), 2);

        }

        for (int item = 0; item < sparse_m.getColumnDimension(); item++) {
            NormForItem = NormForItem + Math.pow(Y.getRowMatrix(item).getNorm(), 2);
        }

        TotalNorm = NormForUser + NormForItem;

        return TotalNorm;
    }

    public String getStatus() {
        return status;
    }


    public static void main(String args[]) {
        WorkerClass worker = new WorkerClass();
        worker.initialize();
        //for 10 sweeps
        for (int sweep = 0; sweep < 10; sweep++) {
            //System.out.println("Sweep number: " + sweep);
            //first we will compute all the user factors!!
            //RealMatrix YY = worker.preCalculateYY(Y).copy();//we compute Y^T * Y
            //for each user
            for (int user = 0; user < sparse_m.getRowDimension(); user++) {
                worker.calculateCuMatrix(user, Cmatrix);
                X.setRowMatrix(user, worker.calculate_x_u(user, Y, worker.getCu()).transpose());
            }
            //we will compute all the item factors!!
            //RealMatrix XX = worker.preCalculateXX(X).copy();//we compute X^T * X
            //for each item
            for (int item = 0; item < sparse_m.getColumnDimension(); item++) {
                worker.calculateCiMatrix(item, Cmatrix);
                Y.setRowMatrix(item, worker.calculate_y_i(item, X, worker.getCi()).transpose());
            }
            double error = worker.calculateError();
            System.out.println("Error : " + error);
            if (error <= 0.01) {
                System.out.println("Threshhold reached");
                break;
            }
        }

    }
}