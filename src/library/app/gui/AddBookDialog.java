package library.app.gui;

import library.app.models.Book;

import javax.swing.*;
import java.awt.*;

public class AddBookDialog extends JDialog {
    private JTextField cipherField;
    private JTextField titleField;
    private JTextField authorField;
    private JTextField yearField;
    private JTextField genreField;
    private Book createdBook;

    public AddBookDialog(JFrame owner) {
        super(owner, "Добавить новую книгу", true);
        setSize(400, 250);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(6, 2, 5, 5));

        add(new JLabel("Шифр:"));
        cipherField = new JTextField();
        add(cipherField);

        add(new JLabel("Название:"));
        titleField = new JTextField();
        add(titleField);

        add(new JLabel("Автор:"));
        authorField = new JTextField();
        add(authorField);

        add(new JLabel("Год:"));
        yearField = new JTextField();
        add(yearField);

        add(new JLabel("Жанр:"));
        genreField = new JTextField();
        add(genreField);

        JButton okButton = new JButton("Добавить");
        JButton cancelButton = new JButton("Отмена");

        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel);
        add(new JLabel());
    }

    private void onOk() {
        try {
            String cipher = cipherField.getText().trim();
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            int year = Integer.parseInt(yearField.getText().trim());
            String genre = genreField.getText().trim();

            if (cipher.isEmpty() || title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Заполните шифр, название и автора!", "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }

            createdBook = new Book(cipher, title, author, year, genre);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Год должен быть числом!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Book getCreatedBook() {
        return createdBook;
    }
}