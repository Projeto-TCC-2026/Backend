package com.tcc.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3JsonCacheStorageTest {

    private static final String BUCKET = "tcc-dashboard-cache";
    private static final String KEY = "dashboard/admin.json";
    private static final String JSON = "{\"totalHospitals\":3}";

    @Mock
    private S3Client s3Client;

    private S3JsonCacheStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3JsonCacheStorage(s3Client, BUCKET);
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("grava o JSON no bucket configurado, sem ACL pública")
        void shouldPutObjectWithoutPublicAcl() {
            storage.write(KEY, JSON);

            ArgumentCaptor<PutObjectRequest> captor =
                    ArgumentCaptor.forClass(PutObjectRequest.class);

            verify(s3Client).putObject(
                    captor.capture(),
                    any(RequestBody.class));

            PutObjectRequest request = captor.getValue();
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.key()).isEqualTo(KEY);
            assertThat(request.contentType()).isEqualTo("application/json");
            assertThat(request.acl()).isNull();
        }

        @Test
        @DisplayName("falha do S3 na escrita não propaga exceção")
        void shouldNotPropagateWriteFailure() {
            when(s3Client.putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class)))
                    .thenThrow(S3Exception.builder()
                            .message("bucket indisponível")
                            .build());

            assertThatCode(() -> storage.write(KEY, JSON))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("chave existente devolve o conteúdo do objeto")
        void shouldReturnContentWhenKeyExists() {
            ResponseBytes<GetObjectResponse> responseBytes =
                    ResponseBytes.fromByteArray(
                            GetObjectResponse.builder().build(),
                            JSON.getBytes(StandardCharsets.UTF_8));

            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenReturn(responseBytes);

            Optional<String> result = storage.read(KEY);

            assertThat(result).contains(JSON);

            ArgumentCaptor<GetObjectRequest> captor =
                    ArgumentCaptor.forClass(GetObjectRequest.class);

            verify(s3Client).getObjectAsBytes(captor.capture());
            assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
            assertThat(captor.getValue().key()).isEqualTo(KEY);
        }

        @Test
        @DisplayName("chave inexistente devolve Optional vazio")
        void shouldReturnEmptyWhenKeyDoesNotExist() {
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().build());

            Optional<String> result = storage.read(KEY);

            assertThat(result).isEmpty();
        }
    }
}
