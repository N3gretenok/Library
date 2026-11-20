package library.app.exceptions;

public class BookAlreadyIssuedException extends LibraryException {
    public BookAlreadyIssuedException(String cipher) {
        super("Книга с шифром \"" + cipher + "\" уже выдана другому читателю.");
    }
}