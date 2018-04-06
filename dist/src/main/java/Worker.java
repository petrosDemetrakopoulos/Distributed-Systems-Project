import org.apache.commons.math3.linear.RealMatrix;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Worker {
    void initialize();

    //void calculateCuMatrix(int i, RealMatrix realMatrix);

    //void calculateCiMatrix(int i, RealMatrix realMatrix);

    RealMatrix preCalculateYY(RealMatrix realMatrix);

    RealMatrix preCalculateXX(RealMatrix realMatrix);

    RealMatrix calculate_x_u(int i, RealMatrix realMatrix1, RealMatrix realMatrix2);

    RealMatrix calculate_y_i(int i, RealMatrix realMatrix1, RealMatrix realMatrix2);

    //void sendResultsToMasterForX(ObjectInputStream in, ObjectOutputStream out);

    //void sendResultsToMasterForY(ObjectInputStream in, ObjectOutputStream out);
}

