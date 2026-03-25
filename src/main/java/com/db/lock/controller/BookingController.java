package com.db.lock.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.db.lock.entity.Booking;
import com.db.lock.entity.Event;
import com.db.lock.repository.BookingRepository;
import com.db.lock.repository.EventRepository;
import com.db.lock.service.BookingService;
import com.db.lock.service.SerializableBookingService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

	private final EventRepository eventRepository;
	private final BookingRepository bookingRepository;
	private final BookingService bookingService;
    private final SerializableBookingService serializableBookingService;

	@PostMapping("/events")
	public Event createEvent(@RequestBody CreateEventRequest request) {
		Event event = new Event(request.name(), request.capacity());
		return eventRepository.save(event);
	}

	@GetMapping("/events/{eventId}")
	public Event getEvent(@PathVariable Long eventId) {
		return eventRepository.findById(eventId)
				.orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
	}

	@GetMapping("/events/{eventId}/bookings")
	public List<Booking> getBookings(@PathVariable Long eventId) {
		return bookingRepository.findByEventId(eventId);
	}

	@PostMapping("/events/{eventId}/book")
	public BookingService.BookingResult book(@PathVariable Long eventId, @RequestParam String userId,
			@RequestParam BookingService.BookingMode mode) {
		log.info("[{}] user={} event={} | Booking request received", mode, userId, eventId);

		return switch (mode) {
		case PLAIN -> bookingService.bookPlain(eventId, userId);
		case OPTIMISTIC -> bookingService.bookOptimistic(eventId, userId);
		case PESSIMISTIC -> bookingService.bookPessimistic(eventId, userId);
		case SERIALIZABLE -> serializableBookingService.bookSerializable(eventId, userId);
        };

	}

	public record CreateEventRequest(@NotBlank String name, @Min(1) int capacity) {
	}


}