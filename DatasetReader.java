import com.opencsv.CSVReader;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.*;

public class DatasetReader {

    public static RealMatrix DatasetReader(String excelFileName){

        RealMatrix data = MatrixUtils.createRealMatrix(765,1964); // creating our realmatrix array that has the users rating for any poi

      //  System.out.println(data);

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(excelFileName));

            CSVReader csvReader = new CSVReader(bufferedReader); // helps to read csv files
            String [] table; // helping array that keeps the users rating

            while ((table = csvReader.readNext()) != null)
            {
                data.setEntry(Integer.parseInt(table[0].trim()),Integer.parseInt(table[1].trim()),Integer.parseInt(table[2].trim())); // setting the score of a specific poi from a specific user
                //table[0] --> is the row (user)
                //table[1[ --> is the column (poi)
                //table[2] --> is the rate of a specific poi (score)
         //       System.out.println(table[0] + table[1] + table[2]);
            }
  //          System.out.println("\n\n" + "aaaaaaaaaaaaaaaek");
 //           System.out.println("/n" + "\n");
 //           System.out.println(data);

  //          System.out.println("/n" + "\n");

/*           for (int i = 0; i < 765; i++)
             {
                for(int j = 0; j < 1964; j++) {
                    if (data.getEntry(i, j) != 0)
                        System.out.print(data.getEntry(i,j) + "  " + i + "  " + j + "  " );
                    //    data.setEntry(i, j, 10);
             }
           }*/

 //           System.out.println("/n" + "\n");
//           System.out.println(data);
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

 //       System.out.println(Master_Data);
    }

}


/* import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DatasetReader {

    public static RealMatrix DatasetReader(String excelFileName) throws IOException {

        RealMatrix data = MatrixUtils.createRealMatrix(764,1963);


        FileInputStream fileInputStream = new FileInputStream(new File(excelFileName));

        HSSFWorkbook wb = new HSSFWorkbook(fileInputStream); //create workbook instance that refers to our .csv file

        HSSFSheet sheet = wb.getSheetAt(0); //creating a sheet objevt to retrive the sheet

        FormulaEvaluator formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator(); // that is for evaluate the cell type

        for (Row row : sheet){
            for(Cell cell : row){
                if(formulaEvaluator.evaluateInCell(cell).getCellType() == Cell.CELL_TYPE_NUMERIC)
                {
                    System.out.println(cell.getNumericCellValue() + " \t\t" + "\n" );
                }
            }

        }

        return data;
    }

    public static void main(String args[]) throws IOException{

        String fileName = "input_matrix_no_zeros.csv";

        RealMatrix MasterData;
        MasterData = DatasetReader(fileName);

    }

} */
