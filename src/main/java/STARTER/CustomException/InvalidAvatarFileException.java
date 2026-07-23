package STARTER.CustomException;

public class InvalidAvatarFileException extends RuntimeException {

    public InvalidAvatarFileException(String message) {
        super(message);
    }
}
