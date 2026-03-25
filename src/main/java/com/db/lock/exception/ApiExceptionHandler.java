package com.db.lock.exception;

import java.util.Map;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, Object> handleNotFound(IllegalArgumentException ex) {
		log.warn("Booking illegal argument exception | {}", ex.getMessage());
		return Map.of("status", 404, "error", ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, Object> handleConflict(IllegalStateException ex) {
		log.warn("Booking failed | {}", ex.getMessage());
		return Map.of("status", 409, "error", ex.getMessage());
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, Object> handleOptimistic(OptimisticLockingFailureException ex) {
		log.warn("[OPTIMISTIC] Conflict detected | Another request updated the event first");
		return Map.of("status", 409, "error", "Concurrent update detected in OPTIMISTIC mode. Please retry.");
	}

	@ExceptionHandler(ConcurrencyFailureException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, Object> handleSerializable(ConcurrencyFailureException ex) {
		log.warn("[SERIALIZABLE] Conflict detected | PostgreSQL aborted one transaction to keep execution serial");
		return Map.of("status", 409, "error", "Serializable transaction conflict detected. Retry the request.");
	}
}