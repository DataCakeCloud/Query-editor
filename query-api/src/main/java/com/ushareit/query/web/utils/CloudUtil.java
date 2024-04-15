package com.ushareit.query.web.utils;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.obs.services.ObsClient;
import com.obs.services.model.PutObjectResult;
import com.ushareit.query.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * @author: tianxu
 * @create: 2022-05-23 10:24
 */

@Slf4j
@Service
public class CloudUtil {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.CHINA) ;

    private static File multipartFileToFile(MultipartFile multiFile, String upload_local_tmp) {
        String fileCsv = null;
        File file = null;

        String fileName = multiFile.getOriginalFilename();
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        String name = fileName.substring(0, fileName.lastIndexOf("."));
        String tmpDir = upload_local_tmp + fileName;
        try {
            if (prefix.equals(".csv")) {
                if (new File(tmpDir).exists()) {
                    new File(tmpDir).delete();
                }
//                fileCsv = FileUtil.csvRemoveFirstLine(multiFile);
                file = File.createTempFile(prefix, name, new File(upload_local_tmp));
                FileUtils.copyInputStreamToFile(multiFile.getInputStream(), file);
//                FileUtils.copyInputStreamToFile(new ByteArrayInputStream(fileCsv.getBytes("UTF-8")), file);
            }else if (prefix.equals(".xls") || prefix.equals(".xlsx")) {
                fileCsv = FileUtil.excelToCsv(multiFile);
//                tmpDir = upload_local_tmp + fileCsv.getName();
                tmpDir = upload_local_tmp + name + ".csv";
                name = ".csv";
                file = File.createTempFile(prefix, name, new File(upload_local_tmp));
                if (new File(tmpDir).exists()) {
                    new File(tmpDir).delete();
                }
//                FileUtils.copyInputStreamToFile(new FileInputStream(fileCsv), file);
                FileUtils.copyInputStreamToFile(new ByteArrayInputStream(fileCsv.getBytes("UTF-8")), file);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
//            throw new ServiceException(BaseResponseCodeEnum.AWS_FILE_CONVERSION_FAIL);
            throw new ServiceException(fileName, "AWS文件转化失败");
        }
        return file;
    }

    public static String upload(Map<String, Object> params, String user, MultipartFile multipartFile) throws IOException {
        String dir = "";
        if (params.get("engine_key").equals("presto_aws") || params.get("engine_key").equals("spark-submit-sql-3_aws_us-east-1") || params.get("engine_key").equals("ares_ue1")) {
            dir = "ue1/";
        } else if (params.get("engine_key").equals("presto_aws_sg") || params.get("engine_key").equals("spark-submit-sql-3_aws_ap-southeast-1") || params.get("engine_key").equals("ares_ap1")) {
            dir = "sg1/";
        } else if (params.get("cloud_provider").equals("googlecloud")
                || params.get("cloud_provider").equals("ksyun")) {
            dir = params.get("ue1_aws_region").toString() + "/";
        }
        String fileDir = "tempdb/" + dir + "months_" + params.get("lifeCycle") + "/" + user + "/" + String.format("%s_%s/", user, dateFormat.format(new Date(System.currentTimeMillis())));

        String location = "";
        String filePath = fileDir + params.get("filename");

        if (params.get("cloud_provider").equals("googlecloud")) {
            GoogleCloudUtil.upload(
                    params.get("ue1_aws_bucket").toString(),
                    filePath,
                    multipartFile.getInputStream());
            location = "gs://" + params.get("ue1_aws_bucket") + "/" + fileDir;
        } else if (params.get("cloud_provider").equals("ksyun")) {
            String aws_prefix = (String)params.get("ue1_aws_prefix");
            if (null != aws_prefix && aws_prefix.length() > 0) {
                if (!aws_prefix.substring(aws_prefix.length() - 1).equals("/")) {
                    aws_prefix += "/";
                }
                fileDir = aws_prefix + fileDir;
                filePath = aws_prefix + filePath;
            }
            KsCloudUtil.putObject(params.get("ks_endpoint").toString(),
                    params.get("ks_access_key_id").toString(),
                    params.get("ks_secret_access_key").toString(),
                    params.get("ue1_aws_bucket").toString(),
                    filePath,
                    multipartFileToFile(multipartFile, params.get("upload_local_tmp").toString()));
            location = "ks3://" + params.get("ue1_aws_bucket") + "/" + fileDir;
        } else if (!params.get("engine_key").equals("presto_huawei") && !params.get("engine_key").equals("spark-submit-sql-3_huawei_ap-southeast-3")) {
        	String aws_prefix = (String)params.get("ue1_aws_prefix");
        	if (null != aws_prefix && aws_prefix.length() > 0) {
                if (!aws_prefix.substring(aws_prefix.length() - 1).equals("/")) {
                    aws_prefix += "/";
                }
        	    fileDir = aws_prefix + fileDir;
        	    filePath = aws_prefix + filePath;
        	}
            AWSCredentials awsCredentials = new BasicAWSCredentials(params.get("ue1_aws_access_keys_id").toString(),
                    params.get("ue1_aws_secret_access_key").toString());
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(params.get("ue1_aws_region").toString())
                    .build();
            AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                    .withRoleArn(params.get("s3_role_data").toString())
                    .withRoleSessionName("QETmpTable");
            AssumeRoleResult assumeRoleResult = stsClient.assumeRole(assumeRoleRequest);
            Credentials sessionCredentials = assumeRoleResult.getCredentials();
            BasicSessionCredentials tmpCredentials = new BasicSessionCredentials(
                    sessionCredentials.getAccessKeyId(),
                    sessionCredentials.getSecretAccessKey(),
                    sessionCredentials.getSessionToken());
            final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(tmpCredentials))
                    .withRegion(params.get("ue1_aws_region").toString())
                    .build();
            s3.putObject(params.get("ue1_aws_bucket").toString(), filePath, multipartFileToFile(multipartFile, params.get("upload_local_tmp").toString()));
            location = "s3://" + params.get("ue1_aws_bucket") + "/" + fileDir;
        } else {
            ObsClient obsClient = new ObsClient(params.get("obs_access_key_id").toString(), params.get("obs_secret_access_key").toString(), params.get("obs_endPoint").toString());
//                    obsClient.putObject(bucket, object, new ByteArrayInputStream("Hello OBS".getBytes()));
            PutObjectResult putObjectResult = obsClient.putObject(params.get("obs_bucket").toString(), filePath, multipartFileToFile(multipartFile, params.get("upload_local_tmp").toString()));
            location = "obs://" + params.get("obs_bucket") + "/" + fileDir;
//            String url1 = putObjectResult.getObjectUrl();
//            String url2 = URLDecoder.decode(url1, "UTF-8");
//            System.out.println(url2);
        }

        return location;
    }
}
