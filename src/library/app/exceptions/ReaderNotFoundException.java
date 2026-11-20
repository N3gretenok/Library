package library.app.exceptions;

public class ReaderNotFoundException extends LibraryException {
    public ReaderNotFoundException(String studentCardNumber) {
        super("Читатель с номером билета \"" + studentCardNumber + "\" не найден.");
    }
    public ReaderNotFoundException(int id) {
        super("Читатель с идентификатором " + id + " не найден.");
    }
}