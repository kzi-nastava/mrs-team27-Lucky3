package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface ImageService {
    public Image store(MultipartFile file) throws IOException;
    public Optional<Image> findById(Long id);
    Image getDefaultAvatar();
    byte[] loadImage(String filename) throws IOException;
}

