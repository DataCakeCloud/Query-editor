package com.ushareit.query.web.utils;


import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.ushareit.query.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class FileUtil {
    public static String getStringSize(long size) {
        double doubleSize = 0;
        String unit = "KB";
        if (size >= 1073741824) {
            doubleSize = size * 1.0 / 1073741824;
            unit = "GB";
        } else if (size >= 1048576) {
            doubleSize = size * 1.0 / 1048576;
            unit = "MB";
        } else {
            doubleSize = size * 1.0 / 1024;
            unit = "KB";
        }
        String result = String.format("%.2f", doubleSize);
        return result + unit;
    }

    public static String excelToCsv(MultipartFile file){
        String buffer = "";
        Workbook wb =null;
        Sheet sheet = null;
        Row row = null;
        String cellData = null;
        File fileText = null;

        String fileName = file.getOriginalFilename();
        String fileNameNew = fileName.substring(0, fileName.lastIndexOf(".")) + ".csv";

        wb = readExcel(file);
        if(wb != null){
            //获取第一个sheet
            sheet = wb.getSheetAt(0);
            //获取最大行数
            int rownum = sheet.getPhysicalNumberOfRows();
            //获取第一行
            row = sheet.getRow(0);
            //获取最大列数
            int colnum = row.getPhysicalNumberOfCells();
            for (int i = 0; i<rownum; i++) {
                row = sheet.getRow(i);
                for (int j = 0; j < colnum; j++) {
                    cellData = (String) getCellFormatValue(row.getCell(j));
                    buffer +=cellData;
                }
                buffer = buffer.substring(0, buffer.lastIndexOf(",")).toString();
                buffer += "\n";
            }

//            try {
//                // 创建文件对象
//                fileText = new File(fileNameNew);
//                // 向文件写入对象写入信息
//                FileWriter fileWriter = new FileWriter(fileText);
//                // 写文件
//                fileWriter.write(buffer);
//                // 关闭
//                fileWriter.close();
//                System.out.println(buffer);
//            } catch (IOException e) {
//                try{
//                    throw e;
//                }catch(IOException se){
//                    se.printStackTrace();
//                }
//            }
        }
        return buffer;

    }

    public static Workbook readExcel(MultipartFile file){
        Workbook wb = null;
        String fileName = file.getOriginalFilename();
        try {
            if(fileName.endsWith(".xls")){
                InputStream inputStream = file.getInputStream();
                HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
                wb = workbook;
            }else if(fileName.endsWith(".xlsx")){
                InputStream inputStream = file.getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                wb = workbook;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wb;
    }

    public static Object getCellFormatValue(Cell cell){
        Object cellValue = null;
        if(cell!=null){
            //判断cell类型
            switch(cell.getCellType()){
                case Cell.CELL_TYPE_NUMERIC:{
                    cellValue = String.valueOf(cell.getNumericCellValue()).replaceAll("\n", " ") + ",";
                    break;
                }
                case Cell.CELL_TYPE_FORMULA:{
                    //判断cell是否为日期格式
                    if(DateUtil.isCellDateFormatted(cell)){
                        //转换为日期格式YYYY-mm-dd
                        cellValue = String.valueOf(cell.getDateCellValue()).replaceAll("\n", " ") + ",";;
                    }else{
                        //数字
                        cellValue = String.valueOf(cell.getNumericCellValue()).replaceAll("\n", " ") + ",";;
                    }
                    break;
                }
                case Cell.CELL_TYPE_STRING:{
                    cellValue = cell.getRichStringCellValue().getString().replaceAll("\n", " ") + ",";;
                    break;
                }
                default:
                    cellValue = "";
            }
        }else{
            cellValue = "";
        }
        return cellValue;
    }

    public static String csvRemoveFirstLine(MultipartFile file) {
        String buffer = "";
        List<String[]> list = new ArrayList<String[]>();
        int i = 0;
        try {
            CSVReader csvReader = new CSVReaderBuilder(
                    new BufferedReader(
                            new InputStreamReader(file.getInputStream(), "utf-8"))).build();
            Iterator<String[]> iterator = csvReader.iterator();
            while (iterator.hasNext()) {
                String[] next = iterator.next();
                //去除第一行的表头，从第二行开始
                if (i >= 1) {
                    String tempStr = String.join(",", next);
                    buffer = buffer + tempStr + "\n";
                }
                i++;
            }
            csvReader.close();
        } catch (Exception e) {
            log.error("CSV文件读取异常");
            try {
                throw e;
            } catch (Exception ex) {
                String message = ex.getMessage().trim();
                log.error(String.format("There is an Exception when remove the first line of csv [%s]: %s", file.getOriginalFilename(), message));
            }
        }
        return buffer;
    }
}
