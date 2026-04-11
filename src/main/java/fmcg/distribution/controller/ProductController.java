package fmcg.distribution.controller;

import fmcg.distribution.dto.*;
import fmcg.distribution.service.ProductService;
import fmcg.distribution.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private StorageService storageService;

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest request) {
        try {
            ProductResponse response = productService.createProduct(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, request));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/upload-image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = productService.uploadImage(id, file);
            Map<String, String> response = new HashMap<>();
            response.put("image_url", imageUrl);
            response.put("path", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("detail", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

@RestController
@RequestMapping("/files")
class FileController {

    @Autowired
    private StorageService storageService;

    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI()
                    .substring(request.getContextPath().length())
                    .replaceFirst("/api/files/", "");
            byte[] fileData = storageService.getFile(fullPath);
            return ResponseEntity.ok()
                    .contentType(getContentType(fullPath))
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType getContentType(String path) {
        if (path == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = path.toLowerCase();
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif"))  return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".svg"))  return MediaType.parseMediaType("image/svg+xml");
        if (lower.endsWith(".pdf"))  return MediaType.APPLICATION_PDF;
        return MediaType.IMAGE_JPEG; // default for .jpg / .jpeg and unknown types
    }
}