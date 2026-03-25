package com.db.lock.service;

import static com.db.lock.util.Constants.SLEEP_TIME_MS;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.db.lock.entity.Booking;
import com.db.lock.entity.Event;
import com.db.lock.repository.BookingRepository;
import com.db.lock.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

	public enum BookingMode {
		PLAIN, OPTIMISTIC, PESSIMISTIC, SERIALIZABLE
	}

	private final EventRepository eventRepository;
	private final BookingRepository bookingRepository;


    @Transactional
	public BookingResult bookPlain(Long eventId, String userId) {
		log.info("[PLAIN] user={} event={} | Starting booking flow", userId, eventId);

        EventRepository.EventSnapshot snapshot = eventRepository.findSnapshotById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

		log.info("[PLAIN] user={} event={} | Read event snapshot | seats={}", userId, eventId,
				snapshot.getAvailableSeats());

		if (snapshot.getAvailableSeats() <= 0) {
			log.warn("[PLAIN] user={} event={} | Booking rejected | reason=No seats left", userId, eventId);
			throw new IllegalStateException("No seats left");
		}

		log.info("[PLAIN] user={} event={} | Seat looks available | waiting={}ms", userId, eventId, SLEEP_TIME_MS);

		artificialDelay("PLAIN", eventId, userId);

		int newAvailableSeats = snapshot.getAvailableSeats() - 1;

		log.info("[PLAIN] user={} event={} | Writing new seat count | old={} new={}", userId, eventId,
				snapshot.getAvailableSeats(), newAvailableSeats);

		eventRepository.updateAvailableSeatsPlain(eventId, newAvailableSeats);

		Event eventRef = eventRepository.getReferenceById(eventId);
		bookingRepository.save(new Booking(userId, eventRef));

		log.info("[PLAIN] user={} event={} | Booking saved successfully", userId, eventId);

		return new BookingResult("SUCCESS", "Booked with PLAIN mode", eventId, userId);
	}

	@Transactional
	public BookingResult bookOptimistic(Long eventId, String userId) {
		log.info("[OPTIMISTIC] user={} event={} | Starting booking flow", userId, eventId);

		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

		log.info("[OPTIMISTIC] user={} event={} | Read event | seats={} version={}", userId, eventId,
				event.getAvailableSeats(), event.getVersion());

		if (event.getAvailableSeats() <= 0) {
			log.warn("[OPTIMISTIC] user={} event={} | Booking rejected | reason=No seats left", userId, eventId);
			throw new IllegalStateException("No seats left");
		}

		log.info("[OPTIMISTIC] user={} event={} | Seat looks available | waiting={}ms", userId, eventId, SLEEP_TIME_MS);

		artificialDelay("OPTIMISTIC", eventId, userId);

		int oldSeats = event.getAvailableSeats();
		Long oldVersion = event.getVersion();

		event.setAvailableSeats(oldSeats - 1);

		log.info("[OPTIMISTIC] user={} event={} | Trying to update event | oldSeats={} newSeats={} expectedVersion={}",
				userId, eventId, oldSeats, event.getAvailableSeats(), oldVersion);

        Event savedEvent = eventRepository.saveAndFlush(event);

        log.info("[OPTIMISTIC] user={} event={} | Event updated successfully | newVersion={}", userId, eventId,
                savedEvent.getVersion());

		bookingRepository.save(new Booking(userId, event));

		log.info("[OPTIMISTIC] user={} event={} | Booking saved successfully", userId, eventId);

		return new BookingResult("SUCCESS", "Booked with OPTIMISTIC mode", eventId, userId);
	}

	@Transactional
	public BookingResult bookPessimistic(Long eventId, String userId) {
		log.info("[PESSIMISTIC] user={} event={} | Starting booking flow", userId, eventId);
		log.info("[PESSIMISTIC] user={} event={} | Waiting to acquire row lock", userId, eventId);

		Event event = eventRepository.findByIdForUpdate(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

		log.info("[PESSIMISTIC] user={} event={} | Row lock acquired | seats={}", userId, eventId,
				event.getAvailableSeats());

		if (event.getAvailableSeats() <= 0) {
			log.warn("[PESSIMISTIC] user={} event={} | Booking rejected after lock | reason=No seats left", userId,
					eventId);
			throw new IllegalStateException("No seats left");
		}

		log.info("[PESSIMISTIC] user={} event={} | Seat available after lock | waiting={}ms", userId, eventId,
				SLEEP_TIME_MS);

		artificialDelay("PESSIMISTIC", eventId, userId);

		int oldSeats = event.getAvailableSeats();
		event.setAvailableSeats(oldSeats - 1);

		log.info("[PESSIMISTIC] user={} event={} | Updating event | oldSeats={} newSeats={}", userId, eventId, oldSeats,
				event.getAvailableSeats());

		bookingRepository.save(new Booking(userId, event));

		log.info("[PESSIMISTIC] user={} event={} | Booking saved successfully", userId, eventId);
		log.info("[PESSIMISTIC] user={} event={} | Transaction will now release the lock", userId, eventId);

		return new BookingResult("SUCCESS", "Booked with PESSIMISTIC mode", eventId, userId);
	}

	private void artificialDelay(String mode, Long eventId, String userId) {
		try {
			log.info("[{}] user={} event={} | Sleeping {} ms to simulate race condition", mode, userId, eventId,
					SLEEP_TIME_MS);
			Thread.sleep(SLEEP_TIME_MS);
			log.info("[{}] user={} event={} | Woke up and continuing", mode, userId, eventId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[{}] user={} event={} | Sleep interrupted", mode, userId, eventId, e);
			throw new RuntimeException("Thread interrupted", e);
		}
	}

	public record BookingResult(String status, String message, Long eventId, String userId) {
	}
}