package fmcg.distribution.controller;

import fmcg.distribution.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private StorageService storageService;

    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) {

        String fullPath = request.getRequestURI()
                .substring(request.getContextPath().length())
                .replaceFirst("/api/files/", "");

        byte[] fileData = storageService.getFile(fullPath);

        return ResponseEntity.ok()
                .header("Content-Type", getContentType(fullPath))
                .body(fileData);
    }

    private String getContentType(String path) {
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
