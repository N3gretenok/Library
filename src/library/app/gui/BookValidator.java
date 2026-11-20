package library.app.gui;

import library.app.exceptions.InvalidDataException;
import library.app.models.Book;
import library.app.models.Reader;

public class BookValidator {

    public static void validateBook(Book book) throws InvalidDataException {
        if (book == null) {
            throw new InvalidDataException("book", "объект книги не может быть null");
        }
        if (book.getCipher() == null || book.getCipher().trim().isEmpty()) {
            throw new InvalidDataException("cipher", "шифр книги не может быть пустым");
        }
        if (book.getCipher().length() < 2) {
            throw new InvalidDataException("cipher", "шифр должен содержать минимум 2 символа");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new InvalidDataException("title", "название книги не может быть пустым");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new InvalidDataException("author", "автор книги не может быть пустым");
        }
        if (book.getYear() < 1000 || book.getYear() > 2100) {
            throw new InvalidDataException("year", "год издания должен быть в диапазоне 1000–2100");
        }
    }

    public static void validateReader(Reader reader) throws InvalidDataException {
        if (reader == null) {
            throw new InvalidDataException("reader", "объект читателя не может быть null");
        }
        if (reader.getStudentCardNumber() == null || reader.getStudentCardNumber().trim().isEmpty()) {
            throw new InvalidDataException("studentCardNumber", "номер билета не может быть пустым");
        }
        if (reader.getStudentCardNumber().length() < 3) {
            throw new InvalidDataException("studentCardNumber", "номер билета должен содержать минимум 3 символа");
        }
        if (reader.getName() == null || reader.getName().trim().isEmpty()) {
            throw new InvalidDataException("name", "имя читателя не может быть пустым");
        }
        if (reader.getSurname() == null || reader.getSurname().trim().isEmpty()) {
            throw new InvalidDataException("surname", "фамилия читателя не может быть пустой");
        }
        if (reader.getName().length() < 2 || reader.getSurname().length() < 2) {
            throw new InvalidDataException("name", "имя и фамилия должны содержать минимум 2 символа");
        }
    }
}