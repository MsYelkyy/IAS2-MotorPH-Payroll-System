package motorph.ui.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

import java.util.Map;

/** Manages theme (light/dark) and language (English/Filipino) state across page transitions. */
public final class ThemeManager {

    private static ThemeManager instance;

    private String currentTheme = "Dark";
    private String currentLanguage = "English";

    private static final Map<String, String> FILIPINO_LABELS = Map.ofEntries(
            // ── Login & Auth ───────────────────────────────────────────
            Map.entry("Welcome Back", "Maligayang Pagbabalik"),
            Map.entry("Sign in to continue to your employee workspace.", "Mag-sign in upang magpatuloy sa inyong workspace."),
            Map.entry("Company Email", "Email ng Kumpanya"),
            Map.entry("Password", "Password"),
            Map.entry("Remember me", "Tandaan ako"),
            Map.entry("Forgot Password", "Nakalimutan ang Password"),
            Map.entry("Sign In", "Mag-sign In"),
            Map.entry("Protected by Two-Factor Authentication", "Protektado ng Two-Factor Authentication"),
            Map.entry("Show", "Ipakita"),
            Map.entry("Hide", "Itago"),
            // ── OTP & Password Reset ───────────────────────────────────
            Map.entry("Verify OTP", "I-verify ang OTP"),
            Map.entry("Enter the verification code sent to your registered email address.", "Ilagay ang verification code na ipinadala sa inyong registered email."),
            Map.entry("OTP Code", "OTP Code"),
            Map.entry("Verify and Continue", "I-verify at Magpatuloy"),
            Map.entry("← Back to Login", "← Bumalik sa Login"),
            Map.entry("Reset Password", "I-reset ang Password"),
            Map.entry("Enter your email to receive a reset OTP.", "Ilagay ang inyong email para makatanggap ng reset OTP."),
            Map.entry("Email", "Email"),
            Map.entry("Send Reset OTP", "Ipadala ang Reset OTP"),
            Map.entry("Verify Reset OTP", "I-verify ang Reset OTP"),
            Map.entry("Enter the reset code sent to your registered email address.", "Ilagay ang reset code na ipinadala sa inyong registered email."),
            Map.entry("← Back to Email", "← Bumalik sa Email"),
            Map.entry("Create New Password", "Lumikha ng Bagong Password"),
            Map.entry("Use at least 8 characters.", "Gumamit ng hindi bababa sa 8 na karakter."),
            Map.entry("New Password", "Bagong Password"),
            Map.entry("Confirm Password", "Kumpirmahin ang Password"),
            Map.entry("Update Password", "I-update ang Password"),
            // ── Navigation ─────────────────────────────────────────────
            Map.entry("Dashboard", "Dashboard"),
            Map.entry("Profile", "Profil"),
            Map.entry("Attendance", "Presensya"),
            Map.entry("Payslip / Payroll", "Payslip / Payroll"),
            Map.entry("Account Settings", "Mga Setting ng Account"),
            Map.entry("Logout", "Mag-logout"),
            Map.entry("EMPLOYEE", "EMPLEYADO"),
            Map.entry("Operations", "Operasyon"),
            // ── Dashboard labels ───────────────────────────────────────
            Map.entry("EMPLOYEE WORKSPACE", "WORKSPACE NG EMPLEYADO"),
            Map.entry("TOTAL NET PAY", "KABUUANG NET PAY"),
            Map.entry("ATTENDANCE RATE", "RATE NG PRESENSYA"),
            Map.entry("DAYS WORKED", "MGA ARAW NA NAGTRABAHO"),
            Map.entry("AVAILABLE LEAVE", "NALALABAS NA LEAVE"),
            Map.entry("PENDING REQUESTS", "MGA KAHILINGANG NAGHIHINTAY"),
            Map.entry("NET PAY TREND", "TREND NG NET PAY"),
            Map.entry("EARNINGS BREAKDOWN", "BREAKDOWN NG KITA"),
            Map.entry("ATTENDANCE SUMMARY", "BUOD NG PRESENSYA"),
            Map.entry("LEAVE SUMMARY", "BUOD NG LEAVE"),
            Map.entry("Vacation Leave", "Vacation Leave"),
            Map.entry("Sick Leave", "Sick Leave"),
            Map.entry("Emergency Leave", "Emergency Leave"),
            Map.entry("Special Leave", "Espesyal na Leave"),
            Map.entry("Maternity Leave", "Maternity Leave"),
            Map.entry("UPCOMING PAYDAY", "DARATING NA SAHOD"),
            Map.entry("In 26 days", "Sa loob ng 26 na araw"),
            Map.entry("RECENT PAYSLIP", "PINAKABAGONG PAYSLIP"),
            Map.entry("ANNOUNCEMENTS", "MGA ANUNSYO"),
            Map.entry("Company Meeting", "Pulong ng Kumpanya"),
            Map.entry("All employees are expected to attend.", "Lahat ng empleyado ay inaasahang dadalo."),
            Map.entry("Active Session", "Aktibong Session"),
            Map.entry("View Payslip", "Tingnan ang Payslip"),
            Map.entry("Welcome to your employee workspace.", "Maligayang pagdating sa inyong workspace."),
            Map.entry("Employee workspace is ready.", "Handa na ang workspace ng empleyado."),
            Map.entry("Please log in before using this action.", "Mangyaring mag-login bago gamitin ang aksyong ito."),
            // ── Attendance view ────────────────────────────────────────
            Map.entry("Attendance Logs", "Mga Talaan ng Presensya"),
            Map.entry("Work Date", "Petsa ng Trabaho"),
            Map.entry("Time In", "Pasok"),
            Map.entry("Time Out", "Labas"),
            Map.entry("Total Hours", "Kabuuang Oras"),
            Map.entry("Status", "Katayuan"),
            Map.entry("Pending", "Naghihintay"),
            // ── Leave view ─────────────────────────────────────────────
            Map.entry("Leave Request", "Kahilingang Leave"),
            Map.entry("Leave Type", "Uri ng Leave"),
            Map.entry("Available Credits", "Nalalabas na Credits"),
            Map.entry("Start Date", "Petsa ng Simula"),
            Map.entry("End Date", "Petsa ng Katapusan"),
            Map.entry("Leave Hours", "Oras ng Leave"),
            Map.entry("Reason", "Dahilan"),
            Map.entry("Submit Leave Request", "Isumite ang Kahilingang Leave"),
            Map.entry("Leave History", "Kasaysayan ng Leave"),
            Map.entry("Pending requests can be cancelled. Approved requests consume leave credits.", "Ang mga naghihintay na kahilingan ay maaaring kanselahin. Ang mga naaprubahang kahilingan ay kumukuha ng leave credits."),
            Map.entry("Cancel Latest Pending Request", "Kanselahin ang Pinakabagong Kahilingan"),
            // ── Payslip view ───────────────────────────────────────────
            Map.entry("Payslip Preview", "Preview ng Payslip"),
            Map.entry("Gross Pay:", "Gross Pay:"),
            Map.entry("Deductions:", "Mga Bawas:"),
            Map.entry("Net Pay:", "Net Pay:"),
            Map.entry("Search", "Hanapin"),
            Map.entry("Preview Payslip", "I-preview ang Payslip"),
            Map.entry("Export Text Payslip", "I-export ang Payslip"),
            Map.entry("Print Payslip", "I-print ang Payslip"),
            Map.entry("Employee can only view their own persisted payroll data.", "Ang empleyado ay makakakita lamang ng sariling payroll data."),
            // ── Account Settings ───────────────────────────────────────
            Map.entry("Email is view-only and remains the login identity.", "Ang email ay para sa pagtingin lamang at nananatiling login identity."),
            Map.entry("Change Password", "Palitan ang Password"),
            // ── OTP footer ─────────────────────────────────────────────
            Map.entry("MotorPH Payroll Management System  ·  v1.0", "MotorPH Payroll Management System  ·  v1.0")
    );

    // singleton — no external instantiation
    private ThemeManager() {
    }

    // returns the shared ThemeManager instance
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // getters
    public String getCurrentTheme() {
        return currentTheme;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    // applies the theme setting to the root node and stores it as current
    public void setTheme(String mode, Node root) {
        this.currentTheme = mode == null ? "Dark" : mode;
        applyThemeToRoot(this.currentTheme, root);
    }

    // applies the language setting to the root node and stores it as current
    public void setLanguage(String lang, Node root) {
        this.currentLanguage = lang == null ? "English" : lang;
        applyLanguageToRoot(this.currentLanguage, root);
    }

    // applies the stored theme and language to a newly loaded root node
    public void applyCurrentTo(Node root) {
        applyThemeToRoot(currentTheme, root);
        applyLanguageToRoot(currentLanguage, root);
    }

    // adds or removes the light-mode CSS class based on the theme mode
    private void applyThemeToRoot(String mode, Node root) {
        if (root == null) return;
        if ("Light".equals(mode)) {
            if (!root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().add("light-mode");
            }
        } else {
            root.getStyleClass().remove("light-mode");
        }
    }

    // applies Filipino or English labels to all labeled nodes in the tree
    private void applyLanguageToRoot(String lang, Node root) {
        if (root == null) return;
        boolean filipino = "Filipino".equals(lang);
        applyLanguageToNode(root, filipino);
    }

    // recursively applies language to a node and its children
    private void applyLanguageToNode(Node node, boolean filipino) {
        if (node == null) return;

        if (node instanceof Label labeled) {
            Object stored = labeled.getProperties().get("englishText");
            if (stored == null) {
                stored = labeled.getText() == null ? "" : labeled.getText();
                labeled.getProperties().put("englishText", stored);
            }
            String english = stored.toString();
            labeled.setText(filipino ? translateText(english) : english);

            Object storedWrap = labeled.getProperties().get("englishWrap");
            if (storedWrap == null) {
                labeled.getProperties().put("englishWrap", labeled.isWrapText());
            }
            labeled.setWrapText(filipino || (boolean) labeled.getProperties().get("englishWrap"));
        }

        if (node instanceof TextInputControl input) {
            Object storedPrompt = input.getProperties().get("englishPrompt");
            if (storedPrompt == null) {
                storedPrompt = input.getPromptText() == null ? "" : input.getPromptText();
                input.getProperties().put("englishPrompt", storedPrompt);
            }
            String english = storedPrompt.toString();
            input.setPromptText(filipino ? translateText(english) : english);
        }

        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> applyLanguageToNode(child, filipino));
        }
    }

    // looks up a Filipino translation, falling back to the original text
    private String translateText(String english) {
        if (english == null) return "";
        String direct = FILIPINO_LABELS.get(english);
        if (direct != null) return direct;
        String key = english.trim();
        for (Map.Entry<String, String> e : FILIPINO_LABELS.entrySet()) {
            if (e.getKey().trim().equalsIgnoreCase(key)) return e.getValue();
        }
        return english;
    }
}
