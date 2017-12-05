package spirite.pc.ui;

import spirite.gui.hybrid.SPanel;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TestFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private SPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestFrame frame = new TestFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public TestFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new SPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		SPanel panel_1 = new SPanel();
		tabbedPane.addTab("New tab", null, panel_1, null);
		
		SPanel panelA = new SPanel();
		SPanel panelB = new SPanel();
		
		GroupLayout gl_panel_1 = new GroupLayout(panel_1);
		gl_panel_1.setHorizontalGroup(
			gl_panel_1.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_1.createSequentialGroup()
					.addComponent(panelA, 100, 100, 100)
					.addComponent(panelB, 0, 100, Integer.MAX_VALUE)
				)
		);
		gl_panel_1.setVerticalGroup(
			gl_panel_1.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_1.createSequentialGroup()
					.addGroup(gl_panel_1.createParallelGroup(Alignment.LEADING)
							.addComponent(panelA, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
							.addComponent(panelB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
					)
				)
				
		);
		
		JRadioButton radioButton = new JRadioButton("1");
		panelA.add(radioButton);
		
		JRadioButton radioButton_1 = new JRadioButton("2");
		panelA.add(radioButton_1);
		
		JSpinner spinner = new JSpinner();
		panelA.add(spinner);
		
		JComboBox comboBox = new JComboBox();
		panelA.add(comboBox);
		
		JRadioButton radioButton_2 = new JRadioButton("3");
		panelA.add(radioButton_2);
		panel_1.setLayout(gl_panel_1);
		
	}

}
