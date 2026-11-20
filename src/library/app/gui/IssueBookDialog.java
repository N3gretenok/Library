package library.app.gui;

import library.app.exceptions.*;
import library.app.models.Library;

import javax.swing.*;
import java.awt.*;

public class IssueBookDialog extends JDialog {
    private JTextField cipherField;
    private JTextField cardNumberField;
    private boolean actionCompleted = false;
    private Library library;

    public IssueBookDialog(JFrame owner, Library library) {
        super(owner, "Выдать книгу читателю", true);
        this.library = library;
        setSize(400, 200);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Шифр книги:"));
        cipherField = new JTextField();
        add(cipherField);

        add(new JLabel("Номер студенческого билета:"));
        cardNumberField = new JTextField();
        add(cardNumberField);

        JButton okButton = new JButton("Выдать");
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
        String cipher = cipherField.getText().trim();
        String cardNumber = cardNumberField.getText().trim();

        if (cipher.isEmpty() || cardNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните оба поля!",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            library.issueBook(cipher, cardNumber);
            actionCompleted = true;
            JOptionPane.showMessageDialog(this, "Книга успешно выдана!",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (BookNotFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Книга не найдена", JOptionPane.ERROR_MESSAGE);
        } catch (ReaderNotFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Читатель не найден", JOptionPane.ERROR_MESSAGE);
        } catch (BookAlreadyIssuedException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Книга недоступна", JOptionPane.WARNING_MESSAGE);
        } catch (MaxBooksExceededException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Лимит книг превышен", JOptionPane.WARNING_MESSAGE);
        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка операции", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Неожиданная ошибка: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isActionCompleted() {
        return actionCompleted;
    }
}