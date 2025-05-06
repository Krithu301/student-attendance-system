// Save this as StudentAttendanceGUI.java

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class StudentAttendanceGUI {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db";
    private static final String USER = "root";
    private static final String PASS = "Gokul@2005";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createDatabase();
            showLogin();
        });
    }

    private static void createDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS student_attendance_db");
            stmt.execute("USE student_attendance_db");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS student_attendance (" +
                            "record_id INT AUTO_INCREMENT PRIMARY KEY," +
                            "student_id VARCHAR(15) NOT NULL," +
                            "student_name VARCHAR(50) NOT NULL," +
                            "course VARCHAR(50) NOT NULL," +
                            "semester VARCHAR(20) NOT NULL," +
                            "date DATE NOT NULL," +
                            "check_in TIME," +
                            "check_out TIME," +
                            "status ENUM('Present', 'Absent', 'Late', 'On Leave') DEFAULT 'Absent'," +
                            "hours_worked DECIMAL(4,2) DEFAULT 0.00," +
                            "INDEX idx_student_id (student_id)," +
                            "INDEX idx_date (date))"
            );

        } catch (SQLException e) {
            showErrorDialog("DB Error: " + e.getMessage());
        }
    }

    private static void showLogin() {
        JFrame frame = new JFrame("Student Attendance Login");
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        JLabel userLabel = new JLabel("Username:");
        JTextField userText = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passText = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        panel.add(userLabel);
        panel.add(userText);
        panel.add(passLabel);
        panel.add(passText);
        panel.add(new JLabel());
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            if ("admin".equals(userText.getText()) && "admin123".equals(new String(passText.getPassword()))) {
                frame.dispose();
                showMainGUI();
            } else {
                showErrorDialog("Invalid credentials!");
            }
        });

        frame.add(panel);
        frame.setSize(300, 150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void showMainGUI() {
        JFrame frame = new JFrame("Student Attendance System");
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);

        model.addColumn("Record ID");
        model.addColumn("Student ID");
        model.addColumn("Name");
        model.addColumn("Course");
        model.addColumn("Semester");
        model.addColumn("Date");
        model.addColumn("Check-In");
        model.addColumn("Check-Out");
        model.addColumn("Status");
        model.addColumn("Hours");

        loadData(model);

        JButton registerBtn = new JButton("Register Student");
        JButton checkInBtn = new JButton("Check In");
        JButton checkOutBtn = new JButton("Check Out");
        JButton markAbsentBtn = new JButton("Mark Absent");
        JButton refreshBtn = new JButton("Refresh");
        JButton eligibilityBtn = new JButton("Check Eligibility");

        registerBtn.addActionListener(e -> showRegisterDialog(model));
        checkInBtn.addActionListener(e -> showCheckInDialog(model));
        checkOutBtn.addActionListener(e -> showCheckOutDialog(table, model));
        markAbsentBtn.addActionListener(e -> showMarkAbsentDialog(model));
        refreshBtn.addActionListener(e -> {
            model.setRowCount(0);
            loadData(model);
        });
        eligibilityBtn.addActionListener(e -> showEligibilityDialog());

        JPanel btnPanel = new JPanel();
        btnPanel.add(registerBtn);
        btnPanel.add(checkInBtn);
        btnPanel.add(checkOutBtn);
        btnPanel.add(markAbsentBtn);
        btnPanel.add(refreshBtn);
        btnPanel.add(eligibilityBtn);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(btnPanel, BorderLayout.SOUTH);
        frame.setSize(1200, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void loadData(DefaultTableModel model) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM student_attendance ORDER BY date DESC, student_id")) {

            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("record_id"),
                        rs.getString("student_id"),
                        rs.getString("student_name"),
                        rs.getString("course"),
                        rs.getString("semester"),
                        rs.getDate("date"),
                        rs.getTime("check_in") != null ? rs.getTime("check_in").toLocalTime().format(timeFormat) : "-",
                        rs.getTime("check_out") != null ? rs.getTime("check_out").toLocalTime().format(timeFormat) : "-",
                        rs.getString("status"),
                        rs.getDouble("hours_worked")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Error loading data: " + e.getMessage());
        }
    }

    private static void showRegisterDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Register Student");
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

        JTextField stuIdField = new JTextField();
        JTextField stuNameField = new JTextField();
        JTextField courseField = new JTextField();
        JTextField semField = new JTextField();
        JButton saveBtn = new JButton("Register");

        panel.add(new JLabel("Student ID:")); panel.add(stuIdField);
        panel.add(new JLabel("Name:")); panel.add(stuNameField);
        panel.add(new JLabel("Course:")); panel.add(courseField);
        panel.add(new JLabel("Semester:")); panel.add(semField);
        panel.add(new JLabel()); panel.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO student_attendance (student_id, student_name, course, semester, date, status) " +
                                 "VALUES (?, ?, ?, ?, ?, 'Absent')")) {

                pstmt.setString(1, stuIdField.getText());
                pstmt.setString(2, stuNameField.getText());
                pstmt.setString(3, courseField.getText());
                pstmt.setString(4, semField.getText());
                pstmt.setDate(5, Date.valueOf(LocalDate.now()));
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(dialog, "Student registered!");
                dialog.dispose();
                model.setRowCount(0);
                loadData(model);
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private static void showCheckInDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Check-In");
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));

        JTextField stuIdField = new JTextField();
        JLabel stuInfo = new JLabel();
        JButton searchBtn = new JButton("Search");
        JButton checkInBtn = new JButton("Check In");

        panel.add(new JLabel("Student ID:")); panel.add(stuIdField);
        panel.add(new JLabel()); panel.add(searchBtn);
        panel.add(new JLabel("Student Info:")); panel.add(stuInfo);
        panel.add(new JLabel()); panel.add(checkInBtn);

        searchBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT student_name, course, semester FROM student_attendance " +
                                 "WHERE student_id = ? ORDER BY date DESC LIMIT 1")) {

                pstmt.setString(1, stuIdField.getText());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    stuInfo.setText(rs.getString("student_name") + " - " +
                            rs.getString("course") + " - " +
                            rs.getString("semester"));
                } else {
                    showErrorDialog("Student not found!");
                }
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        checkInBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE student_attendance SET check_in = ?, status = ? " +
                                 "WHERE student_id = ? AND date = ?")) {

                LocalTime currentTime = LocalTime.now();
                String status = currentTime.isAfter(LocalTime.of(9, 0)) ? "Late" : "Present";

                pstmt.setTime(1, Time.valueOf(currentTime));
                pstmt.setString(2, status);
                pstmt.setString(3, stuIdField.getText());
                pstmt.setDate(4, Date.valueOf(LocalDate.now()));
                int updated = pstmt.executeUpdate();

                if (updated > 0) {
                    JOptionPane.showMessageDialog(dialog, "Check-in recorded!");
                    dialog.dispose();
                    model.setRowCount(0);
                    loadData(model);
                } else {
                    showErrorDialog("No attendance record found for today!");
                }
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private static void showCheckOutDialog(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Select a student first!");
            return;
        }

        int recordId = (int) model.getValueAt(selectedRow, 0);
        String stuName = (String) model.getValueAt(selectedRow, 2);

        if (model.getValueAt(selectedRow, 6).equals("-")) {
            showErrorDialog("Not checked in yet!");
            return;
        }

        if (!model.getValueAt(selectedRow, 7).equals("-")) {
            showErrorDialog("Already checked out!");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE student_attendance SET check_out = ?, hours_worked = TIMESTAMPDIFF(MINUTE, check_in, ?)/60.0 " +
                             "WHERE record_id = ?")) {

            LocalTime currentTime = LocalTime.now();
            pstmt.setTime(1, Time.valueOf(currentTime));
            pstmt.setTime(2, Time.valueOf(currentTime));
            pstmt.setInt(3, recordId);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(null, stuName + " checked out successfully!");
            model.setRowCount(0);
            loadData(model);
        } catch (SQLException ex) {
            showErrorDialog("Database error: " + ex.getMessage());
        }
    }

    private static void showMarkAbsentDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Mark Absent");
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));

        JTextField stuIdField = new JTextField();
        JLabel stuInfo = new JLabel();
        JButton searchBtn = new JButton("Search");
        JButton markBtn = new JButton("Mark Absent");

        panel.add(new JLabel("Student ID:")); panel.add(stuIdField);
        panel.add(new JLabel()); panel.add(searchBtn);
        panel.add(new JLabel("Student Info:")); panel.add(stuInfo);
        panel.add(new JLabel()); panel.add(markBtn);

        searchBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT student_name, course, semester FROM student_attendance " +
                                 "WHERE student_id = ? ORDER BY date DESC LIMIT 1")) {

                pstmt.setString(1, stuIdField.getText());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    stuInfo.setText(rs.getString("student_name") + " - " +
                            rs.getString("course") + " - " +
                            rs.getString("semester"));
                } else {
                    showErrorDialog("Student not found!");
                }
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        markBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE student_attendance SET status = 'Absent' " +
                                 "WHERE student_id = ? AND date = ?")) {

                pstmt.setString(1, stuIdField.getText());
                pstmt.setDate(2, Date.valueOf(LocalDate.now()));
                int updated = pstmt.executeUpdate();

                if (updated > 0) {
                    JOptionPane.showMessageDialog(dialog, "Marked absent!");
                    dialog.dispose();
                    model.setRowCount(0);
                    loadData(model);
                } else {
                    showErrorDialog("No record found for today!");
                }
            } catch (Exception ex) {
                showErrorDialog("Error: " + ex.getMessage());
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private static void showEligibilityDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Exam Eligibility Report");
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);

        StringBuilder html = new StringBuilder("<html><body style='font-family: Arial;'>");
        html.append("<h2 style='text-align:center;'>Exam Eligibility Report</h2><hr>");

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT student_id, student_name, " +
                             "SUM(CASE WHEN status IN ('Present', 'Late') THEN 1 ELSE 0 END) AS attended, " +
                             "COUNT(*) AS total " +
                             "FROM student_attendance GROUP BY student_id, student_name")) {

            while (rs.next()) {
                double percentage = (rs.getInt("attended") * 100.0) / rs.getInt("total");
                String eligibility = (percentage >= 75.0) ? "Eligible" : "Not Eligible";
                String color = (percentage >= 75.0) ? "green" : "red";

                html.append("<p><b>")
                        .append(rs.getString("student_id")).append(" - ")
                        .append(rs.getString("student_name")).append("</b>: ")
                        .append(String.format("<span style='color:%s;'>%.2f%% - %s</span>", color, percentage, eligibility))
                        .append("</p>");
            }

        } catch (SQLException e) {
            showErrorDialog("Eligibility error: " + e.getMessage());
            return;
        }

        html.append("</body></html>");
        textPane.setText(html.toString());

        JScrollPane scrollPane = new JScrollPane(textPane);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}