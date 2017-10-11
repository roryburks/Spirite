package spirite.pc.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import spirite.base.brains.MasterControl;
import spirite.base.brains.renderer.RenderEngine.RenderMethod;
import spirite.pc.ui.UIUtil;
import spirite.pc.ui.dialogs.Dialogs;


public class RenderOptionsCombo extends JPanel {
	private final JComboBox<RenderTuple> comboBox;
	private final JPanel rcOptions = new JPanel();
	private final Dialogs dialogs;
	private final RenderOptionCellRenderer renderer = new RenderOptionCellRenderer();
	private final JLabel rcLabel = new JLabel("Mode:");

	public static class RenderTuple {
		final RenderMethod method;
		int value;
		RenderTuple( RenderMethod method) {
			this.method = method;
			this.value = method.defaultValue;
		}
	}
	
	public RenderOptionsCombo(MasterControl master) {
		this.dialogs = master.getDialogs();
		
		RenderMethod values[] = RenderMethod.values();
		RenderTuple options[] = new RenderTuple[ values.length];
		for( int i=0; i <values.length; ++i)
			options[i] = new RenderTuple( values[i]);
	
		
		comboBox = new JComboBox<>(options);
		comboBox.setRenderer(renderer);
		
		initLayout();
		

		comboBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetRCOptionPanel();
			}
		});
	}
	
	private void initLayout() {
		GroupLayout layout = new GroupLayout( this);
		
		layout.setHorizontalGroup( layout.createSequentialGroup()
				.addComponent(rcLabel)
				.addComponent(comboBox)
				.addGap(3)
				.addComponent(rcOptions, 30, 30, 30));
		layout.setVerticalGroup( layout.createParallelGroup()
				.addComponent(rcLabel)
				.addComponent(comboBox)
				.addComponent(rcOptions));
		
		this.setLayout(layout);
	}
	
	public RenderMethod getMethod() {
		return comboBox.getItemAt(comboBox.getSelectedIndex()).method;
	}
	public int getRenderValue() {
		return comboBox.getItemAt(comboBox.getSelectedIndex()).value;
	}
	

	private void resetRCOptionPanel() {
		rcOptions.removeAll();
		RenderTuple sel = ((RenderTuple)comboBox.getSelectedItem());
		switch( sel.method) {
		case COLOR_CHANGE:
			renderer.ccPanel.setBackground(new Color(sel.value));
			rcOptions.add(renderer.ccPanel);
			break;
		case DEFAULT:
			break;
		
		}
		rcOptions.doLayout();
		rcOptions.revalidate();
		rcOptions.repaint();
	}

	/** CellRenderer for the RenderOption Combo Box. */
	public class RenderOptionCellRenderer implements ListCellRenderer<RenderTuple> {
		private final JPanel panel = new JPanel();
		private final JLabel lbl = new JLabel();
		private final Color comboSel = new Color( 164,164,216);
		private final Color comboNill = new Color( 196,196,196);
		
		private JPanel ccPanel = new JPanel();
		
		public RenderOptionCellRenderer() {
			panel.setLayout(new GridLayout());
			panel.add(lbl);
			
			ccPanel.addMouseListener( new UIUtil.ClickAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					RenderTuple sel = ((RenderTuple)comboBox.getSelectedItem());
					Color c = dialogs.pickColor(new Color(sel.value));
					if( c != null) {
						sel.value = c.getRGB();
						ccPanel.setBackground(c);
						//updateSelMethod();
					}
				}
			});
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends RenderTuple> list, 
				RenderTuple value, 
				int index,
				boolean isSelected, 
				boolean cellHasFocus) 
		{
			lbl.setText(value.method.description);
			
			panel.setBackground( (isSelected) ? comboSel : comboNill);
			
			return panel;
		}
	}
}
