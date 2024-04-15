package com.ushareit.query.web.utils;

import com.ksyun.ks3.dto.PutObjectResult;
import com.ksyun.ks3.service.Ks3;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.Ks3ClientConfig;

import java.io.File;

public class KsCloudUtil {
    public static Ks3 Ks3ObjectStorage(String endpoint, String accessKey, String secretKey) {
        Ks3ClientConfig config = new Ks3ClientConfig();
        config.setEndpoint(endpoint);
        config.setPathStyleAccess(false);
        return new Ks3Client(accessKey, secretKey, config);
    }

    public static String putObject(String endpoint, String accessKey, String secretKey,
                                   String bucketName, String objectKey, File file) {
        Ks3 ks3 = Ks3ObjectStorage(endpoint, accessKey, secretKey);
        PutObjectResult res = ks3.putObject(bucketName, objectKey, file);
        return res.geteTag();
    }
}
