package library.app.models;

import library.app.exceptions.*;
import org.hibernate.Session;
import library.app.gui.BookValidator;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class Library {
    private String name;

    public Library(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void addBook(Book book) throws InvalidDataException {
        BookValidator.validateBook(book);
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            session.persist(book);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new InvalidDataException("cipher", "книга с таким шифром уже существует");
        } finally {
            session.close();
        }
    }

    public void removeBook(String cipher) throws BookNotFoundException, LibraryException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            Book found = session.get(Book.class, cipher);
            if (found == null) {
                tx.rollback();
                throw new BookNotFoundException(cipher);
            }
            if (!found.isAvailable()) {
                tx.rollback();
                throw new LibraryException("Нельзя списать книгу: она сейчас выдана читателю.");
            }
            session.remove(found);
            tx.commit();
        } finally {
            session.close();
        }
    }

    public void issueBook(String cipher, String studentCardNumber)
            throws BookNotFoundException, ReaderNotFoundException,
            BookAlreadyIssuedException, MaxBooksExceededException, LibraryException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            Book book = session.get(Book.class, cipher);
            if (book == null) {
                tx.rollback();
                throw new BookNotFoundException(cipher);
            }

            Query<Reader> q = session.createQuery(
                    "FROM Reader r WHERE r.studentCardNumber = :card", Reader.class);
            q.setParameter("card", studentCardNumber);
            Reader reader = q.uniqueResult();
            if (reader == null) {
                tx.rollback();
                throw new ReaderNotFoundException(studentCardNumber);
            }

            if (!book.isAvailable()) {
                tx.rollback();
                throw new BookAlreadyIssuedException(cipher);
            }

            if (!reader.canBorrowMore()) {
                tx.rollback();
                throw new MaxBooksExceededException(reader.getBorrowedCount(), Reader.MAX_BOOKS);
            }

            book.setAvailable(false);
            book.setReader(reader);
            reader.addBook(book);
            session.merge(book);
            session.merge(reader);
            tx.commit();
        } finally {
            session.close();
        }
    }

    public void returnBook(String cipher, String studentCardNumber)
            throws BookNotFoundException, ReaderNotFoundException, LibraryException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            Book book = session.get(Book.class, cipher);
            if (book == null) {
                tx.rollback();
                throw new BookNotFoundException(cipher);
            }

            Query<Reader> q = session.createQuery(
                    "FROM Reader r WHERE r.studentCardNumber = :card", Reader.class);
            q.setParameter("card", studentCardNumber);
            Reader reader = q.uniqueResult();
            if (reader == null) {
                tx.rollback();
                throw new ReaderNotFoundException(studentCardNumber);
            }

            if (!reader.getBorrowedBooks().contains(book)) {
                tx.rollback();
                throw new LibraryException("У этого читателя нет данной книги.");
            }

            book.setAvailable(true);
            book.setReader(null);
            reader.removeBook(book);
            session.merge(book);
            session.merge(reader);
            tx.commit();
        } finally {
            session.close();
        }
    }

    public void addReader(Reader reader) throws InvalidDataException {
        BookValidator.validateReader(reader);
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            Reader existing = session.get(Reader.class, reader.getStudentCardNumber());
            if (existing != null) {
                tx.rollback();
                throw new InvalidDataException("studentCardNumber",
                        "читатель с номером билета \"" + reader.getStudentCardNumber() + "\" уже существует");
            }
            session.persist(reader);
            tx.commit();
        } finally {
            session.close();
        }
    }

    public void removeReader(String studentCardNumber)
            throws ReaderNotFoundException, LibraryException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            Reader reader = session.get(Reader.class, studentCardNumber);
            if (reader == null) {
                tx.rollback();
                throw new ReaderNotFoundException(studentCardNumber);
            }
            if (!reader.getBorrowedBooks().isEmpty()) {
                tx.rollback();
                throw new LibraryException("Нельзя удалить читателя: у него есть книги на руках ("
                        + reader.getBorrowedCount() + " шт.). Сначала верните все книги.");
            }
            session.remove(reader);
            tx.commit();
        } finally {
            session.close();
        }
    }

    public List<Book> getAllBooks() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("from Book", Book.class).list();
        } finally {
            session.close();
        }
    }

    public List<Reader> getAllReaders() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery(
                    "SELECT DISTINCT r FROM Reader r LEFT JOIN FETCH r.borrowedBooks", Reader.class).list();
        } finally {
            session.close();
        }
    }
}