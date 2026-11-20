package library.app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @Column(name = "cipher", unique = true, nullable = false)
    private String cipher;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author", nullable = false)
    private String author;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "genre")
    private String genre;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id")
    private Reader reader;

    public Book() {}

    public Book(String cipher, String title, String author, int year, String genre) {
        setCipher(cipher);
        setTitle(title);
        setAuthor(author);
        setYear(year);
        setGenre(genre);
        this.available = true;
    }

    public String getCipher() { return cipher; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public String getGenre() { return genre; }
    public boolean isAvailable() { return available; }
    public Reader getReader() { return reader; }

    public void setCipher(String cipher) { this.cipher = cipher; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setYear(int year) { this.year = year; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setReader(Reader reader) { this.reader = reader; }

    @Override
    public String toString() {
        return "Шифр: " + cipher + " | \"" + title + "\" — " + author +
                " (" + year + ", " + genre + ")" + (available ? " [свободна]" : " [выдана]");
    }
}