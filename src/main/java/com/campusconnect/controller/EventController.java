package com.campusconnect.controller;

import com.campusconnect.model.Event;
import com.campusconnect.model.RSVP;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RSVPRepository;
import com.campusconnect.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepo;
    private final RSVPRepository rsvpRepo;
    private final UserRepository userRepo;

    @Autowired
    public EventController(EventRepository eventRepo, RSVPRepository rsvpRepo, UserRepository userRepo) {
        this.eventRepo = eventRepo;
        this.rsvpRepo = rsvpRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<?> getEvents() {
        try {
            List<Event> events = eventRepo.findAll();
            List<Map<String, Object>> eventsWithRSVPs = events.stream()
                .map(event -> {
                    int rsvpCount = rsvpRepo.countByEventId(event.getId());
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", event.getId());
                    map.put("name", event.getName());
                    map.put("description", event.getDescription());
                    map.put("date", event.getDate());
                    map.put("startTime", event.getStartTime());
                    map.put("endTime", event.getEndTime());
                    map.put("venue", event.getVenue());
                    map.put("organization", event.getOrganization());
                    map.put("rsvpCount", rsvpCount);
                    return map;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(eventsWithRSVPs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch events: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable Long id) {
        try {
            Optional<Event> event = eventRepo.findById(id);
            if (event.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Event not found"));
            }
            Event foundEvent = event.get();
            List<RSVP> rsvps = rsvpRepo.findByEventId(id);
            int rsvpCount = rsvps.size();
            
            Map<String, Object> eventWithRSVPs = Map.of(
                "id", foundEvent.getId(),
                "name", foundEvent.getName(),
                "description", foundEvent.getDescription(),
                "date", foundEvent.getDate(),
                "startTime", foundEvent.getStartTime(),
                "endTime", foundEvent.getEndTime(),
                "venue", foundEvent.getVenue(),
                "organization", foundEvent.getOrganization(),
                "rsvpCount", rsvpCount,
                "attendees", rsvps.stream()
                    .filter(Objects::nonNull)
                    .map(rsvp -> Map.of(
                        "id", rsvp.getId(),
                        "studentEmail", rsvp.getStudentEmail() != null ? rsvp.getStudentEmail() : "",
                        "qrCode", rsvp.getQrCode() != null ? rsvp.getQrCode() : "",
                        "status", rsvp.getStatus() != null ? rsvp.getStatus() : "",
                        "timestamp", rsvp.getTimestamp() != null ? rsvp.getTimestamp() : ""
                    ))
                    .collect(Collectors.toList())
            );
            return ResponseEntity.ok(eventWithRSVPs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch event: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Event event) {
        try {
            Event saved = eventRepo.save(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create event: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        try {
            Optional<Event> existingEvent = eventRepo.findById(id);
            if (existingEvent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Event not found"));
            }
            event.setId(id);
            Event saved = eventRepo.save(event);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update event: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            Optional<Event> event = eventRepo.findById(id);
            if (event.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Event not found"));
            }
            eventRepo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete event: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{eventId}/rsvps/{rsvpId}")
    public ResponseEntity<?> deleteRSVP(@PathVariable Long eventId, @PathVariable Long rsvpId) {
        try {
            Optional<RSVP> rsvp = rsvpRepo.findById(rsvpId);
            if (rsvp.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RSVP not found"));
            }
            
            // Verify the RSVP belongs to the specified event
            if (!rsvp.get().getEventId().equals(eventId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "RSVP does not belong to the specified event"));
            }
            
            rsvpRepo.deleteById(rsvpId);
            return ResponseEntity.ok(Map.of("message", "RSVP deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete RSVP: " + e.getMessage()));
        }
    }

    private String getStudentCourse(String email) {
        return userRepo.findByEmail(email)
            .map(user -> user.getCourse() != null ? user.getCourse() : "")
            .orElse("");
    }
}