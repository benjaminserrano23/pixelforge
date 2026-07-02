package com.pixelforge.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone pixelforge.uploads.dir como archivos estáticos bajo /uploads/**,
 * para que coverImageUrl (ej. "/uploads/uuid.png") sea directamente cargable
 * por el <img> del frontend.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadsDir;

    public WebConfig(@Value("${pixelforge.uploads.dir}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadsDir.endsWith("/") ? uploadsDir : uploadsDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
