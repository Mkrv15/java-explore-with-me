package ru.practicum.ewm.error.model;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}