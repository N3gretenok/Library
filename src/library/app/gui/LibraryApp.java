package library.app.gui;

import library.app.exceptions.*;
import library.app.models.Book;
import library.app.models.Library;
import library.app.models.Reader;
import library.app.models.HibernateUtil;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.io.*;

public class LibraryApp {
    private JFrame frame;
    private DefaultTableModel bookTableModel;
    private DefaultTableModel readerTableModel;
    private JTable bookTable;
    private JTable readerTable;
    private JTextField searchField;
    private Library library;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private int completedWorkers = 0;
    private ExecutorService executorService;
    private SwingWorker<?, ?> currentWorker;
    private JButton cancelButton;
    private AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public LibraryApp() {
        this.library = new Library("Городская библиотека №1");
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public void show() {
        frame = new JFrame("Управление библиотекой");
        frame.setSize(1200, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    executorService.shutdownNow();
                }
                HibernateUtil.shutdown();
                System.exit(0);
            }
        });

        createToolBar();
        createTables();
        createBottomPanel();

        refreshBookTable();
        refreshReaderTable();

        frame.setVisible(true);
    }

    private void setStatus(String message, boolean showProgress) {
        statusLabel.setText(message);
        progressBar.setVisible(showProgress);
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar("Панель инструментов");
        toolBar.setFloatable(false);

        JButton addButton = new JButton("Добавить книгу");
        JButton deleteButton = new JButton("Удалить книгу");
        JButton issueButton = new JButton("Выдать читателю");
        JButton returnButton = new JButton("Вернуть книгу");
        JButton addReaderButton = new JButton("Добавить читателя");
        JButton removeReaderButton = new JButton("Удалить читателя");
        JButton saveButton = new JButton("Сохранить в файл");
        JButton loadButton = new JButton("Загрузить из файла");
        JButton reportButton = new JButton("Сформировать отчёт");
        JButton parallelButton = new JButton("Параллельная загрузка");
        cancelButton = new JButton("Отменить операцию");
        cancelButton.setEnabled(false);

        toolBar.addSeparator();
        toolBar.add(cancelButton);
        toolBar.addSeparator();
        toolBar.add(reportButton);
        toolBar.add(addButton);
        toolBar.add(deleteButton);
        toolBar.addSeparator();
        toolBar.add(issueButton);
        toolBar.add(returnButton);
        toolBar.addSeparator();
        toolBar.add(addReaderButton);
        toolBar.add(removeReaderButton);
        toolBar.addSeparator();
        toolBar.add(saveButton);
        toolBar.add(loadButton);
        toolBar.addSeparator();
        toolBar.add(parallelButton);

        reportButton.addActionListener(e -> onGenerateReport());
        addButton.addActionListener(e -> onAddBook());
        deleteButton.addActionListener(e -> onDeleteBook());
        issueButton.addActionListener(e -> onIssueBook());
        returnButton.addActionListener(e -> onReturnBook());
        addReaderButton.addActionListener(e -> onAddReader());
        removeReaderButton.addActionListener(e -> onRemoveReader());
        saveButton.addActionListener(e -> onSaveToFile());
        loadButton.addActionListener(e -> onLoadFromFile());
        cancelButton.addActionListener(e -> {
            if (currentWorker != null && !currentWorker.isDone()) {
                currentWorker.cancel(true);
                cancelRequested.set(true);
                setStatus("Запрос на отмену...", false);
                cancelButton.setEnabled(false);
            }
        });
        parallelButton.addActionListener(e -> onParallelLoad());

        frame.add(toolBar, BorderLayout.NORTH);
    }

    private void createTables() {
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        String[] bookColumns = {"Шифр", "Название", "Автор", "Год", "Жанр", "Статус"};
        bookTableModel = new DefaultTableModel(bookColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(bookTableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane bookScroll = new JScrollPane(bookTable);

        String[] readerColumns = {"Номер билета", "Имя", "Фамилия", "Книг на руках"};
        readerTableModel = new DefaultTableModel(readerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        readerTable = new JTable(readerTableModel);
        readerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane readerScroll = new JScrollPane(readerTable);

        tablesPanel.add(bookScroll);
        tablesPanel.add(readerScroll);

        frame.add(tablesPanel, BorderLayout.CENTER);
    }

    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Поиск по названию или автору:"));
        searchField = new JTextField(20);
        searchPanel.add(searchField);

        JButton searchButton = new JButton("Найти");
        JButton resetButton = new JButton("Сброс");
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        searchButton.addActionListener(e -> onSearch());
        resetButton.addActionListener(e -> {
            searchField.setText("");
            refreshBookTable();
        });

        bottomPanel.add(searchPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusLabel = new JLabel("Готово");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusPanel.add(statusLabel);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(150, 16));
        progressBar.setVisible(false);
        statusPanel.add(progressBar);

        bottomPanel.add(statusPanel, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void onAddBook() {
        AddBookDialog dialog = new AddBookDialog(frame);
        dialog.setVisible(true);

        Book book = dialog.getCreatedBook();
        if (book != null) {
            try {
                library.addBook(book);
                refreshBookTable();
                JOptionPane.showMessageDialog(frame, "Книга успешно добавлена!",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (InvalidDataException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Ошибка валидации: " + ex.getMessage(),
                        "Некорректные данные", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Неожиданная ошибка: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onDeleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame,
                    "Пожалуйста, выберите книгу в таблице для удаления.",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cipher = (String) bookTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Вы уверены, что хотите списать книгу с шифром " + cipher + "?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                library.removeBook(cipher);
                refreshBookTable();
                JOptionPane.showMessageDialog(frame, "Книга успешно удалена.",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (BookNotFoundException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(),
                        "Книга не найдена", JOptionPane.WARNING_MESSAGE);
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(),
                        "Ошибка операции", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Неожиданная ошибка: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onIssueBook() {
        IssueBookDialog dialog = new IssueBookDialog(frame, library);
        dialog.setVisible(true);

        if (dialog.isActionCompleted()) {
            refreshBookTable();
            refreshReaderTable();
        }
    }

    private void onReturnBook() {
        String cipher = JOptionPane.showInputDialog(frame, "Введите шифр книги для возврата:");
        if (cipher == null || cipher.trim().isEmpty()) return;

        String cardNumber = JOptionPane.showInputDialog(frame, "Введите номер студенческого билета читателя:");
        if (cardNumber == null || cardNumber.trim().isEmpty()) return;

        try {
            library.returnBook(cipher.trim(), cardNumber.trim());
            refreshBookTable();
            refreshReaderTable();
            JOptionPane.showMessageDialog(frame, "Книга успешно возвращена.",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (BookNotFoundException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Книга не найдена", JOptionPane.WARNING_MESSAGE);
        } catch (ReaderNotFoundException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Читатель не найден", JOptionPane.WARNING_MESSAGE);
        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Ошибка операции", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Неожиданная ошибка: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAddReader() {
        AddReaderDialog dialog = new AddReaderDialog(frame);
        dialog.setVisible(true);

        Reader reader = dialog.getCreatedReader();
        if (reader != null) {
            try {
                library.addReader(reader);
                refreshReaderTable();
                JOptionPane.showMessageDialog(frame, "Читатель успешно зарегистрирован!",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (InvalidDataException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Ошибка валидации: " + ex.getMessage(),
                        "Некорректные данные", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Неожиданная ошибка: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onParallelLoad() {
        setStatus("Параллельная загрузка данных...", true);
        progressBar.setIndeterminate(true);
        completedWorkers = 0;

        SwingWorker<List<Book>, Void> booksWorker = new SwingWorker<>() {
            @Override
            protected List<Book> doInBackground() {
                return library.getAllBooks();
            }

            @Override
            protected void done() {
                try {
                    bookTableModel.setRowCount(0);
                    for (Book book : get()) {
                        bookTableModel.addRow(new Object[]{
                                book.getCipher(), book.getTitle(), book.getAuthor(),
                                book.getYear(), book.getGenre(),
                                book.isAvailable() ? "Свободна" : "Выдана"
                        });
                    }
                    checkAllWorkersDone();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        SwingWorker<List<Reader>, Void> readersWorker = new SwingWorker<>() {
            @Override
            protected List<Reader> doInBackground() {
                return library.getAllReaders();
            }

            @Override
            protected void done() {
                try {
                    readerTableModel.setRowCount(0);
                    for (Reader reader : get()) {
                        readerTableModel.addRow(new Object[]{
                                reader.getStudentCardNumber(),
                                reader.getName(),
                                reader.getSurname(),
                                reader.getBorrowedCount()
                        });
                    }
                    checkAllWorkersDone();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        executorService.execute(booksWorker);
        executorService.execute(readersWorker);
    }

    private synchronized void checkAllWorkersDone() {
        completedWorkers++;
        if (completedWorkers >= 2) {
            completedWorkers = 0;
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
            setStatus("Параллельная загрузка завершена", false);
        }
    }

    private void onRemoveReader() {
        int selectedRow = readerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame,
                    "Пожалуйста, выберите читателя в таблице для удаления.",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cardNumber = (String) readerTableModel.getValueAt(selectedRow, 0);
        String readerName = readerTableModel.getValueAt(selectedRow, 1) + " " +
                readerTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Вы уверены, что хотите удалить читателя " + readerName + "?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                library.removeReader(cardNumber);
                refreshReaderTable();
                refreshBookTable();
                JOptionPane.showMessageDialog(frame, "Читатель успешно удалён.",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (ReaderNotFoundException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(),
                        "Читатель не найден", JOptionPane.WARNING_MESSAGE);
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(),
                        "Ошибка операции", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Неожиданная ошибка: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSaveToFile() {
        String[] dataTypes = {"Только книги", "Только читатели", "Всё вместе"};
        String dataType = (String) JOptionPane.showInputDialog(
                frame, "Что сохранить в XML-файл?", "Тип данных",
                JOptionPane.QUESTION_MESSAGE, null, dataTypes, dataTypes[2]);
        if (dataType == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранение данных в XML-файл");
        fileChooser.setSelectedFile(new File("library_data.xml"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("XML файлы", "xml"));

        int result = fileChooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;
        String fileName = fileChooser.getSelectedFile().getAbsolutePath();

        setStatus("Сохранение в файл...", true);
        progressBar.setIndeterminate(true);
        cancelButton.setEnabled(true);

        currentWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element rootElement = doc.createElement("library");
                doc.appendChild(rootElement);

                List<Book> books = library.getAllBooks();
                List<Reader> readers = library.getAllReaders();
                publish("Загружено книг: " + books.size());
                publish("Загружено читателей: " + readers.size());

                if ("Только книги".equals(dataType) || "Всё вместе".equals(dataType)) {
                    Element booksElement = doc.createElement("books");
                    rootElement.appendChild(booksElement);
                    for (int i = 0; i < books.size(); i++) {
                        if (cancelRequested.get()) {
                            publish("Отменено пользователем");
                            return null;
                        }
                        Book book = books.get(i);
                        Element bookElement = doc.createElement("book");
                        bookElement.setAttribute("cipher", book.getCipher());
                        bookElement.setAttribute("available",
                                String.valueOf(book.isAvailable()));

                        bookElement.appendChild(createTextElement(doc, "title", book.getTitle()));
                        bookElement.appendChild(createTextElement(doc, "author", book.getAuthor()));
                        bookElement.appendChild(createTextElement(doc, "year",
                                String.valueOf(book.getYear())));
                        bookElement.appendChild(createTextElement(doc, "genre",
                                book.getGenre() != null ? book.getGenre() : ""));
                        booksElement.appendChild(bookElement);

                        publish("Сохранена книга " + (i + 1) + " из " + books.size());
                        Thread.sleep(50);
                    }
                }

                if ("Только читатели".equals(dataType) || "Всё вместе".equals(dataType)) {
                    Element readersElement = doc.createElement("readers");
                    rootElement.appendChild(readersElement);
                    for (int i = 0; i < readers.size(); i++) {
                        if (cancelRequested.get()) {
                            publish("Отменено пользователем");
                            return null;
                        }
                        Reader reader = readers.get(i);
                        Element readerElement = doc.createElement("reader");
                        readerElement.setAttribute("studentCardNumber",
                                reader.getStudentCardNumber());

                        readerElement.appendChild(createTextElement(doc, "name", reader.getName()));
                        readerElement.appendChild(createTextElement(doc, "surname", reader.getSurname()));
                        readerElement.appendChild(createTextElement(doc, "borrowedCount",
                                String.valueOf(reader.getBorrowedCount())));
                        readersElement.appendChild(readerElement);

                        publish("Сохранён читатель " + (i + 1) + " из " + readers.size());
                        Thread.sleep(50);
                    }
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                DOMSource source = new DOMSource(doc);
                StreamResult streamResult = new StreamResult(new File(fileName));
                transformer.transform(source, streamResult);

                publish("Файл записан: " + fileName);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                String last = chunks.get(chunks.size() - 1);
                setStatus(last, true);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                cancelButton.setEnabled(false);
                if (!cancelRequested.get()) {
                    setStatus("Сохранение завершено", false);
                    JOptionPane.showMessageDialog(frame,
                            "Данные успешно сохранены в XML-файл: " + fileName
                                    + "\nТип: " + dataType,
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    setStatus("Сохранение отменено", false);
                    JOptionPane.showMessageDialog(frame,
                            "Операция сохранения отменена пользователем.",
                            "Отмена", JOptionPane.INFORMATION_MESSAGE);
                }
                cancelRequested.set(false);
            }
        };
        executorService.execute(currentWorker);
    }

    private Element createTextElement(Document doc, String tagName, String text) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(text));
        return element;
    }

    private void onLoadFromFile() {
        String[] dataTypes = {"Только книги", "Только читатели", "Всё вместе"};
        String dataType = (String) JOptionPane.showInputDialog(
                frame, "Что загрузить из XML-файла?", "Тип данных",
                JOptionPane.QUESTION_MESSAGE, null, dataTypes, dataTypes[2]);
        if (dataType == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Загрузка данных из XML-файла");
        fileChooser.setFileFilter(new FileNameExtensionFilter("XML файлы", "xml"));

        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;
        String fileName = fileChooser.getSelectedFile().getAbsolutePath();

        setStatus("Загрузка из файла...", true);
        progressBar.setIndeterminate(true);

        currentWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new File(fileName));
                doc.getDocumentElement().normalize();

                Element root = doc.getDocumentElement();
                if (!"library".equals(root.getTagName())) {
                    throw new Exception("Ожидается корневой элемент <library>.");
                }

                if ("Только книги".equals(dataType) || "Всё вместе".equals(dataType)) {
                    NodeList booksList = doc.getElementsByTagName("book");
                    publish("Найдено книг: " + booksList.getLength());
                    for (int i = 0; i < booksList.getLength(); i++) {
                        if (cancelRequested.get()) {
                            publish("Отменено пользователем");
                            return null;
                        }
                        Element bookElement = (Element) booksList.item(i);
                        String cipher = bookElement.getAttribute("cipher");
                        boolean available = Boolean.parseBoolean(
                                bookElement.getAttribute("available"));

                        String title = bookElement.getElementsByTagName("title")
                                .item(0).getTextContent();
                        String author = bookElement.getElementsByTagName("author")
                                .item(0).getTextContent();
                        int year = Integer.parseInt(
                                bookElement.getElementsByTagName("year")
                                        .item(0).getTextContent());
                        String genre = bookElement.getElementsByTagName("genre")
                                .item(0).getTextContent();

                        Book book = new Book(cipher, title, author, year, genre);
                        book.setAvailable(available);
                        library.addBook(book);
                        publish("Загружена книга " + (i + 1) + " из " + booksList.getLength());
                        Thread.sleep(50);
                    }
                }

                if ("Только читатели".equals(dataType) || "Всё вместе".equals(dataType)) {
                    NodeList readersList = doc.getElementsByTagName("reader");
                    publish("Найдено читателей: " + readersList.getLength());
                    for (int i = 0; i < readersList.getLength(); i++) {
                        if (cancelRequested.get()) {
                            publish("Отменено пользователем");
                            return null;
                        }
                        Element readerElement = (Element) readersList.item(i);
                        String cardNumber = readerElement.getAttribute("studentCardNumber");

                        String name = readerElement.getElementsByTagName("name")
                                .item(0).getTextContent();
                        String surname = readerElement.getElementsByTagName("surname")
                                .item(0).getTextContent();

                        Reader readerObj = new Reader(cardNumber, name, surname);
                        library.addReader(readerObj);
                        publish("Загружен читатель " + (i + 1) + " из " + readersList.getLength());
                        Thread.sleep(50);
                    }
                }

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                String last = chunks.get(chunks.size() - 1);
                setStatus(last, true);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                if (!cancelRequested.get()) {
                    refreshBookTable();
                    refreshReaderTable();
                    setStatus("Загрузка завершена", false);
                    JOptionPane.showMessageDialog(frame,
                            "Данные успешно загружены из XML-файла: " + fileName
                                    + "\nТип: " + dataType,
                            "Успех", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    setStatus("Загрузка отменена", false);
                    JOptionPane.showMessageDialog(frame,
                            "Операция загрузки отменена пользователем.",
                            "Отмена", JOptionPane.INFORMATION_MESSAGE);
                }
                cancelRequested.set(false);
            }
        };
        executorService.execute(currentWorker);
    }

    private void onSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            refreshBookTable();
            return;
        }

        List<Book> allBooks = library.getAllBooks();
        bookTableModel.setRowCount(0);
        for (Book book : allBooks) {
            if (book.getTitle().toLowerCase().contains(query)
                    || book.getAuthor().toLowerCase().contains(query)) {
                bookTableModel.addRow(new Object[]{
                        book.getCipher(), book.getTitle(), book.getAuthor(),
                        book.getYear(), book.getGenre(),
                        book.isAvailable() ? "Свободна" : "Выдана"
                });
            }
        }
    }

    private void refreshBookTable() {
        setStatus("Загрузка книг...", true);
        currentWorker = new SwingWorker<List<Book>, Void>() {
            @Override
            protected List<Book> doInBackground() {
                return library.getAllBooks();
            }

            @Override
            protected void done() {
                try {
                    bookTableModel.setRowCount(0);
                    List<Book> books = get();
                    for (Book book : books) {
                        bookTableModel.addRow(new Object[]{
                                book.getCipher(), book.getTitle(), book.getAuthor(),
                                book.getYear(), book.getGenre(),
                                book.isAvailable() ? "Свободна" : "Выдана"
                        });
                    }
                    setStatus("Книг загружено: " + books.size(), false);
                } catch (Exception ex) {
                    setStatus("Ошибка загрузки книг", false);
                    JOptionPane.showMessageDialog(frame,
                            "Ошибка: " + ex.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        executorService.execute(currentWorker);
    }

    private void refreshReaderTable() {
        setStatus("Загрузка читателей...", true);
        currentWorker = new SwingWorker<List<Reader>, Void>() {
            @Override
            protected List<Reader> doInBackground() {
                return library.getAllReaders();
            }

            @Override
            protected void done() {
                try {
                    readerTableModel.setRowCount(0);
                    List<Reader> readers = get();
                    for (Reader reader : readers) {
                        readerTableModel.addRow(new Object[]{
                                reader.getStudentCardNumber(),
                                reader.getName(),
                                reader.getSurname(),
                                reader.getBorrowedCount()
                        });
                    }
                    setStatus("Читателей загружено: " + readers.size(), false);
                } catch (Exception ex) {
                    setStatus("Ошибка загрузки читателей", false);
                    JOptionPane.showMessageDialog(frame,
                            "Ошибка: " + ex.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        executorService.execute(currentWorker);
    }

    private void onGenerateReport() {
        String[] reportTypes = {"По книгам", "По читателям", "Всё целиком"};
        String reportType = (String) JOptionPane.showInputDialog(
                frame, "Выберите тип отчёта:", "Тип отчёта",
                JOptionPane.QUESTION_MESSAGE, null, reportTypes, reportTypes[0]);
        if (reportType == null) return;

        String[] formats = {"PDF", "HTML"};
        String format = (String) JOptionPane.showInputDialog(
                frame, "Выберите формат отчёта:", "Формат отчёта",
                JOptionPane.QUESTION_MESSAGE, null, formats, formats[0]);
        if (format == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить отчёт");
        String defaultName = "library_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setSelectedFile(new File(defaultName + "." + format.toLowerCase()));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                format + " файлы", format.toLowerCase()));

        int result = fileChooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;
        String fileName = fileChooser.getSelectedFile().getAbsolutePath();

        try {
            List<Book> books = library.getAllBooks();
            List<Reader> readers = library.getAllReaders();
            long available = books.stream().filter(Book::isAvailable).count();
            long issued = books.size() - available;

            List<Map<String, Object>> bookData = new ArrayList<>();
            for (Book book : books) {
                Map<String, Object> row = new HashMap<>();
                row.put("cipher", book.getCipher());
                row.put("title", book.getTitle());
                row.put("author", book.getAuthor());
                row.put("year", book.getYear());
                row.put("genre", book.getGenre() != null ? book.getGenre() : "");
                row.put("status", book.isAvailable() ? "Свободна" : "Выдана");
                bookData.add(row);
            }

            List<Map<String, Object>> readerData = new ArrayList<>();
            for (Reader reader : readers) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentCardNumber", reader.getStudentCardNumber());
                row.put("name", reader.getName());
                row.put("surname", reader.getSurname());
                row.put("borrowedCount", reader.getBorrowedCount());
                readerData.add(row);
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("libraryName", library.getName());
            parameters.put("reportDate",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            parameters.put("totalBooks", books.size());
            parameters.put("totalReaders", readers.size());

            JasperPrint jasperPrint;

            if ("По книгам".equals(reportType)) {
                parameters.put("availableBooks", (int) available);
                parameters.put("issuedBooks", (int) issued);
                JasperReport jasperReport = compileTemplate("/reports/library_report.jrxml");
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters,
                        new JRBeanCollectionDataSource(bookData));

            } else if ("По читателям".equals(reportType)) {
                parameters.put("totalBooksIssued", (int) issued);
                JasperReport jasperReport = compileTemplate("/reports/readers_report.jrxml");
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters,
                        new JRBeanCollectionDataSource(readerData));

            } else {
                parameters.put("availableBooks", (int) available);
                parameters.put("issuedBooks", (int) issued);
                parameters.put("totalBooksIssued", (int) issued);
                parameters.put("booksDataSource", new JRBeanCollectionDataSource(bookData));
                parameters.put("readersDataSource", new JRBeanCollectionDataSource(readerData));
                parameters.put("booksSubreport", compileTemplate("/reports/books_subreport.jrxml"));
                parameters.put("readersSubreport", compileTemplate("/reports/readers_subreport.jrxml"));
                JasperReport jasperReport = compileTemplate("/reports/full_report.jrxml");
                jasperPrint = JasperFillManager.fillReport(jasperReport, parameters,
                        new JREmptyDataSource());
            }

            if ("PDF".equals(format)) {
                JasperExportManager.exportReportToPdfFile(jasperPrint, fileName);
            } else {
                JasperExportManager.exportReportToHtmlFile(jasperPrint, fileName);
            }

            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("Отчёт: " + reportType);
            viewer.setVisible(true);

            JOptionPane.showMessageDialog(frame,
                    "Отчёт успешно сформирован: " + fileName,
                    "Успех", JOptionPane.INFORMATION_MESSAGE);

        } catch (JRException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка JasperReports: " + ex.getMessage(),
                    "Ошибка отчёта", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка при формировании отчёта: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JasperReport compileTemplate(String resourcePath) throws JRException, IOException {
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream == null) {
            File file = new File("src/main/resources" + resourcePath);
            if (!file.exists()) {
                throw new JRException("Шаблон отчёта не найден: " + resourcePath);
            }
            stream = new FileInputStream(file);
        }
        return JasperCompileManager.compileReport(stream);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryApp().show());
    }
}