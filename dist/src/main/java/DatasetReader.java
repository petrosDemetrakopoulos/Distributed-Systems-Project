import com.opencsv.CSVReader;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;

public class DatasetReader {

    public static RealMatrix DatasetReader(String excelFileName){

        RealMatrix data = MatrixUtils.createRealMatrix(835,1692); // creating our realmatrix array that has the users rating for any poi

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(excelFileName));

            CSVReader csvReader = new CSVReader(bufferedReader); // helps to read csv files
            String [] table; // helping array that keeps the users rating

            while ((table = csvReader.readNext()) != null)
            {
                data.setEntry(Integer.parseInt(table[0].trim()),Integer.parseInt(table[1].trim()),Integer.parseInt(table[2].trim())); // setting the score of a specific poi from a specific user

            }

        }
        catch (FileNotFoundException filenotfoundexpeption){
            System.err.println("Error finding the file.");
        }
        catch (IOException io){
            System.err.println("Error closing file.");

        }

        try {
            bufferedReader.close();
        }catch (IOException e){
            System.err.println("Error closing file.");
        }

        return data;
    }

    public static void main(String args[]) throws IOException{

        String fileName = "input_matrix_no_zeros.csv"; // fileName of the excel file

        RealMatrix Master_Data; // a realmatrix array that has poi's rating without any further computation
        Master_Data = DatasetReader(fileName);

    }

}
