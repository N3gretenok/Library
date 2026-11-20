package library.app.exceptions;

public class MaxBooksExceededException extends LibraryException {
    private final int maxBooks;
    private final int currentCount;

    public MaxBooksExceededException(int currentCount, int maxBooks) {
        super("Читатель уже взял " + currentCount + " книг из максимальных " + maxBooks + ".");
        this.currentCount = currentCount;
        this.maxBooks = maxBooks;
    }

    public int getMaxBooks() { return maxBooks; }
    public int getCurrentCount() { return currentCount; }
}