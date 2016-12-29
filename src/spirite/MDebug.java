// Rory Burks 2015

package spirite;

import javax.swing.JOptionPane;

public class MDebug {
    public static void handleError( int priority, String message) {
        JOptionPane.showMessageDialog(null, "MDB: " + message);
    }

    public static void handleError( int priority, String message, Exception e) {
        JOptionPane.showMessageDialog(null, "MDB: " + message);
    }

    public static void handleError( int priority, Exception e) {
        JOptionPane.showMessageDialog(null, "MDB: " + e.getCause() + "/n" + e.getMessage() + "/n" + e.toString());
    }

    public static void note( String message) {
        JOptionPane.showMessageDialog(null, message);
    }
}
