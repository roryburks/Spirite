package spirite.pc.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;

import spirite.base.brains.MasterControl;

/**
 *
 * @author Rory Burks
 */
public class NewImagePanel extends javax.swing.JPanel 
    implements ActionListener
{
	private static final long serialVersionUID = 1L;
	MasterControl master;
    MouseListener textfieldMouseListener;
    ColorComboModel comboModel;
    Color selected_color;

    // :::: External Interaction
    public int getValueWidth() {
        return Integer.parseInt(jtfWidth.getText());
    }
    public int getValueHeight() {
        return Integer.parseInt(jtfHeight.getText());
    }
    public Color getValueColor() {
        return selected_color;
    }


    // :::: Construction
    public NewImagePanel( MasterControl master) {
        this.master = master;
        textfieldMouseListener = createTextfieldMouseListener();

        initComponents();

        comboModel = new ColorComboModel();
        colorCombo.setRenderer( comboModel.renderer);
        colorCombo.addActionListener(this);

        selected_color = (Color)colorCombo.getSelectedItem();
        colorPanel.setBackground(selected_color);

        jtfWidth.setText( Integer.toString(master.getSettingsManager().getDefaultWidth()));
        jtfHeight.setText( Integer.toString(master.getSettingsManager().getDefaultHeight()));
    }


    private JFormattedTextField NumberFormattedTextField(boolean width) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setParseIntegerOnly(true);
        format.setGroupingUsed(false);
        JFormattedTextField jtf = new JFormattedTextField(format);
//        jtf.addMouseListener(textfieldMouseListener);
        jtf.setColumns(10);
//        jtf.addPropertyChangeListener(this);

        return (JFormattedTextField)jtf;
    }

    // Creates a Mouse Listener to make sure when you click on the Text Field
    //  it properly focuses the Carat.  Because without the added listener
    //  sometimes it works and sometimes it doesn't.
    private MouseListener createTextfieldMouseListener() {
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JTextField tf = (JTextField)e.getSource();
                        int offset = tf.viewToModel(e.getPoint());
                        tf.setCaretPosition(offset);
                    }
                });
            }
        };


        return ml;
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jtfWidth = NumberFormattedTextField(true);
        jLabel3 = new javax.swing.JLabel();
        jtfHeight = NumberFormattedTextField(false);
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        colorPanel = new javax.swing.JPanel();
        colorCombo = new javax.swing.JComboBox<>();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel1.setText("Image Size");

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel2.setText("Width:");
        jLabel2.setPreferredSize(new java.awt.Dimension(52, 15));

        jtfWidth.setText("0");
        jtfWidth.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setText("Height:");
        jLabel3.setPreferredSize(new java.awt.Dimension(52, 15));

        jtfHeight.setText("0");
        jtfHeight.setPreferredSize(new java.awt.Dimension(80, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtfWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtfHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel4.setText("Background Color:");

        colorPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        colorPanel.setPreferredSize(new java.awt.Dimension(40, 40));

        javax.swing.GroupLayout colorPanelLayout = new javax.swing.GroupLayout(colorPanel);
        colorPanel.setLayout(colorPanelLayout);
        colorPanelLayout.setHorizontalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );
        colorPanelLayout.setVerticalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 36, Short.MAX_VALUE)
        );

        colorCombo.setModel(new ColorComboModel());

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addComponent(colorCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(colorCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(25, 25, 25))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(64, Short.MAX_VALUE))
        );
    }// </editor-fold>                        


    // Variables declaration - do not modify                     
    private javax.swing.JComboBox<Color> colorCombo;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTextField jtfHeight;
    private javax.swing.JTextField jtfWidth;
    // End of variables declaration                   

    @Override
    public void actionPerformed(ActionEvent e) {
        if( e.getSource() == colorCombo) {
            selected_color = (Color)colorCombo.getSelectedItem();
            colorPanel.setBackground(selected_color);
        }
    }
}


class ColorComboModel extends AbstractListModel<Color>
        implements ComboBoxModel<Color>
{
	private static final long serialVersionUID = 1L;
	Color[] Colors = {
        new Color( 0, 0, 0, 0),
        new Color( 0, 0, 0, 255),
        new Color( 255, 255, 255, 255),
        new Color( 125, 125, 125, 255),
    };
    String[] ColorNames = {
        "Transparent",
        "Black",
        "White",
        "Costum Color"
    };

    CCM_Renderer renderer;


    int selected = 0;

    class CCM_Renderer extends JPanel
      implements ListCellRenderer<Color>
    {
		private static final long serialVersionUID = 1L;
		JLabel jlabel;
        JPanel color_panel, null_panel;

        CCM_Renderer( ) {
            initComponents();
        }

        private void initComponents() {
            jlabel = new JLabel();
//            jlabel.setText(ColorNames[index]);

            color_panel = new JPanel();
            color_panel.setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());
            color_panel.setMaximumSize(new Dimension(64,18));

            null_panel = new JPanel();

            setLayout( new BoxLayout(this, BoxLayout.LINE_AXIS));
            add(jlabel);
            add(null_panel);
            add(color_panel);
        }

        @Override
        public Component getListCellRendererComponent(
        		JList<? extends Color> list, 
        		Color value, 
        		int index, 
        		boolean isSelected, 
        		boolean cellHasFocus) 
        {
            Color c = (Color)value;

            int ind = 0;
            for( ; ind < Colors.length; ++ind)
                if( Colors[ind].equals(c))
                    break;
            
            if( ColorNames.length > ind)
                jlabel.setText( ColorNames[ind]);
            else
                jlabel.setText( Colors[ind].toString());

            color_panel.setBackground( Colors[ind]);

            if( isSelected)
                this.setBackground(Color.GRAY);
            else
                this.setBackground(Color.LIGHT_GRAY);

            return this;
        }

    }

    public ColorComboModel() {
        this.renderer = new CCM_Renderer();
    }

    @Override
    public void setSelectedItem(Object anItem) {
        Color c = (Color) anItem;

        for( int i = 0; i < Colors.length; ++i)
            if( Colors[i].equals(c))
                selected = i;
    }

    @Override
    public Object getSelectedItem() {
        return Colors[selected];
    }

    @Override
    public int getSize() {
          return Colors.length;
    }

    @Override
    public Color getElementAt(int index) {
        return Colors[index];
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        super.addListDataListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        super.removeListDataListener(l);
    }

}