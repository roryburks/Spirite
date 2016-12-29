// Rory Burks

package spirite.dialogs;

import java.awt.Color;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

public class Dialogs {

    /**
     * Pops up the color picker dialog and queries it for a color
     * @return The color picked or null if they canceled
     */
    public static Color pickColor() {
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(null, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            return jcp.getColor();
        }

        return null;
    }
    
}
