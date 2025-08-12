package com.campusconnect.service;

import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepo;

    @Value("${profile.pics.dir:profile-pics}")
    private String profilePicsDir;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public boolean deleteUserByEmail(String email) {
        try {
            // First verify the user exists
            Optional<User> user = userRepo.findByEmail(email);
            if (!user.isPresent()) {
                return false;
            }

            // Perform the delete operation
            int deleted = userRepo.deleteByEmail(email);
            
            // Verify the deletion was successful
            if (deleted > 0) {
                // Double check the user is actually deleted
                return !userRepo.findByEmail(email).isPresent();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    public String saveProfilePic(String email, MultipartFile file) {
        try {
            Optional<User> userOpt = userRepo.findByEmail(email);
            if (userOpt.isEmpty()) throw new RuntimeException("User not found");
            User user = userOpt.get();

            // Save file
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String filename = email.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ext;
            Path dir = Paths.get(profilePicsDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Set URL (served from /profile-pics/)
            String imageUrl = "/profile-pics/" + filename;
            user.setProfilePicUrl(imageUrl);
            userRepo.save(user);
            return imageUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save profile picture", e);
        }
    }
} 