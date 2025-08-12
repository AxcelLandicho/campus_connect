package com.campusconnect.controller;

import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import com.campusconnect.model.RSVP;
import com.campusconnect.service.RsvpService;
import com.campusconnect.model.Event;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RSVPRepository;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserRepository userRepo;
    private final UserService userService;
    private final RsvpService rsvpService;
    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;

    public AuthController(UserRepository userRepo, UserService userService, RsvpService rsvpService, EventRepository eventRepository, RSVPRepository rsvpRepository) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.rsvpService = rsvpService;
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required!"));
            }
            if (user.getName() == null || user.getName().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Name is required!"));
            }
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required!"));
            }
            if (userRepo.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already used!"));
            }
            if (user.getRole().equals("student") && !user.getEmail().endsWith("@g.batstate-u.edu.ph")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Student email must end with @g.batstate-u.edu.ph"));
            }
            if (!"student".equals(user.getRole())) {
                user.setCourse("");
            }

            System.out.println("Saving user: " + user.getEmail() + ", " + user.getName() + ", " + user.getPassword() + ", " + user.getRole() + ", " + user.getCourse());
            User savedUser = userRepo.save(user);
            return ResponseEntity.ok().body(Map.of(
                "message", "Signup successful!",
                "role", savedUser.getRole(),
                "email", savedUser.getEmail(),
                "name", savedUser.getName(),
                "course", savedUser.getCourse(),
                "id", savedUser.getEmail()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        return userRepo.findByEmail(user.getEmail())
            .filter(found -> found.getPassword().equals(user.getPassword()))
            .map(found -> ResponseEntity.ok().body(Map.of(
                "message", "Login successful!",
                "role", found.getRole(),
                "email", found.getEmail(),
                "name", found.getName(),
                "course", found.getCourse()
            )))
            .orElse(ResponseEntity.status(401).body(Map.of("message", "Invalid credentials!")));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(Map.of("count", users.size(), "users", users));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to fetch users: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/users/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        try {
            boolean deleted = userService.deleteUserByEmail(email);
            if (deleted) {
                return ResponseEntity.ok().body(Map.of("message", "User deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found or could not be deleted"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to delete user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userRepo.findByEmail(email)
            .map(user -> ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
                "course", user.getCourse()
            )))
            .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @PostMapping("/testsave")
    public ResponseEntity<?> testSave() {
        User user = new User();
        user.setEmail("test" + System.currentTimeMillis() + "@test.com");
        user.setName("Test");
        user.setPassword("pass");
        user.setRole("organizer");
        user.setCourse("Test Course");
        try {
            userRepo.save(user);
            return ResponseEntity.ok("Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/api/user/profile-pic")
    public ResponseEntity<?> uploadProfilePic(@RequestParam("file") MultipartFile file, Principal principal) {
        // 1. Save the file to disk or cloud storage
        // 2. Update the user's profilePicUrl in the database
        // 3. Return the image URL
        String email = principal.getName();
        String imageUrl = userService.saveProfilePic(email, file); // implement this
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/users/{email}/rsvps")
    public List<RSVP> getUserRsvps(@PathVariable String email) {
        return rsvpRepository.findByStudentEmail(email);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        return userRepo.findByEmail(email)
            .map(user -> ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole()
            )))
            .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @RequestMapping(value = "/signup", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsSignup() {
        return ResponseEntity.ok().build();
    }
}