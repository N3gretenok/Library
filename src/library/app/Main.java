package library.app;

import library.app.exceptions.*;
import library.app.models.Book;
import library.app.models.Library;
import library.app.models.Reader;
import library.app.models.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Library library = new Library("Городская библиотека №1");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Меню библиотеки ===");
            System.out.println("1. Показать все книги");
            System.out.println("2. Показать всех читателей");
            System.out.println("3. Добавить книгу");
            System.out.println("4. Удалить книгу");
            System.out.println("5. Выдать книгу читателю");
            System.out.println("6. Вернуть книгу");
            System.out.println("7. Добавить читателя");
            System.out.println("8. Удалить читателя");
            System.out.println("0. Выход");
            System.out.print("Выберите пункт: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        showAllBooks(library);
                        break;

                    case "2":
                        showAllReaders(library);
                        break;

                    case "3":
                        addBook(library, scanner);
                        break;

                    case "4":
                        removeBook(library, scanner);
                        break;

                    case "5":
                        issueBook(library, scanner);
                        break;

                    case "6":
                        returnBook(library, scanner);
                        break;

                    case "7":
                        addReader(library, scanner);
                        break;

                    case "8":
                        removeReader(library, scanner);
                        break;

                    case "0":
                        System.out.println("Выход из программы.");
                        HibernateUtil.shutdown();
                        return;

                    default:
                        System.out.println("Неверный пункт меню.");
                }
            } catch (Exception ex) {
                System.out.println("Неожиданная ошибка: " + ex.getMessage());
            }
        }
    }

    private static void showAllBooks(Library library) {
        List<Book> books = library.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("Каталог пуст.");
            return;
        }
        System.out.println("\n--- Каталог книг ---");
        for (Book book : books) {
            System.out.println(book);
        }
    }

    private static void showAllReaders(Library library) {
        List<Reader> readers = library.getAllReaders();
        if (readers.isEmpty()) {
            System.out.println("Список читателей пуст.");
            return;
        }
        System.out.println("\n--- Список читателей ---");
        for (Reader reader : readers) {
            System.out.println(reader);
        }
    }

    private static void addBook(Library library, Scanner scanner) throws InvalidDataException {
        System.out.println("\n--- Добавление книги ---");
        System.out.print("Шифр: ");
        String cipher = scanner.nextLine();
        System.out.print("Название: ");
        String title = scanner.nextLine();
        System.out.print("Автор: ");
        String author = scanner.nextLine();
        System.out.print("Год: ");
        int year = Integer.parseInt(scanner.nextLine());
        System.out.print("Жанр: ");
        String genre = scanner.nextLine();

        Book book = new Book(cipher, title, author, year, genre);
        library.addBook(book);
        System.out.println("Книга успешно добавлена.");
    }

    private static void removeBook(Library library, Scanner scanner) throws BookNotFoundException, LibraryException {
        System.out.println("\n--- Удаление книги ---");
        System.out.print("Шифр книги: ");
        String cipher = scanner.nextLine();
        library.removeBook(cipher);
        System.out.println("Книга удалена.");
    }

    private static void issueBook(Library library, Scanner scanner)
            throws BookNotFoundException, ReaderNotFoundException,
            BookAlreadyIssuedException, MaxBooksExceededException, LibraryException {
        System.out.println("\n--- Выдача книги ---");
        System.out.print("Шифр книги: ");
        String cipher = scanner.nextLine();
        System.out.print("Номер студенческого билета читателя: ");
        String cardNumber = scanner.nextLine();

        library.issueBook(cipher, cardNumber);
        System.out.println("Книга выдана.");
    }

    private static void returnBook(Library library, Scanner scanner)
            throws BookNotFoundException, ReaderNotFoundException, LibraryException {
        System.out.println("\n--- Возврат книги ---");
        System.out.print("Шифр книги: ");
        String cipher = scanner.nextLine();
        System.out.print("Номер студенческого билета читателя: ");
        String cardNumber = scanner.nextLine();

        library.returnBook(cipher, cardNumber);
        System.out.println("Книга возвращена.");
    }

    private static void addReader(Library library, Scanner scanner) throws InvalidDataException {
        System.out.println("\n--- Регистрация читателя ---");
        System.out.print("Номер студенческого билета: ");
        String cardNumber = scanner.nextLine();
        System.out.print("Имя: ");
        String name = scanner.nextLine();
        System.out.print("Фамилия: ");
        String surname = scanner.nextLine();

        library.addReader(new Reader(cardNumber, name, surname));
        System.out.println("Читатель зарегистрирован.");
    }

    private static void removeReader(Library library, Scanner scanner)
            throws ReaderNotFoundException, LibraryException {
        System.out.println("\n--- Удаление читателя ---");
        System.out.print("Номер студенческого билета: ");
        String cardNumber = scanner.nextLine();

        library.removeReader(cardNumber);
        System.out.println("Читатель удалён.");
    }
}