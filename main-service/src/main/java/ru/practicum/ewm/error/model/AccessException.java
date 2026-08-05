package ru.practicum.ewm.error.model;

public class AccessException extends RuntimeException {
    public AccessException(String message) {
        super(message);
    }
}
