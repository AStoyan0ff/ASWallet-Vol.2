package STARTER.Events;

public record UserRegisteredEvent(
        String email,
        String username
) {}
