package com.pixelforge.app.game;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda portadas en disco bajo pixelforge.uploads.dir y devuelve una URL
 * relativa servida como recurso estático (ver WebConfig). MVP: disco local +
 * volumen Docker; migrar a S3/Cloud Storage sería un cambio interno de esta
 * clase sin tocar el resto de la app.
 */
@Service
public class CoverStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final Path uploadsDir;

    public CoverStorageService(@Value("${pixelforge.uploads.dir}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir);
        try {
            Files.createDirectories(this.uploadsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("no se pudo crear el directorio de uploads: " + this.uploadsDir, e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidCoverException("el archivo está vacío");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidCoverException("la imagen supera el máximo de 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidCoverException("formato no soportado, usa PNG, JPEG o WEBP");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;

        try {
            file.transferTo(uploadsDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("no se pudo guardar la imagen", e);
        }

        return "/uploads/" + filename;
    }

    public static class InvalidCoverException extends RuntimeException {
        public InvalidCoverException(String message) {
            super(message);
        }
    }
}
