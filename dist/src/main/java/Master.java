import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

public interface Master {
    void initialize();

    void calculateCMatrix(RealMatrix realMatrix);

    void calculatePMatrix(RealMatrix realMatrix);

    void distributeXMatrixToWorkers(int startX, int endX,int loadperWorkerX,int loadWorkerModX);

    void distributeYMatrixToWorkers(int startY, int endY,int loadperWorkerY,int loadWorkerModY );

    double calculateError();

    double calculateScore(int i,int j);

    //List<Poi> calculateBestLocalPoisForUser(int i,double k,double l,int j);

}
