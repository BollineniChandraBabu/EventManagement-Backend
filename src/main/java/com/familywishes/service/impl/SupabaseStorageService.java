package com.familywishes.service.impl;

import com.familywishes.entity.enums.EmailType;
import com.familywishes.entity.enums.Gender;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
public class SupabaseStorageService {
  private final S3Client s3Client;
  private final String bucket;
  private final String publicBaseUrl;
  private final String maleDefaultImagePath;
  private final String femaleDefaultImagePath;
  private final String otherDefaultImagePath;
  private final S3Presigner presigner;

  public SupabaseStorageService(
      @Value("${app.supabase.storage.endpoint}") String endpoint,
      @Value("${app.supabase.storage.region:ap-southeast-1}") String region,
      @Value("${app.supabase.storage.access-key-id}") String accessKeyId,
      @Value("${app.supabase.storage.secret-access-key}") String secretAccessKey,
      @Value("${app.supabase.storage.bucket:golden-greetings}") String bucket,
      @Value("${app.supabase.storage.public-base-url:}") String publicBaseUrl,
      @Value("${app.default-profile-picture.male-path:Profile Pictures/default/male.png}")
          String maleDefaultImagePath,
      @Value("${app.default-profile-picture.female-path:Profile Pictures/default/female.png}")
          String femaleDefaultImagePath,
      @Value("${app.default-profile-picture.other-path:Profile Pictures/default/male.png}")
          String otherDefaultImagePath) {
    this.bucket = bucket;
    this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    this.maleDefaultImagePath = maleDefaultImagePath;
    this.femaleDefaultImagePath = femaleDefaultImagePath;
    this.otherDefaultImagePath = otherDefaultImagePath;
    this.s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .forcePathStyle(true)
            .build();
    this.presigner =
        S3Presigner.builder()
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

  public String uploadUserProfilePicture(MultipartFile file, Long userId) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try {
      String original = file.getOriginalFilename() == null ? "profile.png" : file.getOriginalFilename();
      String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
      String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
      String objectKey =
          "Profile Pictures/" + userId + "/" + datePrefix + "/" + UUID.randomUUID() + "-" + safeName;
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .contentType(file.getContentType() == null ? "image/png" : file.getContentType())
              .build();
      s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
      return toPublicUrl(objectKey);
    } catch (Exception ex) {
      log.error("Failed to upload profile picture", ex);
      return null;
    }
  }

  public PresignedUpload generateUserProfilePictureUploadUrl(
      Long userId, String fileName, String contentType) {
    String original = fileName == null || fileName.isBlank() ? "profile.png" : fileName;
    String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String objectKey =
        "Profile Pictures/" + userId + "/" + datePrefix + "/" + UUID.randomUUID() + "-" + safeName;
    String uploadContentType =
        contentType == null || contentType.isBlank() ? "image/png" : contentType;

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(uploadContentType)
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putObjectRequest)
            .build();
    PresignedPutObjectRequest presignedPutObjectRequest =
        presigner.presignPutObject(presignRequest);
    return new PresignedUpload(
        presignedPutObjectRequest.url().toString(),
        toPublicUrl(objectKey),
        objectKey,
        "PUT",
        600);
  }

  public boolean deleteByPublicUrl(String publicUrl) {
    if (publicUrl == null || publicUrl.isBlank()) {
      return false;
    }
    String objectKey = toObjectKey(publicUrl);
    return deleteObject(objectKey);
  }

  public String getDefaultProfilePictureUrl(Gender gender) {
    return switch (gender == null ? Gender.OTHER : gender) {
      case MALE -> toPublicUrl(maleDefaultImagePath);
      case FEMALE -> toPublicUrl(femaleDefaultImagePath);
      case OTHER -> toPublicUrl(otherDefaultImagePath);
    };
  }

  public String resolveProfilePictureUrl(String profilePictureUrlOrObjectKey) {
    if (profilePictureUrlOrObjectKey == null || profilePictureUrlOrObjectKey.isBlank()) {
      return null;
    }

    String value = profilePictureUrlOrObjectKey.trim();
    if (value.startsWith("http://") || value.startsWith("https://")) {
      if (value.contains("X-Amz-Algorithm=") || value.contains("X-Amz-Signature=")) {
        try {
          String path = URI.create(value).getPath();
          String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
          String marker = bucket + "/";
          int markerIndex = normalizedPath.indexOf(marker);
          if (markerIndex >= 0) {
            String objectKey =
                URLDecoder.decode(
                    normalizedPath.substring(markerIndex + marker.length()),
                    StandardCharsets.UTF_8);
            return toPublicUrl(objectKey);
          }
        } catch (Exception ex) {
          log.warn("Unable to parse presigned profile picture URL; returning original value");
        }
      }
      return value;
    }
    return toPublicUrl(value);
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

  private String toPublicUrl(String objectKey) {
    if (publicBaseUrl.isBlank()) {
      return objectKey;
    }
    String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    return base + "/" + objectKey;
  }

  private String toObjectKey(String publicUrlOrObjectKey) {
    if (publicBaseUrl.isBlank()) {
      return publicUrlOrObjectKey;
    }
    String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    if (publicUrlOrObjectKey.startsWith(base + "/")) {
      return publicUrlOrObjectKey.substring(base.length() + 1);
    }
    return publicUrlOrObjectKey;
  }

  public record PresignedUpload(
      String uploadUrl, String publicUrl, String objectKey, String method, long expiresInSeconds) {}
}
