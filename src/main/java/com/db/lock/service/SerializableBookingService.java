package com.db.lock.service;

import static com.db.lock.util.Constants.SLEEP_TIME_MS;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
public class SerializableBookingService {

	private final EventRepository eventRepository;
	private final BookingRepository bookingRepository;

	@Transactional(isolation = Isolation.SERIALIZABLE)
	public BookingService.BookingResult bookSerializable(Long eventId, String userId) {
		log.info("[SERIALIZABLE] user={} event={} | Starting booking flow", userId, eventId);

		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

		log.info("[SERIALIZABLE] user={} event={} | Read event | seats={}", userId, eventId, event.getAvailableSeats());

		if (event.getAvailableSeats() <= 0) {
			log.warn("[SERIALIZABLE] user={} event={} | Booking rejected | reason=No seats left", userId, eventId);
			throw new IllegalStateException("No seats left");
		}

		log.info("[SERIALIZABLE] user={} event={} | Seat looks available | waiting={}ms", userId, eventId,
				SLEEP_TIME_MS);

		artificialDelay(eventId, userId);

		int oldSeats = event.getAvailableSeats();
		event.setAvailableSeats(oldSeats - 1);

		log.info("[SERIALIZABLE] user={} event={} | Trying to update event | oldSeats={} newSeats={}", userId, eventId,
				oldSeats, event.getAvailableSeats());

		bookingRepository.save(new Booking(userId, event));

		log.info("[SERIALIZABLE] user={} event={} | Booking saved successfully", userId, eventId);

		return new BookingService.BookingResult("SUCCESS", "Booked with SERIALIZABLE mode", eventId, userId);
	}

	private void artificialDelay(Long eventId, String userId) {
		try {
			log.info("[SERIALIZABLE] user={} event={} | Sleeping {} ms to simulate race condition", userId, eventId,
					SLEEP_TIME_MS);
			Thread.sleep(SLEEP_TIME_MS);
			log.info("[SERIALIZABLE] user={} event={} | Woke up and continuing", userId, eventId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[SERIALIZABLE] user={} event={} | Sleep interrupted", userId, eventId, e);
			throw new RuntimeException("Thread interrupted", e);
		}
	}
}