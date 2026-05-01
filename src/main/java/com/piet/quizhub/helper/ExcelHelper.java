package com.piet.quizhub.helper;

import com.piet.quizhub.entity.Question;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    /**
     * Excel file ko parse karke Questions ki list banata hai
     * @param is - Excel file ka input stream
     * @param selectedRound - Jo round admin ne dropdown se select kiya hai
     * @return List of Question objects
     */
    public static List<Question> excelToQuestions(InputStream is, String selectedRound) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0); // Pehli sheet read karega
            Iterator<Row> rows = sheet.iterator();
            List<Question> questions = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // 1. Skip Header Row (First Row)
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Question q = new Question();
                
                // 2. Cell values read karna (Columns: 0-Content, 1-A, 2-B, 3-C, 4-D, 5-Ans)
                q.setContent(getCellValue(currentRow.getCell(0)));
                q.setOptionA(getCellValue(currentRow.getCell(1)));
                q.setOptionB(getCellValue(currentRow.getCell(2)));
                q.setOptionC(getCellValue(currentRow.getCell(3)));
                q.setOptionD(getCellValue(currentRow.getCell(4)));
                
                // 3. Correct Answer formatting (A, B, C, or D)
                String correctVal = getCellValue(currentRow.getCell(5)).trim().toUpperCase();
                q.setCorrectAns(correctVal);

                // 4. Dropdown wala round category mein set karna
                q.setCategory(selectedRound); 

                // 5. Khali rows check (Safety Check)
                if (q.getContent() != null && !q.getContent().trim().isEmpty()) {
                    questions.add(q);
                }
            }
            workbook.close();
            return questions;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Excel parsing error: " + e.getMessage());
        }
    }

    /**
     * Har type ke cell (String, Numeric, Boolean) se text nikalne ke liye helper
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        DataFormatter formatter = new DataFormatter(); // Yeh numeric cells ko bhi text mein handle kar lega
        
        switch (cell.getCellType()) {
            case STRING: 
                return cell.getStringCellValue();
            case NUMERIC:
                return formatter.formatCellValue(cell);
            case BOOLEAN: 
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default: 
                return "";
        }
    }
}