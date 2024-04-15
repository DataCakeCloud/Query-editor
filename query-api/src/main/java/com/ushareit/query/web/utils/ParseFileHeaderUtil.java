package com.ushareit.query.web.utils;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ParseFileHeaderUtil {
    public static ArrayList<Map<String, Object>> readFileHeader(MultipartFile file) {
        ArrayList<Map<String, Object>> meta = new ArrayList<>();
        String fileName = file.getOriginalFilename();
        assert fileName != null;
        if (fileName.endsWith(".csv")) {
            meta = ParseFileHeaderUtil.readCSV(file);
        } else if (fileName.endsWith(".xls")) {
            meta = ParseFileHeaderUtil.readXLS(file);
        } else if (fileName.endsWith(".xlsx")) {
            meta = ParseFileHeaderUtil.readXLSX(file);
        }
        return meta;
    }

    private static ArrayList<Map<String, Object>> readCSV(MultipartFile file) {
        ArrayList<Map<String, Object>> meta = new ArrayList<>();
        try{
            InputStream inputStream = file.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String lineTxt;
            if ((lineTxt=bufferedReader.readLine())!=null){
                InputStream inputStreamBom = file.getInputStream();
                BOMInputStream bomIn = new BOMInputStream(inputStreamBom);
                boolean hasBom = bomIn.hasBOM();
                lineTxt= lineTxt.replace("\"", "");

                if (hasBom) {
                    byte[] byteLine = lineTxt.getBytes();
                    if (byteLine[0]==(byte)0xef && byteLine[1]==(byte)0xbb && byteLine[2]==(byte)0xbf) {
                        lineTxt = new String(byteLine, 3, byteLine.length-3);
                    }
                }

                List<String> title = Arrays.asList(lineTxt.split(","));
                for (String s : title) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("name", s);
                    column.put("type", "string");
                    meta.add(column);
                }
                inputStreamBom.close();
            }
            inputStream.close();
            bufferedReader.close();
        } catch(IOException e){
            try{
                throw e;
            }catch(IOException se){
                se.printStackTrace();
            }
        }
        return meta;
    }

    private static ArrayList<Map<String, Object>> readXLS(MultipartFile file) {
        ArrayList<Map<String, Object>> meta = new ArrayList<>();

        try{
            InputStream inputStream = file.getInputStream();
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = workbook.getSheetAt(0);
            HSSFRow row = sheet.getRow(0);
            if (row != null) {
                int lastCellNum = row.getLastCellNum();
                for (int idx = 0; idx < lastCellNum; idx++) {
                    row.getCell(idx).setCellType(CellType.STRING);
                    String cell = row.getCell(idx).getStringCellValue();
                    Map<String, Object> column = new HashMap<>();
                    column.put("name", cell);
                    column.put("type", "string");
                    meta.add(column);
                }
            }

        } catch(IOException e){
            try{
                throw e;
            }catch(IOException se){
                se.printStackTrace();
            }
        }

        return meta;
    }

    private static ArrayList<Map<String, Object>> readXLSX(MultipartFile file) {
        ArrayList<Map<String, Object>> meta = new ArrayList<>();

        try{
            InputStream inputStream = file.getInputStream();
            XSSFWorkbook Workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = Workbook.getSheetAt(0);
            XSSFRow row = sheet.getRow(0);
            if (row != null) {
                int lastCellNum = row.getLastCellNum();
                for (int idx = 0; idx < lastCellNum; idx++) {
                    row.getCell(idx).setCellType(CellType.STRING);
                    String cell = row.getCell(idx).getStringCellValue();
                    Map<String, Object> column = new HashMap<>();
                    column.put("name", cell);
                    column.put("type", "string");
                    meta.add(column);
                }
            }
        } catch(IOException e){
            try{
                throw e;
            }catch(IOException se){
                se.printStackTrace();
            }
        }

        return meta;
    }
}
