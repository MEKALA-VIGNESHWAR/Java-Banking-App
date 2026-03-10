import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * PACEPAY - Main Application File
 * Main Entry Point: Login class
 */
public class Login extends JFrame {
    private final RoundedTextField emailField = new RoundedTextField(20);
    private final RoundedPasswordField passField = new RoundedPasswordField(20);

    public Login() {
        setTitle("PacePay - Sign in");
        setSize(840, 560);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // --- Left Panel: Branding ---
        JPanel left = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 35),
                        getWidth(), getHeight(), new Color(15, 15, 55));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        left.setPreferredSize(new Dimension(420, 560));
        left.setLayout(new GridBagLayout());

        JLabel brand = new JLabel("<html><div style='text-align:center'>" +
                "<span style='font-size:26px;font-weight:bold;color:#00BFFF'>PacePay</span><br>" +
                "<span style='font-size:12px;color:#B0C4DE'>Fast • Simple • Secure</span></div></html>");
        left.add(brand);

        // --- Right Panel: Login Form ---
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(new Color(20, 20, 40));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(new Color(28, 28, 44));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(45, 45, 70), 1, 18),
                BorderFactory.createEmptyBorder(20, 24, 24, 24)));
        card.setPreferredSize(new Dimension(360, 380));

        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        styleField(emailField, "Email address");
        styleField(passField, "Password");

        RoundedButton loginBtn = new RoundedButton("Sign in", 14);
        loginBtn.setBackground(new Color(0, 150, 255));
        loginBtn.addActionListener(e -> {
            Backend backend = new Backend();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword()).trim();

            if (isPlaceholder(emailField, "Email address") || isPlaceholder(passField, "Password")
                    || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter your email and password", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (backend.authenticate(email, pass)) {
                int id = backend.getCustomerId(email);
                dispose();
                new BankMenu(email, id);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials", "Login failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton toRegister = new JButton("Create account");
        toRegister.setBorderPainted(false);
        toRegister.setFocusPainted(false);
        toRegister.setContentAreaFilled(false);
        toRegister.setForeground(new Color(0, 180, 255));
        toRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toRegister.addActionListener(e -> {
            dispose();
            new RegisterForm();
        });

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 0, 8, 0);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.CENTER;
        card.add(title, g);

        g.gridy++; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1.0;
        card.add(emailField, g);
        g.gridy++;
        card.add(passField, g);

        g.gridy++; g.insets = new Insets(14, 0, 6, 0);
        card.add(loginBtn, g);

        g.gridy++; g.insets = new Insets(2, 0, 0, 0);
        card.add(toRegister, g);

        right.add(card, new GridBagConstraints());
        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);

        setVisible(true);
    }

    private void styleField(JTextField f, String placeholder) {
        f.setForeground(new Color(255, 255, 255, 180));
        f.setText(placeholder);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(320, 42));

        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(Color.WHITE);
                    if (f instanceof JPasswordField) ((JPasswordField) f).setEchoChar('•');
                }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setForeground(new Color(255, 255, 255, 180));
                    f.setText(placeholder);
                    if (f instanceof JPasswordField) ((JPasswordField) f).setEchoChar((char) 0);
                }
            }
        });
        if (f instanceof JPasswordField) ((JPasswordField) f).setEchoChar((char) 0);
    }

    private boolean isPlaceholder(JTextField field, String placeholder) {
        return field.getText().equals(placeholder) && field.getForeground().equals(new Color(255, 255, 255, 180));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Login::new);
    }
}

// --- Bank Dashboard ---

class BankMenu extends JFrame {
    private final int currentUserId;
    private final String currentUsername;
    private JLabel balanceLabel;
    private JPanel historyPanel;

    // Theme Colors
    private final Color BG_DARK = new Color(13, 13, 25);
    private final Color CARD_BG = new Color(22, 22, 40);
    private final Color ACCENT_BLUE = new Color(0, 163, 255);
    private final Color TEXT_WHITE = new Color(240, 240, 245);
    private final Color TEXT_GRAY = new Color(150, 150, 170);

    public BankMenu(String username, int customerId) {
        this.currentUserId = customerId;
        this.currentUsername = username;

        setTitle("PacePay Premium");
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // --- 1. SIDE NAVIGATION BAR ---
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(18, 18, 35));
        sidebar.setPreferredSize(new Dimension(220, 650));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(40, 40, 60)));

        JLabel logo = new JLabel("PacePay");
        logo.setFont(new Font("SansSerif", Font.BOLD, 24));
        logo.setForeground(ACCENT_BLUE);
        logo.setBorder(BorderFactory.createEmptyBorder(30, 25, 50, 25));

        sidebar.add(logo);
        sidebar.add(createNavButton("📊 Dashboard", true));
        sidebar.add(createNavButton("💳 My Cards", false));
        sidebar.add(createNavButton("📈 Investing", false));
        sidebar.add(createNavButton("⚙️ Settings", false));

        sidebar.add(Box.createVerticalGlue());

        RoundedButton logout = new RoundedButton("Logout", 10);
        logout.setBackground(new Color(40, 40, 60));
        logout.addActionListener(e -> { dispose(); new Login(); });
        JPanel logoutContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoutContainer.setOpaque(false);
        logoutContainer.add(logout);
        sidebar.add(logoutContainer);
        sidebar.add(Box.createVerticalStrut(20));

        // --- 2. MAIN CONTENT AREA ---
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel welcome = new JLabel("Welcome back, " + currentUsername);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcome.setForeground(TEXT_WHITE);
        header.add(welcome, BorderLayout.WEST);

        // Top Cards Grid (Balance & Actions)
        JPanel topGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        topGrid.setOpaque(false);
        topGrid.setPreferredSize(new Dimension(0, 180));

        // Balance Card
        JPanel balCard = createStyledCard();
        balCard.setLayout(new BorderLayout());
        JLabel balTitle = new JLabel("Total Balance");
        balTitle.setForeground(TEXT_GRAY);
        balanceLabel = new JLabel("₹0.00");
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        balanceLabel.setForeground(TEXT_WHITE);

        balCard.add(balTitle, BorderLayout.NORTH);
        balCard.add(balanceLabel, BorderLayout.CENTER);

        // Quick Actions Card
        JPanel actionCard = createStyledCard();
        actionCard.setLayout(new GridLayout(1, 3, 15, 0));
        actionCard.add(createIconButton("💰", "Deposit", e -> doDeposit()));
        actionCard.add(createIconButton("💸", "Withdraw", e -> doWithdraw()));
        actionCard.add(createIconButton("🔁", "Transfer", e -> doTransfer()));

        topGrid.add(balCard);
        topGrid.add(actionCard);

        // History Section (Bottom)
        JPanel bottomSection = createStyledCard();
        bottomSection.setLayout(new BorderLayout());
        JLabel histTitle = new JLabel("Recent Activity");
        histTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        histTitle.setForeground(TEXT_WHITE);
        histTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        historyPanel = new JPanel();
        historyPanel.setOpaque(false);
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(historyPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        bottomSection.add(histTitle, BorderLayout.NORTH);
        bottomSection.add(scroll, BorderLayout.CENTER);

        // Assemble Main Content
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 25));
        centerWrapper.setOpaque(false);
        centerWrapper.add(topGrid, BorderLayout.NORTH);
        centerWrapper.add(bottomSection, BorderLayout.CENTER);

        mainContent.add(header, BorderLayout.NORTH);
        mainContent.add(centerWrapper, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);

        updateBalanceDisplay();
        updateTransactions();
        setVisible(true);
    }

    private JPanel createStyledCard() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(45, 45, 70), 1, 20),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        return p;
    }

    private JButton createNavButton(String text, boolean active) {
        JButton b = new JButton(text);
        b.setForeground(active ? ACCENT_BLUE : TEXT_GRAY);
        b.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 15));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 0));
        return b;
    }

    private JPanel createIconButton(String icon, String label, ActionListener action) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JButton b = new JButton(icon);
        b.setFont(new Font("SansSerif", Font.PLAIN, 30));
        b.setForeground(ACCENT_BLUE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);

        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setForeground(TEXT_WHITE);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));

        p.add(b, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    private void updateBalanceDisplay() {
        double bal = new Backend().checkBalance(currentUserId);
        balanceLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(bal));
    }

    private void updateTransactions() {
        List<String> logs = new Backend().recentTransactions(currentUserId, 6);
        historyPanel.removeAll();
        for (String log : logs) {
            JPanel item = new JPanel(new BorderLayout());
            item.setOpaque(false);
            item.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

            JLabel txt = new JLabel(log);
            txt.setForeground(TEXT_WHITE);

            // Add a separator line
            item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 60)));
            item.add(txt, BorderLayout.CENTER);

            historyPanel.add(item);
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    // --- Action Methods ---
    private void doDeposit() {
        String s = JOptionPane.showInputDialog(this, "Amount to Deposit:");
        if (s != null) {
            if (new Backend().deposit(Double.parseDouble(s), currentUserId)) {
                updateBalanceDisplay(); updateTransactions();
            }
        }
    }

    private void doWithdraw() {
        String s = JOptionPane.showInputDialog(this, "Amount to Withdraw:");
        if (s != null) {
            if (new Backend().withdraw(currentUserId, Double.parseDouble(s))) {
                updateBalanceDisplay(); updateTransactions();
            } else JOptionPane.showMessageDialog(this, "Insufficient Funds");
        }
    }

    private void doTransfer() {
        String id = JOptionPane.showInputDialog(this, "Recipient ID:");
        String amt = JOptionPane.showInputDialog(this, "Amount:");
        if (id != null && amt != null) {
            if (new Backend().transfer(currentUserId, Integer.parseInt(id), Double.parseDouble(amt))) {
                updateBalanceDisplay(); updateTransactions();
            } else JOptionPane.showMessageDialog(this, "Transfer Failed");
        }
    }
}

// --- Registration Form ---
class RegisterForm extends JFrame {
    private final RoundedTextField nameField = new RoundedTextField(20);
    private final RoundedTextField emailField = new RoundedTextField(20);
    private final RoundedPasswordField passField = new RoundedPasswordField(20);

    public RegisterForm() {
        setTitle("PacePay - Register");
        setSize(840, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(20, 20, 40));
        setContentPane(root);

        // Styling and adding fields (simplified for flow)
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.gridx = 0; g.gridy = 0;

        root.add(new JLabel("<html><span style='color:white'>Full Name:</span></html>"), g);
        g.gridy++; root.add(nameField, g);
        g.gridy++; root.add(new JLabel("<html><span style='color:white'>Email:</span></html>"), g);
        g.gridy++; root.add(emailField, g);
        g.gridy++; root.add(new JLabel("<html><span style='color:white'>Password:</span></html>"), g);
        g.gridy++; root.add(passField, g);

        RoundedButton regBtn = new RoundedButton("Create Account", 14);
        regBtn.setBackground(new Color(0, 150, 255));
        regBtn.addActionListener(e -> {
            if (new Backend().registerCustomer(nameField.getText(), emailField.getText(), new String(passField.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Account Created!");
                dispose(); new Login();
            } else JOptionPane.showMessageDialog(this, "Registration Failed");
        });

        g.gridy++; root.add(regBtn, g);
        setVisible(true);
    }
}

// --- Helper UI Components ---
class RoundedBorder extends AbstractBorder {
    private Color color; private int thickness; private int radius;
    public RoundedBorder(Color c, int t, int r) { this.color = c; this.thickness = t; this.radius = r; }
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
}

class RoundedButton extends JButton {
    private int radius;
    public RoundedButton(String label, int r) {
        super(label); this.radius = r;
        setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
        setForeground(Color.WHITE);
        setPreferredSize(new Dimension(150, 40));
    }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        super.paintComponent(g);
        g2.dispose();
    }
}

class RoundedTextField extends JTextField {
    public RoundedTextField(int cols) { super(cols); setOpaque(false); setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); fColor(); }
    private void fColor() { setForeground(Color.WHITE); setCaretColor(Color.WHITE); }
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(35, 35, 50));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        super.paintComponent(g);
    }
}

class RoundedPasswordField extends JPasswordField {
    public RoundedPasswordField(int cols) { super(cols); setOpaque(false); setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); fColor(); }
    private void fColor() { setForeground(Color.WHITE); setCaretColor(Color.WHITE); }
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(35, 35, 50));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        super.paintComponent(g);
    }
}