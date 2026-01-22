package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.entity.Image;
import com.team27.lucky3.backend.repository.ImageRepository;
import com.team27.lucky3.backend.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;      // <-- use this
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
// ImageService.java
@Service
public class ImageServiceImpl implements ImageService {
    @Value("classpath:image/default-avatar.png")
    private Resource defaultAvatar;  // src/main/resources/image/default-avatar.png

    private final ImageRepository imageRepository;

    public ImageServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image store(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setData(file.getBytes());
        return imageRepository.save(image);
    }

    public Optional<Image> findById(Long id) {
        return imageRepository.findById(id);
    }

    public Image getDefaultAvatar() {
        try {
            byte[] bytes = defaultAvatar.getInputStream().readAllBytes();
            Image image = new Image();
            image.setId(null); // not persisted
            image.setFileName("default-avatar.png");
            image.setContentType(MediaType.IMAGE_PNG_VALUE);
            image.setSize((long) bytes.length);
            image.setData(bytes);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load default avatar image", e);
        }
    }
}



