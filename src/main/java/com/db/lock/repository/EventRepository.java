package com.db.lock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.db.lock.entity.Event;

import jakarta.persistence.LockModeType;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT e from Event e WHERE e.id = :id")
	Optional<Event> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT e FROM Event e WHERE e.id = :id")
	Optional<EventSnapshot> findSnapshotById(@Param("id") Long id);

	@Modifying
	@Query("UPDATE Event e SET e.availableSeats = :availableSeats WHERE e.id = :id")
	int updateAvailableSeatsPlain(@Param("id") Long id, @Param("availableSeats") int availableSeats);

	// Part of Event (Without VERSION)
	interface EventSnapshot {
		Long getId();

		int getAvailableSeats();
	}
}
