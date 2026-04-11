package fmcg.distribution.controller;

import fmcg.distribution.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private StorageService storageService;

@GetMapping("/**")
public ResponseEntity<byte[]> getFile(HttpServletRequest request) {
    try {
        String uri = request.getRequestURI();

        // Extract correct path AFTER /api/files/
        String fullPath = uri.substring(uri.indexOf("/api/files/") + "/api/files/".length());

        System.out.println("FULL PATH: " + fullPath); // 🔥 debug

        byte[] fileData = storageService.getFile(fullPath);

        if (fileData == null || fileData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, getContentType(fullPath))
                .body(fileData);

    } catch (Exception e) {
        e.printStackTrace(); // 🔥 VERY IMPORTANT
        return ResponseEntity.internalServerError().build();
    }
}

    private String getContentType(String path) {
        String lower = path.toLowerCase();

        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (lower.endsWith(".webp")) return "image/webp";

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
