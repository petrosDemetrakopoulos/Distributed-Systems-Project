package Project;

import org.apache.commons.math3.linear.RealMatrix;

public interface Worker {
    public void initialize();

    public void calculateCMatrix(int i, RealMatrix realMatrix);

    public void calculateCuMatrix(int i, RealMatrix realMatrix);

    public void calculateCiMatrix(int i, RealMatrix realMatrix);

    public RealMatrix preCalculateYY(RealMatrix realMatrix);

    public RealMatrix preCalculateXX(RealMatrix realMatrix);

    public RealMatrix calculate_x_u(int i, RealMatrix realMatrix1, RealMatrix realMatrix2);

    public RealMatrix calculate_y_i(int i, RealMatrix realMatrix1, RealMatrix realMatrix2);

    public void sendResultsToMaster();

}
