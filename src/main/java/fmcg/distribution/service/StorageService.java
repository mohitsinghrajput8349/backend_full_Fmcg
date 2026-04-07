package fmcg.distribution.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${storage.url}")
    private String storageUrl;

    @Value("${storage.emergent.key}")
    private String emergentKey;

    @Value("${storage.app.name}")
    private String appName;

    private String storageKey;

    private WebClient webClient = WebClient.create();

    private String initStorage() {
        if (storageKey != null) {
            return storageKey;
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("emergent_key", emergentKey);

        Map<String, String> response = webClient.post()
                .uri(storageUrl + "/init")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        storageKey = response.get("storage_key");
        return storageKey;
    }

    public String uploadFile(MultipartFile file, String path) {
        try {
            String key = initStorage();
            String extension = getFileExtension(file.getOriginalFilename());
            String filePath = appName + "/" + path + "/" + UUID.randomUUID() + "." + extension;

            webClient.put()
                    .uri(storageUrl + "/objects/" + filePath)
                    .header("X-Storage-Key", key)
                    .header("Content-Type", file.getContentType())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    public byte[] getFile(String path) {
        String key = initStorage();
        return webClient.get()
                .uri(storageUrl + "/objects/" + path)
                .header("X-Storage-Key", key)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}