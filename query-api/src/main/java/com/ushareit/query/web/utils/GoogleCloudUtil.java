package com.ushareit.query.web.utils;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

@Slf4j
public class GoogleCloudUtil {
    public static Storage storage;

    public static Storage getStorage() throws IOException {
        if (storage != null) {
            return storage;
        }
        StorageOptions storageOptions = StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.getApplicationDefault()).build();
        return storageOptions.getService();
    }

    public static String upload(String bucket, String objectName, InputStream contents) {
        try {
            log.info("Uploading..... bucket:{}, objectName:{}", bucket, objectName);
            Storage storage = getStorage();
            BlobId blobId = BlobId.of(bucket, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            Blob blob = storage.createFrom(blobInfo, contents);
            String link = blob.getSelfLink();
            link = URLDecoder.decode(link, "UTF-8");
            return link;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("upload failed to gcs", e);
            return "";
        }
    }
}
