package STARTER.CustomException;

// Advanced — invalid avatar upload
public class InvalidAvatarFileException extends RuntimeException {

    public InvalidAvatarFileException(String message) {
        super(message);
    }
}
