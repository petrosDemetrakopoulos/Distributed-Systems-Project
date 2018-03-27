import org.apache.commons.math3.analysis.function.Inverse;
import org.apache.commons.math3.linear.*;

import java.io.*;
import java.net.*;

public class WorkerClass extends Thread implements Worker  {
    private int availableProcessors;
    private long availableMemory;
    private static OpenMapRealMatrix sparse_m = new OpenMapRealMatrix(200,200);
    private static RealMatrix Cmatrix = MatrixUtils.createRealMatrix(200,200);
    private static RealMatrix X = MatrixUtils.createRealMatrix(200,20);
    private static RealMatrix Y = MatrixUtils.createRealMatrix(200,20);
    private RealMatrix Cu,Ci;

    public WorkerClass(int availableProcessors,long availableMemory){
        this.availableProcessors = availableProcessors;
        this.availableMemory = availableMemory;
    }

    public WorkerClass(){}

    public void initialize(){
        //new WorkerClass(Runtime.getRuntime().availableProcessors(),Runtime.getRuntime().freeMemory()).start();
        calculateCMatrix(sparse_m);

        for(int i=0; i < 200; i++){
            for(int j=0; j < 20; j++){
                X.setEntry(i,j,sparse_m.getEntry(i,j));
            }
        }


        for(int j=0; j < 200; j++){
            for(int i=0; i < 20; i++){
                Y.setEntry(j,i,sparse_m.getEntry(j,i));
            }
        }

    }

    /*public void run(){
        Socket requestSocket = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        try{
            requestSocket = new Socket("BAZOUME THN IP TOU SERVER",10001);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            while(true){
                Scanner sc = new Scanner(System.in);
                System.out.println("Write your message: ");
                String message = sc.nextLine();
                out.writeObject(message);
                out.flush();
                System.out.println("Server>" + in.readObject());
            }
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
    }*/

    public int getAvailableProcessors(){
        return this.availableProcessors;
    }

    public long getAvailableMemory(){
        return this.availableMemory;
    }

    public RealMatrix getCu() {
        return Cu;
    }

    public RealMatrix getCi() {
        return Ci;
    }

    public void calculateCMatrix(RealMatrix realMatrix){

        int a = 40;
        for(int i=0; i < 200; i++){
            for(int j=0; j < 200; j++){
                Cmatrix.setEntry(i,j, 1 + a* sparse_m.getEntry(i,j));
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
    public void calculateCuMatrix(int user,RealMatrix realMatrix){
        double[] diag_elements = realMatrix.getRow(user);
        Cu = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    //diagwnios pinakas diastasewn mxm(osoi einai oi users) pou sthn diagwnio exei tis protimiseis olwn twn users gia to item i
    public void calculateCiMatrix(int item,RealMatrix realMatrix){
        double[] diag_elements = realMatrix.getColumn(item);
        Ci = MatrixUtils.createRealDiagonalMatrix(diag_elements);
    }

    public RealMatrix calculate_x_u(int user, RealMatrix realMatrixYY, RealMatrix realMatrixCu){
        //TO x_u EINAI GIA KA8E XRHSTH u!!!
        double l = 0.01;
        RealMatrix Ytranspose = Y.transpose();
        RealMatrix product1 = Ytranspose.multiply(realMatrixCu);
        RealMatrix product2 = product1.multiply(Y);
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixCu.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);
        RealMatrix inverseTerm = product2.add(regularization);
        RealMatrix Inverse = new QRDecomposition(inverseTerm).getSolver().getInverse();
        RealMatrix multiplication2 = Ytranspose.multiply(realMatrixCu);
        double[] pu_data = sparse_m.getRow(user);
        RealMatrix pu = MatrixUtils.createColumnRealMatrix(pu_data);//ftiaxnei to p(i)
        RealMatrix multplication3 = multiplication2.multiply(pu);
        RealMatrix x_u = Inverse.multiply(multplication3);

        return x_u;
    }


    public RealMatrix calculate_y_i(int item, RealMatrix realMatrixXX, RealMatrix realMatrixCi){
        double l = 0.01;
        RealMatrix Xtranspose = X.transpose();
        RealMatrix IdentityMatrix = MatrixUtils.createRealIdentityMatrix(realMatrixCi.getColumnDimension());//ftiaxnei monadiaio pinaka
        RealMatrix subtract = realMatrixCi.subtract(IdentityMatrix);//afairesh C(i) - I
        RealMatrix regularization = IdentityMatrix.scalarMultiply(l);//stoixeio pros stoixeio me to l->( l * I)
        RealMatrix product1 = Xtranspose.multiply(subtract);//X^T * (C(i) - I)
        RealMatrix product2 = product1.multiply(X);//(X^T * (C(i) - I)) * X
        RealMatrix InverseTerm1 = realMatrixXX.add(product2);//X^T*X + (X^T * (C(i) - I)) * X
        RealMatrix InverseTerm = InverseTerm1.add(regularization);//X^T*X + (X^T * (C(i) - I)) * X +l*I
        RealMatrix Inverse = new QRDecomposition(InverseTerm).getSolver().getInverse();
        double[] pi_data = sparse_m.getColumn(item);
        RealMatrix pi = MatrixUtils.createColumnRealMatrix(pi_data);//ftiaxnei to p(i)
        RealMatrix product3 = Xtranspose.multiply(realMatrixCi);
        RealMatrix product4 = product3.multiply(pi);
        RealMatrix y_i = Inverse.multiply(product4);

        return y_i;
    }

    public static void main(String args[]){
        WorkerClass worker = new WorkerClass();
        worker.initialize();
        //for 10 sweeps
        for(int sweep = 0; sweep < 10; sweep++){
            //first we will compute all the user factors!!
            RealMatrix YY = worker.preCalculateYY(Y).copy();//we compute Y^T * Y
            //for each user
            for(int user = 0; user < sparse_m.getRowDimension(); user++){
                worker.calculateCuMatrix(user,Cmatrix);
                System.out.println(YY.getRowDimension());
                X.setRowMatrix(user,worker.calculate_x_u(user,YY,worker.getCu()));
            }
            //we will compute all the item factors!!
            RealMatrix XX = worker.preCalculateXX(X).copy();//we compute X^T * X
            //for each item
            for(int item = 0; item < sparse_m.getColumnDimension(); item++){
                worker.calculateCiMatrix(item,Cmatrix);
                Y.setColumnMatrix(item,worker.calculate_y_i(item,XX,worker.getCi()));
            }
        }
    }
}
