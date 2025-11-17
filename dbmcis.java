import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class dbmcis extends JFrame {
    //database connection details are kept as final as we sectioned each action seperately
    private static final String DB_URL = "jdbc:oracle:thin:@oracle.scs.ryerson.ca:1521:orcl";
    private static final String USER = "USER";
    private static final String PASS = "PASS";
    private JTextArea outputArea = new JTextArea(18, 80);

    //query labels for dropdown as a string array
    private static final String[] QUERY_LABELS = {
        "All Patients",
        "All Doctors",
        "All Appointments",
        "All Prescriptions",
        "All Billing",
        "Patients by Age (oldest-youngest)",
        "Doctor Specialties",
        "Appointment Count by Date",
        "Unique Medications by Doctor",
        "Bills by Payment Status",
        "Appointment Details (JOIN)",
        "Billing Report (JOIN)",
        "Doctor Prescription History",
        "Patients with Multiple Doctors",
        "Patient Billing Summary",
        "Patients with >1 Appointment",
        "Doctors w/Appts No Prescriptions",
        "Patients: No Appt or Declined Bill",
        "Never Prescribed Aspirin",
        "Heart/Chest Appointments",
        "Bills $100-200",
        "Average Bill Amount"
    };

    //query commands, each query is reduced to one line for easier readability \" to not confused as a command
    private static final String[] QUERIES = {
        "SELECT * FROM patients",
        "SELECT * FROM doctors",
        "SELECT * FROM appointments",
        "SELECT * FROM prescriptions",
        "SELECT * FROM billing",
        "SELECT DISTINCT patient_id AS \"Patient ID\", name AS \"Patient Name\", date_of_birth AS \"Date of Birth\", healthcard_no AS \"Health Card Number\", contact AS \"Contact Information\", medical_history AS \"Medical History\" FROM patients ORDER BY date_of_birth ASC",
        "SELECT DISTINCT specialty AS \"Medical Specialty\" FROM doctors WHERE specialty IS NOT NULL ORDER BY specialty",
        "SELECT appointment_date AS \"Appointment Date\", COUNT(DISTINCT appointment_id) AS \"Appointments Per Day\" FROM appointments GROUP BY appointment_date ORDER BY appointment_date",
        "SELECT doctor_id AS \"Doctor ID\", COUNT(DISTINCT medication) AS unique_medications FROM prescriptions GROUP BY doctor_id ORDER BY unique_medications DESC",
        "SELECT DISTINCT bill_id AS \"Bill ID\", payment_status AS \"Payment Status\", amount AS \"Bill Amount\" FROM billing GROUP BY bill_id, payment_status, amount ORDER BY payment_status DESC",
        "SELECT a.appointment_id AS \"Appointment ID\", a.appointment_date AS \"Date\", a.appointment_time AS \"Time\", p.name AS \"Patient Name\", p.healthcard_no AS \"Health Card\", p.contact AS \"Patient Contact\", d.name AS \"Doctor Name\", d.specialty AS \"Specialty\", a.reason AS \"Reason for Visit\" FROM appointments a INNER JOIN patients p ON a.patient_id = p.patient_id INNER JOIN doctors d ON a.doctor_id = d.doctor_id ORDER BY a.appointment_date, a.appointment_time",
        "SELECT b.bill_id AS \"Bill ID\", p.name AS \"Patient Name\", p.healthcard_no AS \"Health Card Number\", a.appointment_date AS \"Appointment Date\", a.reason AS \"Visit Reason\", b.amount AS \"Bill Amount\", b.claim_amount AS \"Insurance Claim\", b.payment_status AS \"Payment Status\" FROM billing b INNER JOIN patients p ON b.patient_id = p.patient_id INNER JOIN appointments a ON b.appointment_id = a.appointment_id ORDER BY b.payment_status, b.bill_id",
        "SELECT d.name AS \"Doctor Name\", d.specialty AS \"Specialty\", p.name AS \"Patient Name\", p.medical_history AS \"Medical History\", pr.medication AS \"Medication\", pr.dosage AS \"Dosage\", pr.date_prescribed AS \"Date Prescribed\" FROM prescriptions pr INNER JOIN doctors d ON pr.doctor_id = d.doctor_id INNER JOIN patients p ON pr.patient_id = p.patient_id ORDER BY d.name, pr.date_prescribed DESC",
        "SELECT s.patient_id, s.name AS \"Patient Name\", s.contact FROM patients s WHERE s.patient_id IN ( SELECT a1.patient_id FROM appointments a1, appointments a2 WHERE a1.patient_id = a2.patient_id AND a1.doctor_id <> a2.doctor_id ) ORDER BY s.patient_id",
        "SELECT p.name AS \"Patient Name\", d.name AS \"Doctor Name\", a.appointment_date AS \"Date\", a.reason AS \"Reason\", b.amount AS \"Bill Amount\", b.payment_status AS \"Status\" FROM patients p, appointments a, billing b, doctors d WHERE p.patient_id = a.patient_id AND a.appointment_id = b.appointment_id AND a.doctor_id = d.doctor_id ORDER BY p.name, a.appointment_date",
        "SELECT p.patient_id, p.name, p.contact FROM patients p WHERE EXISTS ( SELECT a.patient_id FROM appointments a WHERE a.patient_id = p.patient_id GROUP BY a.patient_id HAVING COUNT(*) > 1 )",
        "SELECT DISTINCT d.doctor_id, d.name, d.specialty FROM doctors d, appointments a WHERE d.doctor_id = a.doctor_id MINUS SELECT DISTINCT d.doctor_id, d.name, d.specialty FROM doctors d, prescriptions p WHERE d.doctor_id = p.doctor_id",
        "SELECT patient_id, name FROM patients p WHERE NOT EXISTS ( SELECT * FROM appointments a WHERE a.patient_id = p.patient_id ) UNION SELECT p.patient_id, p.name FROM patients p, billing b WHERE p.patient_id = b.patient_id AND b.payment_status = 'DECLINED' AND NOT EXISTS ( SELECT * FROM billing b2 WHERE b2.patient_id = p.patient_id AND b2.payment_status <> 'DECLINED' )",
        "SELECT patient_id, name, medical_history FROM patients WHERE patient_id NOT IN ( SELECT patient_id FROM prescriptions WHERE medication LIKE '%Aspirin%' )",
        "SELECT a.appointment_id, p.name, a.appointment_date, a.reason FROM appointments a, patients p WHERE a.patient_id = p.patient_id AND (a.reason LIKE '%heart%' OR a.reason LIKE '%chest%' OR a.reason LIKE '%Chest%')",
        "SELECT bill_id, patient_id, amount, payment_status FROM billing WHERE amount BETWEEN 100 AND 200",
        "SELECT AVG(amount) AS \"Average Bill Amount\" FROM billing"
    };

    //view labels for dropdown as a string array
    private static final String[] VIEW_LABELS = {
        "Receptionist Appointments View",
        "Billing Officer Reports View",
        "Doctor Patient Records View"
    };

    //view queries which are taken from a view at the bottom of createTables function
    private static final String[] VIEW_QUERIES = {
        "SELECT * FROM receptionist_appointments",
        "SELECT * FROM billing_officer_reports",
        "SELECT * FROM doctor_patient_records"
    };

    //main constructor for GUI setup
    public dbmcis() {
        setTitle("MCIS Medical Clinic Information System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        //buttons for GUI
        JPanel buttonPanel = new JPanel(new GridLayout(1, 8, 5, 5));
        JButton dropBtn = new JButton("Drop Tables");
        JButton createBtn = new JButton("Create Tables");
        JButton populateBtn = new JButton("Populate Tables");
        JButton exitBtn = new JButton("Exit");

        //dropdown for queries
        JComboBox<String> tableDropdown = new JComboBox<>(QUERY_LABELS);
        JButton showTableBtn = new JButton("Show Selected Query");

        //dropdown for views
        JComboBox<String> viewDropdown = new JComboBox<>(VIEW_LABELS);
        JButton showViewBtn = new JButton("Show View");

        //adding buttons to panel
        buttonPanel.add(dropBtn);
        buttonPanel.add(createBtn);
        buttonPanel.add(populateBtn);
        buttonPanel.add(tableDropdown);
        buttonPanel.add(showTableBtn);
        buttonPanel.add(viewDropdown);
        buttonPanel.add(showViewBtn);
        buttonPanel.add(exitBtn);

        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        setSize(1450, 540);

        dropBtn.addActionListener(e -> executeAction(this::dropTables, "Tables Dropped Successfully!"));
        createBtn.addActionListener(e -> executeAction(this::createTables, "Tables and Views Created Successfully!"));
        populateBtn.addActionListener(e -> executeAction(this::populateTables, "Tables Populated Successfully!"));
        exitBtn.addActionListener(e -> System.exit(0));
        //if someone clicks on the show table button it will show the selected query
        showTableBtn.addActionListener(e -> {
            int idx = tableDropdown.getSelectedIndex();
            runTableViewQuery(QUERIES[idx]);
        });
        //if someone clicks on the view dropdown button it will show all views
        showViewBtn.addActionListener(e -> {
            int idx = viewDropdown.getSelectedIndex();
            runTableViewQuery(VIEW_QUERIES[idx]);
        });
    }

    //separate function for executing actions, SQL Exception if action is not successful (this is used for drop, create, populate)
    private void executeAction(DatabaseAction action, String successMsg) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            action.run(conn);
            outputArea.append(successMsg + "\n");
        } catch(Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
        }
    }

    //separate function for dropping tables, SQL Exception if dropping is not successful
    private void dropTables(Connection conn) throws SQLException {
        String[] tables = {"billing", "prescriptions", "appointments", "patients", "doctors"};
        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.executeUpdate("DROP TABLE " + table + " CASCADE CONSTRAINTS");
                    outputArea.append("Dropped table: " + table + "\n");
                } catch (SQLException ex) {
                    outputArea.append("Could not drop table " + table + " (may not exist): " + ex.getMessage() + "\n");
                }
            }
        }
    }
    //adding the tables and views, sql exception if creation is not successful
    private void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE patients (patient_id NUMBER(10) PRIMARY KEY, name VARCHAR2(120) NOT NULL, date_of_birth DATE NOT NULL, healthcard_no VARCHAR2(64) NOT NULL, address VARCHAR2(255), contact VARCHAR2(120), medical_history CLOB, CONSTRAINT uq_patients_healthcard UNIQUE (healthcard_no))");
        stmt.executeUpdate("CREATE TABLE doctors (doctor_id NUMBER(10) PRIMARY KEY, name VARCHAR2(120) NOT NULL, specialty VARCHAR2(120), contact VARCHAR2(120), availability VARCHAR2(255))");
        stmt.executeUpdate("CREATE TABLE appointments (appointment_id NUMBER(10) PRIMARY KEY, patient_id NUMBER(10) NOT NULL, doctor_id NUMBER(10) NOT NULL, appointment_date DATE NOT NULL, appointment_time INTERVAL DAY TO SECOND, reason VARCHAR2(255), CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id), CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id))");
        stmt.executeUpdate("CREATE TABLE prescriptions (prescription_id NUMBER(10) PRIMARY KEY, patient_id NUMBER(10) NOT NULL, doctor_id NUMBER(10) NOT NULL, medication VARCHAR2(120) NOT NULL, dosage VARCHAR2(60), date_prescribed DATE NOT NULL, CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id), CONSTRAINT fk_rx_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id))");
        stmt.executeUpdate("CREATE TABLE billing (bill_id NUMBER(10) PRIMARY KEY, patient_id NUMBER(10) NOT NULL, appointment_id NUMBER(10) NOT NULL, amount NUMBER(12,2) NOT NULL, payment_status VARCHAR2(20) NOT NULL, claim_amount NUMBER(12,2), CONSTRAINT fk_bill_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id), CONSTRAINT fk_bill_appt FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id), CONSTRAINT ck_billing_amount_pos CHECK (amount >= 0), CONSTRAINT ck_billing_claim_pos CHECK (claim_amount IS NULL OR claim_amount >= 0), CONSTRAINT ck_billing_status CHECK (payment_status IN ('PENDING','PAID','DECLINED')))");
        //views are added separately
        stmt.executeUpdate("CREATE OR REPLACE VIEW receptionist_appointments AS SELECT a.appointment_id AS \"Appointment ID\", a.appointment_date AS \"Date\", a.appointment_time AS \"Time\", p.patient_id AS \"Patient ID\", p.name AS \"Patient Name\", p.contact AS \"Patient Contact\", d.doctor_id AS \"Doctor ID\", d.name AS \"Doctor Name\", d.specialty AS \"Specialty\", a.reason AS \"Reason\" FROM appointments a INNER JOIN patients p ON a.patient_id = p.patient_id INNER JOIN doctors d ON a.doctor_id = d.doctor_id");
        stmt.executeUpdate("CREATE OR REPLACE VIEW billing_officer_reports AS SELECT b.bill_id AS \"Bill ID\", p.patient_id AS \"Patient ID\", p.name AS \"Patient Name\", p.healthcard_no AS \"Health Card\", p.contact AS \"Contact\", a.appointment_date AS \"Service Date\", b.amount AS \"Total Amount\", b.claim_amount AS \"Insurance Claim\", (b.amount - NVL(b.claim_amount, 0)) AS \"Patient Owes\", b.payment_status AS \"Payment Status\" FROM billing b INNER JOIN patients p ON b.patient_id = p.patient_id INNER JOIN appointments a ON b.appointment_id = a.appointment_id");
        stmt.executeUpdate("CREATE OR REPLACE VIEW doctor_patient_records AS SELECT p.patient_id AS \"Patient ID\", p.name AS \"Patient Name\", p.date_of_birth AS \"Date of Birth\", p.healthcard_no AS \"Health Card\", p.medical_history AS \"Medical History\", a.appointment_date AS \"Last Appointment\", a.reason AS \"Last Visit Reason\", pr.medication AS \"Current Medication\", pr.dosage AS \"Dosage\", pr.date_prescribed AS \"Prescription Date\", d.name AS \"Prescribing Doctor\" FROM patients p LEFT JOIN appointments a ON p.patient_id = a.patient_id LEFT JOIN prescriptions pr ON p.patient_id = pr.patient_id LEFT JOIN doctors d ON pr.doctor_id = d.doctor_id");
        stmt.close();
    }

    private void populateTables(Connection conn) throws SQLException {
    Statement stmt = conn.createStatement();
    
    //patients
    stmt.executeUpdate("INSERT INTO patients VALUES (1, 'John Doe', TO_DATE('1985-06-15','YYYY-MM-DD'), 'HC123456', '123 Main St, Toronto', '416-555-1111', 'Allergic to penicillin')");
    stmt.executeUpdate("INSERT INTO patients VALUES (2, 'Jane Smith', TO_DATE('1990-11-22','YYYY-MM-DD'), 'HC654321', '456 Queen St, Toronto', '416-555-2222', 'History of asthma')");
    stmt.executeUpdate("INSERT INTO patients VALUES (3, 'Ali Mirza', TO_DATE('1987-09-23', 'YYYY-MM-DD'), 'HC890123', '50 Gould St, Toronto', '647-999-0022', 'Diabetes Type 2')");
    stmt.executeUpdate("INSERT INTO patients VALUES (4, 'Jacob Mendoza', TO_DATE('2005-06-03', 'YYYY-MM-DD'), 'HC375647', '20 Bay St, Toronto', '647-877-1022', 'Chest Pain')");
    stmt.executeUpdate("INSERT INTO patients VALUES (5, 'Sarah Johnson', TO_DATE('1978-03-12', 'YYYY-MM-DD'), 'HC456789', '789 King St, Toronto', '416-555-3333', 'Hypertension')");
    stmt.executeUpdate("INSERT INTO patients VALUES (6, 'Michael Chen', TO_DATE('1995-08-20', 'YYYY-MM-DD'), 'HC789012', '321 Yonge St, Toronto', '647-555-4444', 'No significant history')");
    
    //doctors
    stmt.executeUpdate("INSERT INTO doctors VALUES (1, 'Dr. Alice Brown', 'Cardiology', 'alice.brown@hospital.ca', 'Mon-Fri 09:00-17:00')");
    stmt.executeUpdate("INSERT INTO doctors VALUES (2, 'Dr. Robert White', 'General Practitioner', 'robert.white@hospital.ca', 'Tue-Sat 10:00-18:00')");
    stmt.executeUpdate("INSERT INTO doctors VALUES (3, 'Dr. Emily Stone', 'Oncology', 'emily.stone@hospital.ca', 'Mon-Wed 08:00-16:00')");
    stmt.executeUpdate("INSERT INTO doctors VALUES (4, 'Dr. James Wilson', 'Pediatrics', 'james.wilson@hospital.ca', 'Mon-Fri 08:00-16:00')");
    
    //appointments
    stmt.executeUpdate("INSERT INTO appointments VALUES (1, 1, 1, TO_DATE('2025-11-01', 'YYYY-MM-DD'), INTERVAL '09:00:00' HOUR TO SECOND, 'Annual checkup')");
    stmt.executeUpdate("INSERT INTO appointments VALUES (2, 2, 2, TO_DATE('2025-11-02', 'YYYY-MM-DD'), INTERVAL '10:30:00' HOUR TO SECOND, 'Asthma follow up')");
    stmt.executeUpdate("INSERT INTO appointments VALUES (3, 3, 3, TO_DATE('2025-11-03', 'YYYY-MM-DD'), INTERVAL '11:15:00' HOUR TO SECOND, 'Diabetes review')");
    stmt.executeUpdate("INSERT INTO appointments VALUES (4, 4, 4, TO_DATE('2025-11-05', 'YYYY-MM-DD'), INTERVAL '14:00:00' HOUR TO SECOND, 'Chest pain evaluation')");
    stmt.executeUpdate("INSERT INTO appointments VALUES (5, 5, 1, TO_DATE('2025-11-06', 'YYYY-MM-DD'), INTERVAL '15:45:00' HOUR TO SECOND, 'Hypertension consultation')");
    
    //prescriptions
    stmt.executeUpdate("INSERT INTO prescriptions VALUES (1, 1, 1, 'Atorvastatin', '10mg', TO_DATE('2025-11-01', 'YYYY-MM-DD'))");
    stmt.executeUpdate("INSERT INTO prescriptions VALUES (2, 2, 2, 'Ventolin', '2 puffs', TO_DATE('2025-11-02', 'YYYY-MM-DD'))");
    stmt.executeUpdate("INSERT INTO prescriptions VALUES (3, 3, 3, 'Metformin', '500mg', TO_DATE('2025-11-03', 'YYYY-MM-DD'))");
    stmt.executeUpdate("INSERT INTO prescriptions VALUES (4, 4, 4, 'Ibuprofen', '400mg', TO_DATE('2025-11-05', 'YYYY-MM-DD'))");
    stmt.executeUpdate("INSERT INTO prescriptions VALUES (5, 5, 1, 'Lisinopril', '20mg', TO_DATE('2025-11-06', 'YYYY-MM-DD'))");
    
    //billing
    stmt.executeUpdate("INSERT INTO billing VALUES (1, 1, 1, 110.00, 'PAID', 100.00)");
    stmt.executeUpdate("INSERT INTO billing VALUES (2, 2, 2, 150.00, 'PENDING', 0.00)");
    stmt.executeUpdate("INSERT INTO billing VALUES (3, 3, 3, 200.00, 'PAID', 200.00)");
    stmt.executeUpdate("INSERT INTO billing VALUES (4, 4, 4, 90.00, 'DECLINED', NULL)");
    stmt.executeUpdate("INSERT INTO billing VALUES (5, 5, 5, 120.00, 'PAID', 120.00)");
    
    stmt.close();
}

    private void runTableViewQuery(String query) {
        outputArea.setText(""); //clear output
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query); //run query
            ResultSetMetaData meta = rs.getMetaData(); //get columns
            int colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++) {
                outputArea.append(meta.getColumnName(i) + "\t"); //print header
            }
            outputArea.append("\n");
            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    outputArea.append(rs.getString(i) + "\t"); //print cell
                }
                outputArea.append("\n");
            }
            rs.close();
        } catch (Exception ex) {
            outputArea.append("Query Error: " + ex.getMessage() + "\n"); //show error if it doesnt run
        }
    }

    @FunctionalInterface
    interface DatabaseAction {
        void run(Connection conn) throws SQLException; //action callback
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new dbmcis().setVisible(true)); //to start gui
    }

}
