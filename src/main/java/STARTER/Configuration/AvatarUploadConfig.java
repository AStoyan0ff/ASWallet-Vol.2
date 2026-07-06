package STARTER.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
// Advanced — serve locally uploaded avatars at /uploads/avatars/**
public class AvatarUploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.avatars-dir:uploads/avatars}")
    private String avatarsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadPath = Paths.get(avatarsDir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();

        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(location);
    }
}
