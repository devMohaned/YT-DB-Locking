package com.db.lock.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.db.lock.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByEventId(Long eventId);
}
