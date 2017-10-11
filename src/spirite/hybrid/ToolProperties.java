package spirite.hybrid;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import spirite.base.brains.MasterControl;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.DBSub;
import spirite.base.util.glmath.Vec2;
import spirite.pc.ui.components.MTextFieldNumber;
import spirite.pc.ui.components.SliderPanel;
import spirite.pc.ui.panel_toolset.PropertyPanels.SizeSlider;

public class ToolProperties {	
	public static abstract class SwingToolProperty extends ToolsetManager.Property{
		public abstract List<JComponent> buildComponent( DataBinding binding, Group horizontal, Group vertical, GroupLayout layout, ToolSettings settings);
	}
	
	public static class SizeProperty extends SwingToolProperty {
		private float value;
		
		public SizeProperty( String id, String hrName, float defaultValue) {
			this.value = defaultValue;
			this.hiName = hrName;
			this.id = id;
		}

		@Override public Float getValue() { return value; }
		@Override protected void setValue( Object newValue) { this.value = (float)newValue;}

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical, 
				GroupLayout layout, ToolSettings settings) 
		{
			SizeSlider slider = new SizeSlider() {
				@Override
				public void onValueChanged(float newValue) {
					binding.uiChange(newValue);
					super.onValueChanged(newValue);
				}
			};
			slider.setValue( (float)value);
			slider.setLabel( hiName + " : ");
			
			binding.setLink( new DBSub() {
				@Override public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}

				@Override
				public void doDataChange(Object newValue) {
					slider.setValue((float)newValue);
				}
			});
			
			horizontal.addComponent(slider).addGap(30);
			vertical.addComponent(slider, 24,24,24);
			
			return Arrays.asList(new JComponent[] {slider});
		}
	}
	
	public static class OpacityProperty extends SwingToolProperty {
		private float value;
		
		public OpacityProperty( String id, String hrName, float defaultValue) {
			this.value = defaultValue;
			this.hiName = hrName;
			this.id = id;
		}

		@Override public Float getValue() { return value; }
		@Override protected void setValue( Object newValue) { this.value = (float)newValue;}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			SliderPanel slider = new SliderPanel() {
				@Override
				public void onValueChanged(float newValue) {
					binding.uiChange(newValue);
					super.onValueChanged(newValue);
				}
				@Override
				protected String valueAsString(float value) {
					return super.valueAsString(value*100);
				}
			};
			
			slider.setValue(value);
			slider.setLabel(hiName + " : ");

			binding.setLink( new DBSub() {
				@Override public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}

				@Override
				public void doDataChange(Object newValue) {
					slider.setValue((float)newValue);
				}
			});
			
			horizontal.addComponent(slider);
			vertical.addComponent(slider, 24,24,24);
			
			return Arrays.asList(new JComponent[] {slider});
		}
	}

	public static class ButtonProperty extends SwingToolProperty {
		private final MasterControl master;	// I don't love this
		private String command;
		
		public ButtonProperty( String id, String hrName, String actionCommand, MasterControl master) {
			this.command = actionCommand;
			this.master = master;
			this.hiName = hrName;
			this.id = id;
		}

		@Override public String getValue() { return command; }
		@Override protected void setValue( Object newValue) { this.command = (String)newValue;}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			JButton button = new JButton(hiName);
			JPanel panel = new JPanel();
			
			button.setActionCommand( command);
			button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					master.executeCommandString(command);
				}
			});

			horizontal.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(button).addComponent(panel));
			vertical.addComponent(button, 20,20,20).addComponent(panel,0,0,0);
			
			return Arrays.asList(new JComponent[] {button});
		}
	}
	
	public static class CheckBoxProperty extends SwingToolProperty {
		private boolean value;
		
		public CheckBoxProperty( String id, String hrName, boolean defaultValue) {
			this.value = defaultValue;
			this.id = id;
			this.hiName = hrName;
		}
		
		@Override public Boolean getValue() {return value;}
		@Override protected void setValue(Object newValue) { this.value = (Boolean)newValue;}

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			JCheckBox checkbox = new JCheckBox(hiName);
			
			checkbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					binding.dataChange(checkbox.isSelected());
				}
			});
			checkbox.setFont(new Font("Tahoma",Font.PLAIN, 10));
			checkbox.setSelected(value);
			
			binding.setLink( new DBSub() {
				@Override public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}
				@Override public void doDataChange(Object newValue) {
					checkbox.setSelected((Boolean)newValue);
				}
			});
			
			horizontal.addGap(5).addComponent(checkbox);
			vertical.addComponent(checkbox);

			return Arrays.asList(new JComponent[] {checkbox});
		}		
	}
	
	public static class DropDownProperty extends SwingToolProperty {
		private int value;
		private String[] options;
		
		public DropDownProperty( String id, String hrName, int defaultValue, String[] options) {
			this.value = defaultValue;
			this.id = id;
			this.hiName = hrName;
			this.options = options;
		}
		
		@Override public Integer getValue() {return value;}
		@Override protected void setValue(Object newValue) { this.value = (Integer)newValue;}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			JComboBox<String> comboBox = new JComboBox<>();
			JLabel label = new JLabel( hiName + ":");
			label.setFont( Globals.getFont("toolset.dropdown"));
			
			// Init Components
			for( int i=0; i<options.length; ++i) {
				comboBox.addItem(options[i]);
			}
			comboBox.setFont(new Font("Tahoma",Font.PLAIN, 10));
			comboBox.setSelectedIndex(value);
			comboBox.addItemListener( new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if( e.getStateChange() == ItemEvent.SELECTED)
						binding.uiChange(comboBox.getSelectedIndex());			
				}
			});
			
			// Binding
			binding.setLink( new DBSub() {
				@Override public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}
				@Override
				public void doDataChange(Object newValue) {
					comboBox.setSelectedIndex((Integer)newValue);
				}
			});
			
			// Add Components to Layout
			horizontal.addGroup(layout.createParallelGroup()
					.addComponent(label)
					.addComponent(comboBox, 0, 0, Short.MAX_VALUE));
			vertical.addComponent(label)
					.addComponent(comboBox, 20, 20, 20);
			
			
			return Arrays.asList(new JComponent[] {comboBox});
		}
	}
	

	public static class RadioButtonProperty extends SwingToolProperty {
		private int value;
		private String[] options;
		
		public RadioButtonProperty( String id, String hrName, int defaultValue, String[] options) {
			this.value = defaultValue;
			this.id = id;
			this.hiName = hrName;
			this.options = options;
		}
		
		@Override public Integer getValue() {return value;}
		@Override protected void setValue(Object newValue) { this.value = (Integer)newValue;}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			List<JComponent> links = new ArrayList<>( options.length);
			// TODO : Add Binding
			
			
			// Create Components and their settings
			JRadioButton[] radioButtons = new JRadioButton[ options.length];
			for( int i=0; i<options.length; ++i) {
				radioButtons[i] = new JRadioButton(options[i]);
				links.add( radioButtons[i]);
				radioButtons[i].setFont(new Font("Tahoma",Font.PLAIN, 10));
			}
			
			radioButtons[value].setSelected(true);
			
			// Link actions of the Components, and add them to the layout
			Group horSub = layout.createParallelGroup();
			for( int i=0; i<options.length; ++i) {
				final int index = i;
				radioButtons[i].addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for( int i=0; i<options.length; ++i) {
							radioButtons[i].setSelected(i == index);
						}
						settings.setValue(id, index);
					}
				});
				
				horSub.addComponent(radioButtons[i]);
				vertical.addComponent(radioButtons[i]);
			}
			horizontal.addGroup(horSub);

			return links;
		}
	}

	public static class FloatBoxProperty extends SwingToolProperty {
		private float value;
		
		public FloatBoxProperty( String id, String hrName, float defaultValue) {
			this.value = defaultValue;
			this.id = id;
			this.hiName = hrName;
		}
		
		@Override public Float getValue() {return value;}
		@Override protected void setValue(Object newValue) { this.value = (Float)newValue;}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			JLabel label = new JLabel( hiName + ":");
			MTextFieldNumber textField = new MTextFieldNumber(true, true);
			
			label.setFont( Globals.getFont("toolset.dropdown"));
			textField.setText( String.valueOf(value));
			
			// Listener
			textField.getDocument().addDocumentListener( new DocumentListener() {
				public void changedUpdate(DocumentEvent arg0) {
					binding.uiChange(textField.getFloat());
				}
				public void insertUpdate(DocumentEvent arg0) {
					binding.uiChange(textField.getFloat());
				}
				public void removeUpdate(DocumentEvent arg0) {
					binding.uiChange(textField.getFloat());
				}
			});
			
			binding.setLink( new DBSub() {
				@Override public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}
				@Override public void doDataChange(Object newValue) {
					textField.setText( String.valueOf(newValue));
				}
			});
			
			horizontal.addGroup(layout.createSequentialGroup()
					.addComponent(label)
					.addGap(3)
					.addComponent(textField, 0, 0, Short.MAX_VALUE));
			vertical.addGroup( layout.createParallelGroup()
					.addComponent(label)
					.addComponent(textField, 16, 16, 16));
			
			return Arrays.asList( new JComponent[] {textField});
		}
	}

	public static class DualFloatBoxProperty extends SwingToolProperty {
		private float x, y;
		private String _label1, _label2;
		
		public DualFloatBoxProperty( String id, String hrName, float defaultX, float defaultY, String label1, String label2) {
			this.x = defaultX;
			this.y = defaultY;
			this.id = id;
			this.hiName = hrName;
			this._label1 = label1;
			this._label2 = label2;
		}
		
		@Override public Vec2 getValue() {return new Vec2(x,y);}
		@Override protected void setValue(Object newValue) { 
			this.x = ((Vec2)newValue).x;
			this.y = ((Vec2)newValue).y;
		}
		

		@Override
		public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
				GroupLayout layout, ToolSettings settings) 
		{
			JLabel labelMain = new JLabel(hiName + ":");
			JLabel label1 = new JLabel( _label1);
			JLabel label2 = new JLabel( _label2);
			MTextFieldNumber textField1 = new MTextFieldNumber(true, true);
			MTextFieldNumber textField2 = new MTextFieldNumber(true, true);
			
			labelMain.setFont( Globals.getFont("toolset.dropdown"));
			label1.setFont( Globals.getFont("toolset.dropdown"));
			label2.setFont( Globals.getFont("toolset.dropdown"));
			textField1.setText( String.valueOf(x));
			textField2.setText( String.valueOf(y));
			
			// Listener
			DocumentListener listener = new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					binding.uiChange(new Vec2(textField1.getFloat(), textField2.getFloat()));
				}
				public void insertUpdate(DocumentEvent e) {
					binding.uiChange(new Vec2(textField1.getFloat(), textField2.getFloat()));
				}
				public void removeUpdate(DocumentEvent e) {
					binding.uiChange(new Vec2(textField1.getFloat(), textField2.getFloat()));
				}
			};
			textField1.getDocument().addDocumentListener(listener);
			textField2.getDocument().addDocumentListener(listener);
			
			binding.setLink( new DBSub() {
				public void doUIChange(Object newValue) {
					settings.setValue( id, newValue);
				}
				public void doDataChange(Object newValue) {
					Vec2 val = (Vec2)newValue;
					textField1.setText( String.valueOf(val.x));
					textField2.setText( String.valueOf(val.y));
				}
			});
			
			// Layout
			horizontal.addGroup(layout.createParallelGroup()
				.addComponent(labelMain)
				.addGroup(layout.createSequentialGroup()
					.addComponent(textField1, 0, 0, Short.MAX_VALUE)
					.addGap(3)
					.addComponent(label1)
					.addGap(5)
					.addComponent(textField2, 0, 0, Short.MAX_VALUE)
					.addGap(3)
					.addComponent(label2)));
			vertical.addGroup(layout.createSequentialGroup()
				.addComponent(labelMain)
				.addGroup(layout.createParallelGroup()
					.addComponent(textField1, 16, 16, 16)
					.addComponent(textField2, 16, 16, 16)
					.addComponent(label1)
					.addComponent(label2)));
			
			return Arrays.asList(new JComponent[] { textField1, textField2});
		}
	}
}
