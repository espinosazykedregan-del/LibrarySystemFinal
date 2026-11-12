import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.logging.Logger;

// ==========================================
// 1. ABSTRACT ITEM CLASS
// ==========================================

abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String category;

    public Item(int id, String title, String category) {
        this.id = id;
        this.title = title;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }
}

// ==========================================
// 2. BOOK CLASS
// ==========================================

class Book extends Item {
    private static final long serialVersionUID = 1L;

    private String author;
    private boolean borrowed;

    public Book(int id, String title, String author, String category) {
        super(id, title, category);
        this.author = author;
        this.borrowed = false;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }

    public String getFormatDetails() {
        return "Book";
    }
}

// ==========================================
// 3. PRINTED BOOK CLASS
// ==========================================

class PrintedBook extends Book {
    private static final long serialVersionUID = 1L;

    private int pages;

    public PrintedBook(int id, String title, String author,
                       String category, int pages) {
        super(id, title, author, category);
        this.pages = pages;
    }

    public int getPages() {
        return pages;
    }

    @Override
    public String getFormatDetails() {
        return "Printed | " + pages + " pages";
    }
}

// ==========================================
// 4. EBOOK CLASS
// ==========================================

class Ebook extends Book {
    private static final long serialVersionUID = 1L;

    private String fileSize;

    public Ebook(int id, String title, String author,
                 String category, String fileSize) {
        super(id, title, author, category);
        this.fileSize = fileSize;
    }

    public String getFileSize() {
        return fileSize;
    }

    @Override
    public String getFormatDetails() {
        return "Ebook | " + fileSize;
    }
}

// ==========================================
// 5. BOOK CHOICE CLASS
// ==========================================

class BookChoice {
    private final int id;
    private final String title;

    public BookChoice(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("ID %02d  |  %s", id, title);
    }
}

// ==========================================
// 6. SQLITE DRIVER WRAPPER
// ==========================================

class DriverShim implements Driver {
    private final Driver driver;

    public DriverShim(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException {
        return driver.connect(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(
            String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() {
        try {
            return driver.getParentLogger();
        } catch (Exception e) {
            return Logger.getLogger("SQLite");
        }
    }
}

// ==========================================
// 7. MAIN CLASS
// ==========================================

public class Main {

    private static final String DB_URL =
            "jdbc:sqlite:zykelibrary.db";

    private static DefaultTableModel tableModel;
    private static JTable tableBooks;
    private static JTextArea txtArea;

    private static JLabel lblTotalVal;
    private static JLabel lblAvailableVal;
    private static JLabel lblBorrowedVal;
    private static JLabel lblStatus;

    private static JComboBox<BookChoice> cmbBooks;

    private static TableRowSorter<DefaultTableModel> rowSorter;

    private static final ArrayList<Book> memoryBooks =
            new ArrayList<>();

    private static boolean databaseAvailable = false;

    private static URLClassLoader sqliteLoader;

    // COLORS

    private static final Color SIDEBAR =
            new Color(24, 36, 58);

    private static final Color SIDEBAR_LIGHT =
            new Color(43, 58, 83);

    private static final Color BACKGROUND =
            new Color(245, 247, 251);

    private static final Color CARD =
            Color.WHITE;

    private static final Color PRIMARY =
            new Color(43, 102, 220);

    private static final Color SUCCESS =
            new Color(24, 155, 91);

    private static final Color DANGER =
            new Color(210, 66, 66);

    private static final Color TEXT =
            new Color(31, 41, 55);

    // ==========================================
    // 8. MAIN METHOD
    // ==========================================

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    // ==========================================
    // 9. CREATE GUI
    // ==========================================

    private static void createAndShowGUI() {

        JFrame frame = new JFrame(
                "ZYKELibrary | Library Management System"
        );

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 700));
        frame.setSize(1280, 820);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BACKGROUND);

        // TABLE MODEL

        tableModel = new DefaultTableModel(
                new Object[]{
                        "ID",
                        "Book Title",
                        "Author",
                        "Category",
                        "Format / Details",
                        "Status"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableBooks = new JTable(tableModel);
        styleBookTable();

        JScrollPane tableScroll = new JScrollPane(tableBooks);
        tableScroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 226, 235)
                )
        );
        tableScroll.getViewport().setBackground(Color.WHITE);

        // SIDEBAR

        JPanel sidebar = createSidebar();

        // TOP AREA

        JPanel topArea = new JPanel();
        topArea.setOpaque(false);
        topArea.setLayout(
                new BoxLayout(topArea, BoxLayout.Y_AXIS)
        );

        topArea.add(createPageHeader());
        topArea.add(Box.createVerticalStrut(18));
        topArea.add(createStatsPanel());
        topArea.add(Box.createVerticalStrut(16));
        topArea.add(createControlsPanel());

        // CATALOG PANEL

        JPanel catalogPanel = new JPanel(new BorderLayout(0, 8));
        catalogPanel.setBackground(CARD);
        catalogPanel.setBorder(
                new EmptyBorder(14, 16, 14, 16)
        );

        JLabel catalogTitle = new JLabel("BOOK CATALOG");
        catalogTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 15)
        );
        catalogTitle.setForeground(TEXT);

        JLabel catalogHint = new JLabel(
                "Select a row to choose its book ID."
        );
        catalogHint.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );
        catalogHint.setForeground(
                new Color(115, 125, 140)
        );

        JPanel catalogHeading = new JPanel(new BorderLayout());
        catalogHeading.setOpaque(false);
        catalogHeading.add(catalogTitle, BorderLayout.WEST);
        catalogHeading.add(catalogHint, BorderLayout.EAST);

        catalogPanel.add(catalogHeading, BorderLayout.NORTH);
        catalogPanel.add(tableScroll, BorderLayout.CENTER);

        // ACTIVITY LOG

        JPanel logPanel = createLogPanel();

        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                catalogPanel,
                logPanel
        );

        centerSplit.setResizeWeight(0.76);
        centerSplit.setDividerLocation(430);
        centerSplit.setBorder(null);
        centerSplit.setContinuousLayout(true);
        centerSplit.setDividerSize(8);

        // MAIN CONTENT

        JPanel mainContent = new JPanel(new BorderLayout(0, 18));
        mainContent.setBackground(BACKGROUND);
        mainContent.setBorder(
                new EmptyBorder(25, 28, 24, 28)
        );

        mainContent.add(topArea, BorderLayout.NORTH);
        mainContent.add(centerSplit, BorderLayout.CENTER);

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(mainContent, BorderLayout.CENTER);

        // DATABASE AND BOOKS

        seedMemoryBooks();
        initDatabase();
        loadBooksIntoTable();
        updateDashboardStats();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        appendLog("[READY] ZYKELibrary is ready.");
        appendLog("[INFO] Ten sample books are loaded.");
    }

    // ==========================================
    // 10. SAMPLE BOOKS
    // ==========================================

    private static void seedMemoryBooks() {

        memoryBooks.clear();

        memoryBooks.add(new PrintedBook(
                1, "Clean Code",
                "Robert C. Martin",
                "Programming", 464
        ));

        memoryBooks.add(new PrintedBook(
                2, "Effective Java",
                "Joshua Bloch",
                "Programming", 416
        ));

        memoryBooks.add(new PrintedBook(
                3, "Head First Java",
                "Kathy Sierra and Bert Bates",
                "Programming", 720
        ));

        memoryBooks.add(new PrintedBook(
                4, "Introduction to Algorithms",
                "Thomas H. Cormen et al.",
                "Computer Science", 1312
        ));

        memoryBooks.add(new PrintedBook(
                5, "The Pragmatic Programmer",
                "David Thomas and Andrew Hunt",
                "Software Engineering", 352
        ));

        memoryBooks.add(new PrintedBook(
                6, "Database System Concepts",
                "Abraham Silberschatz et al.",
                "Database", 1376
        ));

        memoryBooks.add(new PrintedBook(
                7, "Computer Networking",
                "James Kurose and Keith Ross",
                "Networking", 864
        ));

        memoryBooks.add(new Ebook(
                8, "Python Crash Course",
                "Eric Matthes",
                "Programming", "18 MB"
        ));

        memoryBooks.add(new PrintedBook(
                9, "Artificial Intelligence: A Modern Approach",
                "Stuart Russell and Peter Norvig",
                "Artificial Intelligence", 1160
        ));

        memoryBooks.add(new Ebook(
                10, "Operating System Concepts",
                "Abraham Silberschatz et al.",
                "Operating Systems", "24 MB"
        ));
    }

    // ==========================================
    // 11. FIND AND LOAD SQLITE JDBC DRIVER
    // ==========================================

    private static boolean loadSqliteDriver() {

        try {

            ArrayList<File> folders = new ArrayList<>();

            // Folder where Main.class is located

            File classFolder = new File(
                    Main.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            if (classFolder.isFile()) {
                classFolder = classFolder.getParentFile();
            }

            folders.add(classFolder);

            // Current working folder

            folders.add(new File(
                    System.getProperty("user.dir")
            ));

            for (File folder : folders) {

                if (folder == null || !folder.exists()) {
                    continue;
                }

                File[] jars = folder.listFiles(
                        (dir, name) ->
                                name.toLowerCase().startsWith("sqlite-jdbc")
                                && name.toLowerCase().endsWith(".jar")
                );

                if (jars == null) {
                    continue;
                }

                for (File jar : jars) {

                    URL jarURL = jar.toURI().toURL();

                    sqliteLoader = new URLClassLoader(
                            new URL[]{jarURL},
                            Main.class.getClassLoader()
                    );

                    Class<?> driverClass = Class.forName(
                            "org.sqlite.JDBC",
                            true,
                            sqliteLoader
                    );

                    Driver realDriver = (Driver)
                            driverClass.getDeclaredConstructor()
                                    .newInstance();

                    DriverManager.registerDriver(
                            new DriverShim(realDriver)
                    );

                    appendLog(
                            "[SUCCESS] SQLite JDBC driver found: "
                                    + jar.getName()
                    );

                    return true;
                }
            }

        } catch (Exception e) {

            appendLog(
                    "[INFO] SQLite driver could not be loaded: "
                            + e.getMessage()
            );
        }

        return false;
    }

    // ==========================================
    // 12. INITIALIZE SQLITE DATABASE
    // ==========================================

    private static void initDatabase() {

        databaseAvailable = false;

        if (!loadSqliteDriver()) {

            appendLog(
                    "[INFO] Using the built-in 10-book sample list."
            );

            appendLog(
                    "[INFO] Place sqlite-jdbc JAR beside Main.java to enable SQL storage."
            );

            setStatus(
                    "Books loaded. SQLite driver not detected."
            );

            return;
        }

        String createTable =
                "CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "extra TEXT NOT NULL, " +
                "borrowed INTEGER NOT NULL DEFAULT 0)";

        String insertSql =
                "INSERT OR IGNORE INTO books " +
                "(id, title, author, category, type, extra, borrowed) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0)";

        try (
                Connection connection =
                        DriverManager.getConnection(DB_URL);

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(createTable);

            try (
                    PreparedStatement insert =
                            connection.prepareStatement(insertSql)
            ) {

                for (Book book : memoryBooks) {

                    insert.setInt(1, book.getId());
                    insert.setString(2, book.getTitle());
                    insert.setString(3, book.getAuthor());
                    insert.setString(4, book.getCategory());

                    if (book instanceof PrintedBook) {

                        PrintedBook printed =
                                (PrintedBook) book;

                        insert.setString(5, "PrintedBook");
                        insert.setString(
                                6,
                                String.valueOf(printed.getPages())
                        );

                    } else if (book instanceof Ebook) {

                        Ebook ebook = (Ebook) book;

                        insert.setString(5, "Ebook");
                        insert.setString(6, ebook.getFileSize());

                    } else {

                        insert.setString(5, "Book");
                        insert.setString(6, "N/A");
                    }

                    insert.executeUpdate();
                }
            }

            databaseAvailable = true;

            appendLog(
                    "[SUCCESS] SQLite database connected."
            );

            appendLog(
                    "[SUCCESS] Ten sample books checked in the database."
            );

        } catch (SQLException e) {

            databaseAvailable = false;

            appendLog(
                    "[DATABASE ERROR] " + e.getMessage()
            );

            appendLog(
                    "[INFO] The program will display the 10 sample books without database storage."
            );
        }
    }

    // ==========================================
    // 13. LOAD BOOKS INTO TABLE
    // ==========================================

    private static void loadBooksIntoTable() {

        if (tableModel == null) {
            return;
        }

        int previousId = getSelectedBookId();

        tableModel.setRowCount(0);

        cmbBooks.removeAllItems();

        if (databaseAvailable) {

            String sql =
                    "SELECT id, title, author, category, " +
                    "type, extra, borrowed FROM books ORDER BY id";

            try (
                    Connection connection =
                            DriverManager.getConnection(DB_URL);

                    Statement statement =
                            connection.createStatement();

                    ResultSet rs =
                            statement.executeQuery(sql)
            ) {

                while (rs.next()) {

                    int id = rs.getInt("id");

                    String title = rs.getString("title");
                    String author = rs.getString("author");
                    String category = rs.getString("category");
                    String type = rs.getString("type");
                    String extra = rs.getString("extra");

                    boolean borrowed =
                            rs.getInt("borrowed") == 1;

                    String details;

                    if ("Ebook".equalsIgnoreCase(type)) {
                        details = "Ebook | " + extra;
                    } else {
                        details = "Printed | " + extra + " pages";
                    }

                    addBookRow(
                            id, title, author, category,
                            details, borrowed
                    );
                }

            } catch (SQLException e) {

                databaseAvailable = false;

                appendLog(
                        "[LOAD ERROR] " + e.getMessage()
                );

                loadMemoryBooks();
            }

        } else {

            loadMemoryBooks();
        }

        if (previousId != -1) {
            selectBookById(previousId);
        }

        if (cmbBooks.getItemCount() > 0
                && cmbBooks.getSelectedIndex() == -1) {
            cmbBooks.setSelectedIndex(0);
        }

        appendLog(
                "[INFO] Loaded "
                        + tableModel.getRowCount()
                        + " books into the catalog."
        );
    }

    private static void loadMemoryBooks() {

        for (Book book : memoryBooks) {

            addBookRow(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCategory(),
                    book.getFormatDetails(),
                    book.isBorrowed()
            );
        }
    }

    private static void addBookRow(
            int id,
            String title,
            String author,
            String category,
            String details,
            boolean borrowed
    ) {

        String status = borrowed
                ? "Borrowed"
                : "Available";

        tableModel.addRow(
                new Object[]{
                        id,
                        title,
                        author,
                        category,
                        details,
                        status
                }
        );

        cmbBooks.addItem(
                new BookChoice(id, title)
        );
    }

    // ==========================================
    // 14. DASHBOARD STATISTICS
    // ==========================================

    private static void updateDashboardStats() {

        if (databaseAvailable) {

            String sql =
                    "SELECT COUNT(*) AS total, " +
                    "COALESCE(SUM(borrowed), 0) AS borrowed " +
                    "FROM books";

            try (
                    Connection connection =
                            DriverManager.getConnection(DB_URL);

                    Statement statement =
                            connection.createStatement();

                    ResultSet rs =
                            statement.executeQuery(sql)
            ) {

                if (rs.next()) {

                    int total = rs.getInt("total");
                    int borrowed = rs.getInt("borrowed");

                    lblTotalVal.setText(
                            String.valueOf(total)
                    );

                    lblBorrowedVal.setText(
                            String.valueOf(borrowed)
                    );

                    lblAvailableVal.setText(
                            String.valueOf(total - borrowed)
                    );
                }

                return;

            } catch (SQLException e) {

                databaseAvailable = false;

                appendLog(
                        "[STATS ERROR] " + e.getMessage()
                );
            }
        }

        int total = memoryBooks.size();
        int borrowed = 0;

        for (Book book : memoryBooks) {
            if (book.isBorrowed()) {
                borrowed++;
            }
        }

        lblTotalVal.setText(String.valueOf(total));
        lblBorrowedVal.setText(String.valueOf(borrowed));
        lblAvailableVal.setText(String.valueOf(total - borrowed));
    }

    // ==========================================
    // 15. GET SELECTED BOOK ID
    // ==========================================

    private static int getSelectedBookId() {

        if (cmbBooks == null) {
            return -1;
        }

        Object selected = cmbBooks.getSelectedItem();

        if (selected instanceof BookChoice) {
            return ((BookChoice) selected).getId();
        }

        return -1;
    }

    // ==========================================
    // 16. SELECT BOOK BY ID
    // ==========================================

    private static void selectBookById(int id) {

        for (int i = 0; i < cmbBooks.getItemCount(); i++) {

            BookChoice choice = cmbBooks.getItemAt(i);

            if (choice.getId() == id) {
                cmbBooks.setSelectedIndex(i);
                return;
            }
        }
    }

    // ==========================================
    // 17. BORROW AND RETURN BOOK
    // ==========================================

    private static void handleAction(String action) {

        int id = getSelectedBookId();

        if (id == -1) {
            setStatus("Please choose a book first.");
            appendLog("[WARNING] No book was selected.");
            return;
        }

        if (databaseAvailable) {

            if (handleDatabaseAction(id, action)) {
                loadBooksIntoTable();
                updateDashboardStats();
                return;
            }

            // If the database action fails, continue using memory.
            databaseAvailable = false;

            appendLog(
                    "[INFO] Switching to the sample book list."
            );
        }

        handleMemoryAction(id, action);

        loadBooksIntoTable();
        updateDashboardStats();
    }

    private static boolean handleDatabaseAction(
            int id,
            String action
    ) {

        String selectSql =
                "SELECT title, borrowed FROM books WHERE id = ?";

        String updateSql =
                "UPDATE books SET borrowed = ? WHERE id = ?";

        try (
                Connection connection =
                        DriverManager.getConnection(DB_URL)
        ) {

            String title;
            boolean borrowed;

            try (
                    PreparedStatement select =
                            connection.prepareStatement(selectSql)
            ) {

                select.setInt(1, id);

                try (ResultSet rs = select.executeQuery()) {

                    if (!rs.next()) {
                        setStatus("Book ID " + id + " was not found.");
                        return true;
                    }

                    title = rs.getString("title");
                    borrowed = rs.getInt("borrowed") == 1;
                }
            }

            if ("Borrow".equals(action) && borrowed) {
                setStatus("This book is already borrowed.");
                appendLog("[WARNING] " + title + " is already borrowed.");
                return true;
            }

            if ("Return".equals(action) && !borrowed) {
                setStatus("This book has not been borrowed.");
                appendLog("[WARNING] " + title + " was not checked out.");
                return true;
            }

            int newStatus = "Borrow".equals(action) ? 1 : 0;

            try (
                    PreparedStatement update =
                            connection.prepareStatement(updateSql)
            ) {

                update.setInt(1, newStatus);
                update.setInt(2, id);

                update.executeUpdate();
            }

            String message = "Borrow".equals(action)
                    ? "Successfully borrowed: " + title
                    : "Successfully returned: " + title;

            setStatus(message);

            appendLog(
                    "[SUCCESS] " + message + " (ID: " + id + ")"
            );

            return true;

        } catch (SQLException e) {

            appendLog(
                    "[SQL ACTION ERROR] " + e.getMessage()
            );

            return false;
        }
    }

    private static void handleMemoryAction(
            int id,
            String action
    ) {

        for (Book book : memoryBooks) {

            if (book.getId() == id) {

                if ("Borrow".equals(action)) {

                    if (book.isBorrowed()) {
                        setStatus("This book is already borrowed.");
                        appendLog(
                                "[WARNING] " + book.getTitle()
                                        + " is already borrowed."
                        );
                        return;
                    }

                    book.setBorrowed(true);

                    setStatus(
                            "Successfully borrowed: " + book.getTitle()
                    );

                    appendLog(
                            "[SUCCESS] Borrowed: "
                                    + book.getTitle()
                                    + " (ID: " + id + ")"
                    );

                } else {

                    if (!book.isBorrowed()) {
                        setStatus("This book has not been borrowed.");
                        appendLog(
                                "[WARNING] " + book.getTitle()
                                        + " was not checked out."
                        );
                        return;
                    }

                    book.setBorrowed(false);

                    setStatus(
                            "Successfully returned: " + book.getTitle()
                    );

                    appendLog(
                            "[SUCCESS] Returned: "
                                    + book.getTitle()
                                    + " (ID: " + id + ")"
                    );
                }

                return;
            }
        }

        setStatus("Book ID " + id + " was not found.");
    }

    // ==========================================
    // 18. SEARCH TABLE
    // ==========================================

    private static void filterTable(String query) {

        if (rowSorter == null) {
            return;
        }

        String cleaned = query == null ? "" : query.trim();

        if (cleaned.isEmpty()) {
            rowSorter.setRowFilter(null);
            setStatus("Showing all books.");
            return;
        }

        rowSorter.setRowFilter(
                RowFilter.regexFilter(
                        "(?i)" + Pattern.quote(cleaned)
                )
        );

        setStatus("Search filter applied.");
    }

    // ==========================================
    // 19. SIDEBAR
    // ==========================================

    private static JPanel createSidebar() {

        JPanel sidebar = new JPanel();

        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(225, 720));
        sidebar.setBorder(
                new EmptyBorder(28, 18, 24, 18)
        );

        sidebar.setLayout(
                new BoxLayout(sidebar, BoxLayout.Y_AXIS)
        );

        JLabel logo = new JLabel("  ZYKELibrary");
        logo.setForeground(Color.WHITE);
        logo.setFont(
                new Font("Segoe UI", Font.BOLD, 21)
        );

        JLabel subtitle = new JLabel("LIBRARY MANAGEMENT");
        subtitle.setForeground(new Color(164, 178, 199));
        subtitle.setFont(
                new Font("Segoe UI", Font.BOLD, 10)
        );

        JButton dashboard = new JButton("  Dashboard & Catalog");
        dashboard.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashboard.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44)
        );
        dashboard.setHorizontalAlignment(SwingConstants.LEFT);
        dashboard.setForeground(Color.WHITE);
        dashboard.setBackground(SIDEBAR_LIGHT);
        dashboard.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );
        dashboard.setFocusPainted(false);
        dashboard.setBorder(
                new EmptyBorder(0, 12, 0, 8)
        );

        JLabel version = new JLabel("System v4.1");
        version.setForeground(new Color(145, 158, 179));
        version.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
        );

        JLabel copyright = new JLabel("© 2026 ZYKELibrary");
        copyright.setForeground(new Color(115, 130, 153));
        copyright.setFont(
                new Font("Segoe UI", Font.PLAIN, 10)
        );

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(7));
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(35));
        sidebar.add(dashboard);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(version);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(copyright);

        return sidebar;
    }

    // ==========================================
    // 20. PAGE HEADER
    // ==========================================

    private static JPanel createPageHeader() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(
                new BoxLayout(titleBox, BoxLayout.Y_AXIS)
        );

        JLabel title = new JLabel("Library Dashboard");
        title.setFont(
                new Font("Segoe UI", Font.BOLD, 25)
        );
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel(
                "Manage your books, monitor availability, and process borrowing or returns."
        );
        subtitle.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        subtitle.setForeground(new Color(110, 120, 136));

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        JLabel label = new JLabel("BOOK MANAGEMENT");
        label.setFont(
                new Font("Segoe UI", Font.BOLD, 10)
        );
        label.setForeground(PRIMARY);
        label.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(199, 216, 250)
                        ),
                        new EmptyBorder(7, 10, 7, 10)
                )
        );

        panel.add(titleBox, BorderLayout.WEST);
        panel.add(label, BorderLayout.EAST);

        return panel;
    }

    // ==========================================
    // 21. DASHBOARD STATISTICS PANEL
    // ==========================================

    private static JPanel createStatsPanel() {

        JPanel panel = new JPanel(
                new GridLayout(1, 3, 14, 0)
        );
        panel.setOpaque(false);

        lblTotalVal = createStatValue();
        lblAvailableVal = createStatValue();
        lblBorrowedVal = createStatValue();

        panel.add(
                createStatCard("TOTAL BOOKS", lblTotalVal,
                        new Color(55, 120, 235))
        );

        panel.add(
                createStatCard("AVAILABLE", lblAvailableVal,
                        SUCCESS)
        );

        panel.add(
                createStatCard("BORROWED", lblBorrowedVal,
                        DANGER)
        );

        return panel;
    }

    private static JLabel createStatValue() {

        JLabel label = new JLabel("0");
        label.setFont(
                new Font("Segoe UI", Font.BOLD, 27)
        );
        label.setForeground(TEXT);

        return label;
    }

    private static JPanel createStatCard(
            String title,
            JLabel value,
            Color accent
    ) {

        JPanel card = new JPanel(
                new BorderLayout(8, 6)
        );
        card.setBackground(CARD);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0, 5, 0, 0, accent
                        ),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(
                                        new Color(226, 231, 239)
                                ),
                                new EmptyBorder(12, 16, 12, 14)
                        )
                )
        );

        JLabel heading = new JLabel(title);
        heading.setFont(
                new Font("Segoe UI", Font.BOLD, 10)
        );
        heading.setForeground(new Color(111, 122, 139));

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    // ==========================================
    // 22. CONTROL PANEL
    // ==========================================

    private static JPanel createControlsPanel() {

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CARD);
        outer.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 226, 235)
                        ),
                        new EmptyBorder(13, 15, 13, 15)
                )
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel chooseLabel = new JLabel("Choose Book:");
        chooseLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        chooseLabel.setForeground(TEXT);

        cmbBooks = new JComboBox<>();
        cmbBooks.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        cmbBooks.setPreferredSize(new Dimension(310, 36));
        cmbBooks.setMaximumRowCount(10);

        JButton borrowButton = createButton(
                "Borrow Book", SUCCESS
        );

        JButton returnButton = createButton(
                "Return Book", DANGER
        );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        outer.add(chooseLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        outer.add(cmbBooks, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        outer.add(borrowButton, gbc);

        gbc.gridx = 3;
        outer.add(returnButton, gbc);

        JSeparator separator = new JSeparator();

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        outer.add(separator, gbc);

        JLabel searchLabel = new JLabel("Search Catalog:");
        searchLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        searchLabel.setForeground(TEXT);

        JTextField searchField = new JTextField();
        searchField.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        searchField.setPreferredSize(new Dimension(250, 34));
        searchField.setToolTipText(
                "Search by title, author, category, ID, or status."
        );

        JButton searchButton = createButton("Search", PRIMARY);

        JButton showAllButton = createButton(
                "Show All / Refresh",
                new Color(100, 116, 139)
        );

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        outer.add(searchLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        outer.add(searchField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        outer.add(searchButton, gbc);

        gbc.gridx = 3;
        outer.add(showAllButton, gbc);

        borrowButton.addActionListener(
                e -> handleAction("Borrow")
        );

        returnButton.addActionListener(
                e -> handleAction("Return")
        );

        searchButton.addActionListener(
                e -> filterTable(searchField.getText())
        );

        searchField.addActionListener(
                e -> filterTable(searchField.getText())
        );

        showAllButton.addActionListener(e -> {

            searchField.setText("");

            if (rowSorter != null) {
                rowSorter.setRowFilter(null);
            }

            loadBooksIntoTable();
            updateDashboardStats();

            setStatus("Catalog refreshed.");
            appendLog("[INFO] Catalog refreshed.");
        });

        return outer;
    }

    private static JButton createButton(
            String text,
            Color background
    ) {

        JButton button = new JButton(text);

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(
                new EmptyBorder(9, 15, 9, 15)
        );
        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
        button.setPreferredSize(new Dimension(135, 36));

        return button;
    }

    // ==========================================
    // 23. BOOK TABLE DESIGN
    // ==========================================

    private static void styleBookTable() {

        tableBooks.setFont(
                new Font("Segoe UI", Font.PLAIN, 12)
        );
        tableBooks.setRowHeight(34);
        tableBooks.setShowVerticalLines(false);
        tableBooks.setShowHorizontalLines(true);
        tableBooks.setGridColor(
                new Color(232, 236, 242)
        );
        tableBooks.setIntercellSpacing(
                new Dimension(0, 1)
        );
        tableBooks.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tableBooks.setSelectionBackground(
                new Color(222, 235, 255)
        );
        tableBooks.setSelectionForeground(TEXT);
        tableBooks.setFillsViewportHeight(true);

        JTableHeader header = tableBooks.getTableHeader();
        header.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        header.setBackground(
                new Color(238, 243, 250)
        );
        header.setForeground(TEXT);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centered =
                new DefaultTableCellRenderer();

        centered.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        tableBooks.getColumnModel()
                .getColumn(0)
                .setCellRenderer(centered);

        tableBooks.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(55);

        tableBooks.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(270);

        tableBooks.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(175);

        tableBooks.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(145);

        tableBooks.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(145);

        DefaultTableCellRenderer statusRenderer =
                new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {

                Component component =
                        super.getTableCellRendererComponent(
                                table, value, isSelected,
                                hasFocus, row, column
                        );

                setHorizontalAlignment(
                        SwingConstants.CENTER
                );

                setFont(
                        new Font("Segoe UI", Font.BOLD, 11)
                );

                if (!isSelected) {

                    if ("Available".equals(value)) {

                        setForeground(new Color(22, 101, 52));
                        setBackground(new Color(220, 252, 231));

                    } else {

                        setForeground(new Color(153, 27, 27));
                        setBackground(new Color(254, 226, 226));
                    }
                }

                setOpaque(true);

                return component;
            }
        };

        tableBooks.getColumnModel()
                .getColumn(5)
                .setCellRenderer(statusRenderer);

        tableBooks.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(105);

        rowSorter = new TableRowSorter<>(tableModel);
        tableBooks.setRowSorter(rowSorter);

        tableBooks.addMouseListener(
                new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {

                int selectedRow =
                        tableBooks.getSelectedRow();

                if (selectedRow >= 0) {

                    int modelRow =
                            tableBooks.convertRowIndexToModel(
                                    selectedRow
                            );

                    int id = Integer.parseInt(
                            tableModel.getValueAt(
                                    modelRow, 0
                            ).toString()
                    );

                    selectBookById(id);
                }
            }
        });
    }

    // ==========================================
    // 24. ACTIVITY LOG PANEL
    // ==========================================

    private static JPanel createLogPanel() {

        JPanel panel = new JPanel(
                new BorderLayout(8, 8)
        );
        panel.setBackground(CARD);
        panel.setBorder(
                new EmptyBorder(10, 14, 12, 14)
        );

        JPanel statusPanel = new JPanel(
                new BorderLayout()
        );
        statusPanel.setOpaque(false);

        JLabel statusTitle = new JLabel(
                "SYSTEM NOTIFICATION"
        );
        statusTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 11)
        );
        statusTitle.setForeground(
                new Color(100, 115, 135)
        );

        lblStatus = new JLabel("System ready.");
        lblStatus.setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );
        lblStatus.setForeground(PRIMARY);

        statusPanel.add(statusTitle, BorderLayout.WEST);
        statusPanel.add(lblStatus, BorderLayout.CENTER);

        txtArea = new JTextArea();
        txtArea.setEditable(false);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        txtArea.setFont(
                new Font("Consolas", Font.PLAIN, 11)
        );
        txtArea.setBackground(
                new Color(20, 30, 48)
        );
        txtArea.setForeground(
                new Color(224, 232, 243)
        );
        txtArea.setMargin(
                new Insets(9, 10, 9, 10)
        );

        JScrollPane logScroll = new JScrollPane(txtArea);
        logScroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(218, 225, 235)
                )
        );

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(500, 145));

        return panel;
    }

    // ==========================================
    // 25. STATUS LABEL
    // ==========================================

    private static void setStatus(String message) {

        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    // ==========================================
    // 26. ACTIVITY LOG
    // ==========================================

    private static void appendLog(String message) {

        if (txtArea != null) {

            txtArea.append(message + "\n");

            txtArea.setCaretPosition(
                    txtArea.getDocument().getLength()
            );
        }
    }
}