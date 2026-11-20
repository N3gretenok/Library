package library.app.exceptions;

public class InvalidDataException extends LibraryException {
    private final String fieldName;

    public InvalidDataException(String fieldName, String reason) {
        super("Поле \"" + fieldName + "\": " + reason);
        this.fieldName = fieldName;
    }

    public String getFieldName() { return fieldName; }
}