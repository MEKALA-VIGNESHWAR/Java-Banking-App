import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Backend {
    // MAIN DB CONFIG - change these to your environment
    private static final String URL = "jdbc:mysql://localhost:3306/login";
    private static final String USER = "root";
    private static final String PASS = "Chintu@44";

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public boolean authenticate(String email, String password) {
        String sql = "SELECT id FROM customers WHERE user_email = ? AND password = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int getCustomerId(String email) {
        String sql = "SELECT id FROM customers WHERE user_email = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return -1;
    }

    public boolean registerCustomer(String name, String email, String password) {
        String sql = "INSERT INTO customers (full_name, user_email, password, created_at) VALUES (?, ?, ?, NOW())";
        String insertAccount = "INSERT INTO accounts (user_id, balance) VALUES (?, 0)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            int r = ps.executeUpdate();
            if (r == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    try (PreparedStatement ps2 = c.prepareStatement(insertAccount)) {
                        ps2.setInt(1, id);
                        ps2.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
        return false;
    }

    public double checkBalance(int customerId) {
        String sql = "SELECT balance FROM accounts WHERE user_id = ? ORDER BY id LIMIT 1";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return 0.0;
    }

    public boolean deposit(double amount, int customerId) {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE user_id = ?";
        // Added 'type' to match your DESC TRANSACTIONS output
        String trx = "INSERT INTO transactions (from_account_id, to_account_id, amount, type, timestamp) " +
                "VALUES (NULL, (SELECT id FROM accounts WHERE user_id=? LIMIT 1), ?, 'Deposit', NOW())";
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps2 = c.prepareStatement(trx)) {
                ps2.setInt(1, customerId);
                ps2.setDouble(2, amount);
                ps2.executeUpdate();
            }
            c.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean withdraw(int customerId, double amount) {
        // Use FOR UPDATE to lock the row during the check
        String check = "SELECT balance FROM accounts WHERE user_id = ? FOR UPDATE";
        String upd = "UPDATE accounts SET balance = balance - ? WHERE user_id = ?";
        // FIXED: Included 'type' column to match your table description
        String trx = "INSERT INTO transactions (from_account_id, to_account_id, amount, type, timestamp) " +
                "VALUES ((SELECT id FROM accounts WHERE user_id=? LIMIT 1), NULL, ?, 'Withdraw', NOW())";

        try (Connection c = conn()) {
            c.setAutoCommit(false);

            try (PreparedStatement psCheck = c.prepareStatement(check)) {
                psCheck.setInt(1, customerId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        double bal = rs.getDouble("balance");
                        if (bal < amount) {
                            c.rollback();
                            return false;
                        }
                    } else {
                        c.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement psUpd = c.prepareStatement(upd)) {
                psUpd.setDouble(1, amount);
                psUpd.setInt(2, customerId);
                psUpd.executeUpdate();
            }

            try (PreparedStatement ps3 = c.prepareStatement(trx)) {
                ps3.setInt(1, customerId);
                ps3.setDouble(2, amount);
                ps3.executeUpdate();
            }

            c.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean transfer(int fromCustomerId, int toCustomerId, double amount) {
        String checkFrom = "SELECT balance FROM accounts WHERE user_id = ? FOR UPDATE";
        String updateFrom = "UPDATE accounts SET balance = balance - ? WHERE user_id = ?";
        String updateTo = "UPDATE accounts SET balance = balance + ? WHERE user_id = ?";
        // Standardized with your other methods
        String trx = "INSERT INTO transactions (from_account_id, to_account_id, amount, type, timestamp) " +
                "VALUES ((SELECT id FROM accounts WHERE user_id=? LIMIT 1), " +
                "(SELECT id FROM accounts WHERE user_id=? LIMIT 1), ?, 'Transfer', NOW())";

        try (Connection c = conn()) {
            c.setAutoCommit(false);

            // 1. Check sender balance
            try (PreparedStatement ps = c.prepareStatement(checkFrom)) {
                ps.setInt(1, fromCustomerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getDouble("balance") < amount) {
                        c.rollback();
                        return false;
                    }
                }
            }

            // 2. Deduct from sender
            try (PreparedStatement ps1 = c.prepareStatement(updateFrom)) {
                ps1.setDouble(1, amount);
                ps1.setInt(2, fromCustomerId);
                ps1.executeUpdate();
            }

            // 3. Add to receiver
            try (PreparedStatement ps2 = c.prepareStatement(updateTo)) {
                ps2.setDouble(1, amount);
                ps2.setInt(2, toCustomerId);
                if (ps2.executeUpdate() == 0) { // Receiver ID doesn't exist
                    c.rollback();
                    return false;
                }
            }

            // 4. Record transaction
            try (PreparedStatement ps3 = c.prepareStatement(trx)) {
                ps3.setInt(1, fromCustomerId);
                ps3.setInt(2, toCustomerId);
                ps3.setDouble(3, amount);
                ps3.executeUpdate();
            }

            c.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<String> recentTransactions(int customerId, int limit) {
        List<String> list = new ArrayList<>();
        // Updated query to use the 'type' column directly for cleaner code
        String sql = "SELECT amount, type, timestamp FROM transactions t " +
                "JOIN accounts a ON (t.from_account_id = a.id OR t.to_account_id = a.id) " +
                "WHERE a.user_id = ? ORDER BY t.timestamp DESC LIMIT ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double amt = rs.getDouble("amount");
                    String type = rs.getString("type");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    list.add(String.format("%s: ₹%.2f  •  %s", type, amt, ts.toString()));
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return list;
    }
}
