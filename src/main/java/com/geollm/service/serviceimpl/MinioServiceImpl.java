package com.geollm.service.serviceimpl;

import com.geollm.service.MinioService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;

@Service
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;
    private final String bucket;

    public MinioServiceImpl(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        try {
            // 启动时尝试创建 bucket；如果已存在则忽略异常，避免重复创建失败
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        } catch (Exception ignored) {
            // bucket 已存在则忽略
        }
    }

    @Override
    public void upload(String objectName, MultipartFile file) throws Exception {
        // objectName 的命名规则由业务层保证（例如：`${aid}_${pageIndex}`、`${aid}_${pageIndex}_thumbnail`）
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
    }

    @Override
    public String presignedGetUrl(String objectName, Duration expiry) throws Exception {
        // 生成给前端/外部访问用的临时下载链接（避免直接暴露私有对象）
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry((int) expiry.toSeconds())
                .build());
    }

    @Override
    public void delete(String objectName) throws Exception {
        // 删除单个对象（objectName 需与 upload 时保持一致）
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
    }
}

