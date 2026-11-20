package library.app.exceptions;

public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String cipher) {
        super("Книга с шифром \"" + cipher + "\" не найдена в каталоге.");
    }
}