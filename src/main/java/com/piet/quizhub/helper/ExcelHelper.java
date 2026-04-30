package com.piet.quizhub.helper;

import com.piet.quizhub.entity.Question;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    public static List<Question> excelToQuestions(InputStream is, String selectedRound) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            List<Question> questions = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Skip Header Row
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Question q = new Question();
                
                // Column 0 to 4: Question content and options
                q.setContent(getCellValue(currentRow.getCell(0)));
                q.setOptionA(getCellValue(currentRow.getCell(1)));
                q.setOptionB(getCellValue(currentRow.getCell(2)));
                q.setOptionC(getCellValue(currentRow.getCell(3)));
                q.setOptionD(getCellValue(currentRow.getCell(4)));
                
                // Column 5: Correct Answer
                String correctVal = getCellValue(currentRow.getCell(5)).trim().toUpperCase();
                q.setCorrectAns(correctVal);

                // --- FIX: Dropdown wala round use karo ---
                // Excel mein column 6 ho ya na ho, farak nahi padta
                q.setCategory(selectedRound); 

                // Khali rows ko skip karne ke liye safety check
                if (q.getContent() != null && !q.getContent().isEmpty()) {
                    questions.add(q);
                }
            }
            workbook.close();
            return questions;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Excel parse karne mein galti: " + e.getMessage());
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
}