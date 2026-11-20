package library.app.gui;

import library.app.models.Reader;

import javax.swing.*;
import java.awt.*;

public class AddReaderDialog extends JDialog {
    private JTextField cardNumberField;
    private JTextField nameField;
    private JTextField surnameField;
    private Reader createdReader;

    public AddReaderDialog(JFrame owner) {
        super(owner, "Зарегистрировать читателя", true);
        setSize(400, 220);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(4, 2, 5, 5));

        add(new JLabel("Номер студенческого билета:"));
        cardNumberField = new JTextField();
        add(cardNumberField);

        add(new JLabel("Имя:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Фамилия:"));
        surnameField = new JTextField();
        add(surnameField);

        JButton okButton = new JButton("Зарегистрировать");
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
            String cardNumber = cardNumberField.getText().trim();
            String name = nameField.getText().trim();
            String surname = surnameField.getText().trim();

            if (cardNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Введите номер студенческого билета!",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (name.isEmpty() || surname.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Заполните имя и фамилию!",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }

            createdReader = new Reader(cardNumber, name, surname);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Reader getCreatedReader() {
        return createdReader;
    }
}