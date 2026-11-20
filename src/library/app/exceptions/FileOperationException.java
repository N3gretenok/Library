package library.app.exceptions;

public class FileOperationException extends LibraryException {
    public FileOperationException(String operation, String fileName, Throwable cause) {
        super("Ошибка при " + operation + " файла \"" + fileName + "\": " + cause.getMessage(), cause);
    }
}