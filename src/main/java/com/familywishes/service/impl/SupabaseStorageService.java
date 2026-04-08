package com.familywishes.service.impl;

import com.familywishes.entity.enums.EmailType;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class SupabaseStorageService {
  private final S3Client s3Client;
  private final String bucket;

  public SupabaseStorageService(
      @Value("${app.supabase.storage.endpoint}") String endpoint,
      @Value("${app.supabase.storage.region:ap-southeast-1}") String region,
      @Value("${app.supabase.storage.access-key-id}") String accessKeyId,
      @Value("${app.supabase.storage.secret-access-key}") String secretAccessKey,
      @Value("${app.supabase.storage.bucket:golden-greetings}") String bucket) {
    this.bucket = bucket;
    this.s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .forcePathStyle(true)
            .build();
  }

  public String uploadEmailImage(byte[] imageData, EmailType emailType) {
    if (imageData == null || imageData.length == 0) {
      return null;
    }

    String folder = resolveFolder(emailType);
    String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String objectKey = folder + "/" + datePrefix + "/" + UUID.randomUUID() + ".png";

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType("image/png")
            .build();

    s3Client.putObject(request, RequestBody.fromBytes(imageData));
    return objectKey;
  }


  public String uploadChatAttachment(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    try {
      String original =
          file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
      String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
      String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
      String objectKey = "Chat/" + datePrefix + "/" + UUID.randomUUID() + "-" + safeName;

      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .contentType(file.getContentType())
              .build();

      s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
      return objectKey;
    } catch (Exception ex) {
      log.error("Failed to upload chat attachment", ex);
      return null;
    }
  }

  public byte[] downloadImage(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return null;
    }

    try {
      GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(objectKey).build();
      return s3Client.getObjectAsBytes(request).asByteArray();
    } catch (Exception ex) {
      log.warn("Failed to download image from Supabase storage for key={}", objectKey, ex);
      return null;
    }
  }

  public boolean deleteObject(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return false;
    }

    try {
      DeleteObjectRequest request =
          DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build();
      s3Client.deleteObject(request);
      return true;
    } catch (Exception ex) {
      log.warn("Failed to delete object from Supabase storage for key={}", objectKey, ex);
      return false;
    }
  }

  private String resolveFolder(EmailType emailType) {
    if (emailType == null) {
      return "Events";
    }

    return switch (emailType) {
      case BIRTHDAY -> "Birthday";
      case GOOD_MORNING -> "Good Morning";
      case GOOD_NIGHT -> "Good Night";
      case EVENT -> "Events";
      default -> "Festivals";
    };
  }
}
