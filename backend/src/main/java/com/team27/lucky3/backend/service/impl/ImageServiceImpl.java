package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.repository.ImageRepository;
import com.team27.lucky3.backend.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.io.IOException;
import java.util.Optional;

@Service
public class ImageServiceImpl implements ImageService {
    @Value("classpath:image/default-avatar.png")
    private Resource defaultAvatar;

    private final ImageRepository imageRepository;
    private final Path rootLocation = Paths.get("uploads");

    public ImageServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public Image store(MultipartFile file) throws IOException {
        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
             extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        Image image = new Image();
        image.setFileName(filename);
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        // Data is not stored in DB anymore
        image.setData(null);
        return imageRepository.save(image);
    }

    public Optional<Image> findById(Long id) {
        return imageRepository.findById(id);
    }

    @Override
    public byte[] loadImage(String filename) throws IOException {
        Path file = rootLocation.resolve(filename);
        if (Files.exists(file)) {
            return Files.readAllBytes(file);
        }
        return new byte[0];
    }

    @Override
    public Image getDefaultAvatar() {
        try {
            // Ensure default avatar exists in uploads
            Path targetLocation = this.rootLocation.resolve("default-avatar.png");
            if (!Files.exists(targetLocation)) {
                if (!Files.exists(rootLocation)) {
                    Files.createDirectories(rootLocation);
                }
                Files.copy(defaultAvatar.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Create and persist a new Image record for default avatar
            Image image = new Image();
            image.setFileName("default-avatar.png");
            image.setContentType(MediaType.IMAGE_PNG_VALUE);
            image.setSize(Files.size(targetLocation));
            image.setData(null); // No data stored in DB
            return imageRepository.save(image);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load default avatar image", e);
        }

    }
}



