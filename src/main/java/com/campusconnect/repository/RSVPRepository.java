package com.campusconnect.repository;

import com.campusconnect.model.RSVP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface RSVPRepository extends JpaRepository<RSVP, Long> {
    Optional<RSVP> findByEventIdAndStudentEmail(Long eventId, String studentEmail);
    List<RSVP> findByEventId(Long eventId);
    RSVP findByEventIdAndQrCode(Long eventId, String qrCode);
    List<RSVP> findByStudentEmail(String email);
    int countByEventId(Long eventId);
}
