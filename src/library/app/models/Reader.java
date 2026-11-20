package library.app.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "readers")
public class Reader {
    public static final int MAX_BOOKS = 5;

    @Id
    @Column(name = "student_card_number", unique = true, nullable = false)
    private String studentCardNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @OneToMany(mappedBy = "reader", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Book> borrowedBooks = new ArrayList<>();

    public Reader() {}

    public Reader(String studentCardNumber, String name, String surname) {
        this.studentCardNumber = studentCardNumber;
        this.name = name;
        this.surname = surname;
    }

    public String getStudentCardNumber() { return studentCardNumber; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public List<Book> getBorrowedBooks() { return borrowedBooks; }
    public int getBorrowedCount() { return borrowedBooks.size(); }
    public boolean canBorrowMore() { return borrowedBooks.size() < MAX_BOOKS; }

    public void setStudentCardNumber(String studentCardNumber) { this.studentCardNumber = studentCardNumber; }
    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }

    public void addBook(Book book) { borrowedBooks.add(book); }
    public void removeBook(Book book) { borrowedBooks.remove(book); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Читатель ").append(studentCardNumber).append(": ").append(surname).append(" ").append(name)
                .append(" (Взято книг: ").append(getBorrowedCount()).append("/").append(MAX_BOOKS).append(")\n");
        if (!borrowedBooks.isEmpty()) {
            for (Book b : borrowedBooks) {
                sb.append("    - ").append(b.getTitle()).append(" (").append(b.getAuthor()).append(")\n");
            }
        } else {
            sb.append("    (нет выданных книг)\n");
        }
        return sb.toString();
    }
}