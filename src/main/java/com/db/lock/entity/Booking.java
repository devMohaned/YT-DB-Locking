package com.db.lock.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userId;

	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id")
	private Event event;

	public Booking(String userId, Event event) {
		this.userId = userId;
		this.event = event;
		this.createdAt = LocalDateTime.now();
	}
}
