package com.db.lock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private int capacity;

	private int availableSeats;

	@Version
	private Long version;

	public Event(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
		this.availableSeats = capacity;
	}
}