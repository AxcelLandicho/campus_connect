package com.campusconnect.service;

import java.util.List;
import com.campusconnect.model.RSVP;
import org.springframework.stereotype.Service;

@Service
public class RsvpService {
    public List<RSVP> findByUserEmail(String email) {
        // Return an empty list or implement your logic
        return List.of();
    }
}
