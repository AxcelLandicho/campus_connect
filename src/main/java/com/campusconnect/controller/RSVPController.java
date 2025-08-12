package com.campusconnect.controller;

import com.campusconnect.model.RSVP;
import com.campusconnect.repository.RSVPRepository;
import com.campusconnect.dto.RsvpDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.campusconnect.model.Event;
import com.campusconnect.repository.EventRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Collections;

@RestController
@RequestMapping("/api/events")
public class RSVPController {
    @Autowired
    private EventRepository eventRepo;
    @Autowired
    private RSVPRepository rsvpRepo;

    private List<RsvpDto> getRsvpDtos(Long eventId, Boolean attended, Boolean present) {
        List<RSVP> rsvps = rsvpRepo.findByEventId(eventId);
        if (attended != null) {
            rsvps = rsvps.stream().filter(r -> r.isAttended() == attended).toList();
        }
        if (present != null) {
            rsvps = rsvps.stream().filter(r -> r.isPresent() == present).toList();
        }
        return rsvps.stream().map(rsvp -> {
            RsvpDto dto = new RsvpDto();
            dto.setId(rsvp.getId());
            dto.setStudentEmail(rsvp.getStudentEmail());
            dto.setAttended(rsvp.isAttended());
            dto.setPresent(rsvp.isPresent());
            return dto;
        }).toList();
    }

    @GetMapping("/{eventId}/rsvps")
    public ResponseEntity<?> getRsvps(@PathVariable Long eventId) {
        try {
            List<RsvpDto> rsvps = getRsvpDtos(eventId, null, null);
            return ResponseEntity.ok(rsvps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch RSVPs: " + e.getMessage()));
        }
    }

    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<?> rsvpEvent(@PathVariable Long eventId, @RequestBody RSVP rsvp) {
        try {
            Event event = eventRepo.findById(eventId).orElse(null);
            if (event == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Event not found"));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate eventDate = LocalDate.parse(event.getDate(), formatter);
            if (eventDate.isBefore(LocalDate.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot RSVP to a past event"));
            }

            Optional<RSVP> existingOpt = rsvpRepo.findByEventIdAndStudentEmail(eventId, rsvp.getStudentEmail());
            RSVP toSave;
            if (existingOpt.isPresent()) {
                RSVP existing = existingOpt.get();
                existing.setAttended(rsvp.isAttended());
                existing.setPresent(rsvp.isPresent());
                toSave = existing;
            } else {
                rsvp.setEventId(eventId);
                rsvp.setQrCode(rsvp.getStudentId() + ":" + eventId);
                toSave = rsvp;
            }
            RSVP saved = rsvpRepo.save(toSave);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process RSVP: " + e.getMessage()));
        }
    }

    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<?> getAttendees(
        @PathVariable Long eventId,
        @RequestParam(required = false) Boolean attended,
        @RequestParam(required = false) Boolean present
    ) {
        try {
            List<RsvpDto> dtos = getRsvpDtos(eventId, attended, present);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch attendees: " + e.getMessage()));
        }
    }

    @PostMapping("/{eventId}/scan")
    public ResponseEntity<?> scanQr(@PathVariable Long eventId, @RequestBody RSVP scanRequest) {
        try {
            RSVP rsvp = rsvpRepo.findByEventIdAndQrCode(eventId, scanRequest.getQrCode());
            if (rsvp == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Invalid QR code"));
            }
            rsvp.setPresent(true);
            rsvpRepo.save(rsvp);
            return ResponseEntity.ok(Map.of("message", "Attendance marked successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process QR scan: " + e.getMessage()));
        }
    }

    @GetMapping("/{eventId}/attendees/stats")
    public ResponseEntity<?> getAttendeeStats(@PathVariable Long eventId) {
        try {
            List<RSVP> rsvps = rsvpRepo.findByEventId(eventId);
            long total = rsvps.size();
            long attending = rsvps.stream().filter(RSVP::isAttended).count();
            long present = rsvps.stream().filter(RSVP::isPresent).count();
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("attending", attending);
            stats.put("present", present);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch attendee stats: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rsvps/{rsvpId}")
    public ResponseEntity<?> deleteRsvp(@PathVariable Long rsvpId) {
        try {
            Optional<RSVP> rsvp = rsvpRepo.findById(rsvpId);
            if (rsvp.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RSVP not found"));
            }
            rsvpRepo.deleteById(rsvpId);
            return ResponseEntity.ok(Map.of("message", "RSVP deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete RSVP: " + e.getMessage()));
        }
    }

    @PutMapping("/rsvps/{rsvpId}")
    public ResponseEntity<?> updateRsvp(@PathVariable Long rsvpId, @RequestBody RSVP updatedRsvp) {
        try {
            Optional<RSVP> existingRsvp = rsvpRepo.findById(rsvpId);
            if (existingRsvp.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RSVP not found"));
            }
            RSVP rsvp = existingRsvp.get();
            rsvp.setAttended(updatedRsvp.isAttended());
            rsvp.setPresent(updatedRsvp.isPresent());
            RSVP saved = rsvpRepo.save(rsvp);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update RSVP: " + e.getMessage()));
        }
    }

    @PatchMapping("/{eventId}/rsvps/{rsvpId}/attendance")
    public ResponseEntity<?> updateAttendance(
        @PathVariable Long eventId,
        @PathVariable Long rsvpId,
        @RequestBody Map<String, Boolean> body
    ) {
        try {
            Optional<RSVP> rsvpOpt = rsvpRepo.findById(rsvpId);
            if (rsvpOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RSVP not found"));
            }
            RSVP rsvp = rsvpOpt.get();
            if (!rsvp.getEventId().equals(eventId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Event ID mismatch"));
            }
            Boolean present = body.get("present");
            if (present != null) {
                rsvp.setPresent(present);
                rsvpRepo.save(rsvp);
            }
            return ResponseEntity.ok(Map.of("message", "Attendance updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update attendance: " + e.getMessage()));
        }
    }
}
