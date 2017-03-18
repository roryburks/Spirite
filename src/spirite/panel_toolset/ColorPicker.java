package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.PaletteManager.MPaletteObserver;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.UIUtil;
import spirite.ui.components.MTextFieldNumber;

public class ColorPicker extends OmniComponent 
	implements MPaletteObserver
{
	private final ComponentsRGBA comp1 = new ComponentsRGBA();
	private final ComponentsRGBA comp2 = new ComponentsRGBA();
	private final ComponentsHSVA comp1hsv = new ComponentsHSVA();
	private final ComponentsHSVA comp2hsv = new ComponentsHSVA();

	private JTabbedPane container = new JTabbedPane();

	
	private final PaletteManager paletteManager;
	
	public ColorPicker(MasterControl master) {
		this.paletteManager = master.getPaletteManager();
		paletteManager.addPaletteObserver(this);
		
		initComponents();
		
		ColorChangeAlerter p1cca = new ColorChangeAlerter() {
			@Override public void onColorChange(Color c) {
				paletteManager.setActiveColor(0, c);
			}
		};
		ColorChangeAlerter p2cca = new ColorChangeAlerter() {
			@Override public void onColorChange(Color c) {
				paletteManager.setActiveColor(1, c);
			}
		};
		comp1.alerter = p1cca;
		comp2.alerter = p2cca;

		comp1hsv.alerter = p1cca;
		comp2hsv.alerter = p2cca;
	}
	
	
	private void initComponents() {
		this.setLayout(new GridLayout());
		this.add( container);

		JPanel rgb = new JPanel();
		JPanel hsv = new JPanel();
		container.addTab("RGB", rgb);
		container.addTab("HSV", hsv);
		
		
		GroupLayout layout = new GroupLayout( rgb);
		layout.setHorizontalGroup( layout.createParallelGroup()
			.addGroup( rgbaHorCompGroup( layout, comp1))
			.addGroup( rgbaHorCompGroup( layout, comp2))
		);
		layout.setVerticalGroup( layout.createSequentialGroup()
			.addGroup( rgbaVertCompGroup(layout, comp1))
			.addGroup( rgbaVertCompGroup(layout, comp2))
		);
		rgb.setLayout( layout);

		layout = new GroupLayout( hsv);
		layout.setHorizontalGroup( layout.createParallelGroup()
			.addGroup( hsvaHorCompGroup( layout, comp1hsv))
			.addGroup( hsvaHorCompGroup( layout, comp2hsv))
		);
		layout.setVerticalGroup( layout.createSequentialGroup()
			.addGroup( hsvaVertCompGroup(layout, comp1hsv))
			.addGroup( hsvaVertCompGroup(layout, comp2hsv))
		);
		hsv.setLayout(layout);
		
		refreshColors();
	}

	private GroupLayout.Group rgbaHorCompGroup( GroupLayout layout, ComponentsRGBA comp) {
		return layout.createSequentialGroup()
				.addComponent(comp.cPanel, 25, 100, 100)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.rSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.gSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.bSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.aSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.rL)
					.addComponent(comp.gL)
					.addComponent(comp.bL)
					.addComponent(comp.aL)
				)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.rText, 36,36,36)
					.addComponent(comp.gText, 36,36,36)
					.addComponent(comp.bText, 36,36,36)
					.addComponent(comp.aText, 36,36,36)
				);
	}

	private GroupLayout.Group rgbaVertCompGroup( GroupLayout layout, ComponentsRGBA comp) {
		return layout.createParallelGroup()
			.addComponent(comp.cPanel)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.rSlider, 16, 16, 16)
				.addComponent(comp.gSlider, 16, 16, 16)
				.addComponent(comp.bSlider, 16, 16, 16)
				.addComponent(comp.aSlider, 16, 16, 16)
			)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.rL)
				.addComponent(comp.gL)
				.addComponent(comp.bL)
				.addComponent(comp.aL)
			)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.rText, 16, 16, 16)
				.addComponent(comp.gText, 16, 16, 16)
				.addComponent(comp.bText, 16, 16, 16)
				.addComponent(comp.aText, 16, 16, 16)
			);
	}
	
	private GroupLayout.Group hsvaHorCompGroup( GroupLayout layout, ComponentsHSVA comp) {
		return layout.createSequentialGroup()
				.addComponent(comp.cPanel, 25, 100, 100)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.hSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.sSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.vSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(comp.aSlider, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.hL)
					.addComponent(comp.sL)
					.addComponent(comp.vL)
					.addComponent(comp.aL)
				)
				.addGroup( layout.createParallelGroup()
					.addComponent(comp.hText, 36,36,36)
					.addComponent(comp.sText, 36,36,36)
					.addComponent(comp.vText, 36,36,36)
					.addComponent(comp.aText, 36,36,36)
				);
	}

	private GroupLayout.Group hsvaVertCompGroup( GroupLayout layout, ComponentsHSVA comp) {
		return layout.createParallelGroup()
			.addComponent(comp.cPanel)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.hSlider, 16, 16, 16)
				.addComponent(comp.sSlider, 16, 16, 16)
				.addComponent(comp.vSlider, 16, 16, 16)
				.addComponent(comp.aSlider, 16, 16, 16)
			)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.hL)
				.addComponent(comp.sL)
				.addComponent(comp.vL)
				.addComponent(comp.aL)
			)
			.addGroup( layout.createSequentialGroup()
				.addComponent(comp.hText, 16, 16, 16)
				.addComponent(comp.sText, 16, 16, 16)
				.addComponent(comp.vText, 16, 16, 16)
				.addComponent(comp.aText, 16, 16, 16)
			);
	}
	
	class ColorSlider extends JSlider {
		ColorSlider() {
			setMinimum(0);
			setMaximum(255);
			setValue(0);
			setOrientation(JSlider.HORIZONTAL);
		}
	}
	class ColorText extends MTextFieldNumber {
		ColorText() {
			setMinMax(0, 255);
			setColumns(3);
		}
	}
	class ColorPanel extends JPanel {
		ColorPanel() {
			this.setOpaque(false);
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if( getBackground().getAlpha() != 255)
				UIUtil.drawTransparencyBG(g, new Rectangle(0,0,getWidth(),getHeight()), 8);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	interface ColorChangeAlerter {
		void onColorChange( Color c);
	}
	
	/** A container class that exists just to minimize redundant code*/
	class ComponentsRGBA {
		final ColorSlider rSlider = new ColorSlider();
		final ColorSlider gSlider = new ColorSlider();
		final ColorSlider bSlider = new ColorSlider();
		final ColorSlider aSlider = new ColorSlider();
		final ColorText rText = new ColorText();
		final ColorText gText = new ColorText();
		final ColorText bText = new ColorText();
		final ColorText aText = new ColorText();
		final ColorPanel cPanel = new ColorPanel();

		private final JLabel rL = new JLabel("R:");
		private final JLabel gL = new JLabel("G:");
		private final JLabel bL = new JLabel("B:");
		private final JLabel aL = new JLabel("A:");
		ColorChangeAlerter alerter;
		

		ComponentsRGBA(){
			ChangeListener sliderChangeListener = new ChangeListener() {
				@Override public void stateChanged(ChangeEvent e) {
					if(!changing && alerter != null) {
						alerter.onColorChange( new Color(
							rSlider.getValue(),
							gSlider.getValue(),
							bSlider.getValue(),
							aSlider.getValue()));
					}
				}
			};
			rSlider.addChangeListener( sliderChangeListener);
			gSlider.addChangeListener( sliderChangeListener);
			bSlider.addChangeListener( sliderChangeListener);
			aSlider.addChangeListener( sliderChangeListener);
			
			DocumentListener textDocListener = new DocumentListener() {
				private void doIt() {
					if(!changing && alerter != null) {
						alerter.onColorChange( new Color(
							rText.getNumber(),
							gText.getNumber(),
							bText.getNumber(),
							aText.getNumber()));
					}
				}
				@Override public void changedUpdate(DocumentEvent arg0) {
					doIt();
				}
				@Override public void insertUpdate(DocumentEvent arg0) {
					doIt();
				}
				@Override public void removeUpdate(DocumentEvent arg0) {
					doIt();
				}
			};
			rText.getDocument().addDocumentListener(textDocListener);
			gText.getDocument().addDocumentListener(textDocListener);
			bText.getDocument().addDocumentListener(textDocListener);
			aText.getDocument().addDocumentListener(textDocListener);
		}

		private boolean changing = false;
		public void setColor( Color c) {
			SwingUtilities.invokeLater( new Runnable() { @Override
				public void run() {
					changing = true;
					rSlider.setValue(c.getRed());
					gSlider.setValue(c.getGreen());
					bSlider.setValue(c.getBlue());
					aSlider.setValue(c.getAlpha());

					rText.setText(Integer.toString(c.getRed()));
					gText.setText(Integer.toString(c.getGreen()));
					bText.setText(Integer.toString(c.getBlue()));
					aText.setText(Integer.toString(c.getAlpha()));
					
					cPanel.setBackground(c);
					changing = false;
				}
			});
		}
	}
	
	/**
	 * ComponentsHSVA employs a more complex set of UI-Data bindings because the
	 * method used in ComponentsRGBA causes rounding drift from converting to and
	 * from RGBA and HSVA over and over.  The one-way data bindings of ComponentsHSVA
	 * with locking and UI-to-UI migration of data only where necessary is the more 
	 * robust form of data binding that should be used in general.
	 */
	class ComponentsHSVA {
		final JSlider hSlider = new JSlider();
		final JSlider sSlider = new JSlider();
		final JSlider vSlider = new JSlider();
		final JSlider aSlider = new JSlider();
		final MTextFieldNumber hText = new MTextFieldNumber();
		final MTextFieldNumber sText = new MTextFieldNumber();
		final MTextFieldNumber vText = new MTextFieldNumber();
		final MTextFieldNumber aText = new MTextFieldNumber();
		final ColorPanel cPanel = new ColorPanel();

		private final JLabel hL = new JLabel("H:");
		private final JLabel sL = new JLabel("S:");
		private final JLabel vL = new JLabel("V:");
		private final JLabel aL = new JLabel("A:");
		ColorChangeAlerter alerter;
		

		ComponentsHSVA(){
			hSlider.setMinimum(0);
			hSlider.setMaximum(360);
			sSlider.setMinimum(0);
			sSlider.setMaximum(100);
			vSlider.setMinimum(0);
			vSlider.setMaximum(100);
			aSlider.setMinimum(0);
			aSlider.setMaximum(255);
			hText.setMinMax(0,360);
			sText.setMinMax(0,100);
			vText.setMinMax(0,100);
			aText.setMinMax(0,255);
			ChangeListener sliderChangeListener = new ChangeListener() {
				@Override public void stateChanged(ChangeEvent e) {
					if(!lock && alerter != null) {
						lock = true;
						int rgb =  Color.HSBtoRGB(
								hSlider.getValue() / 360.0f, 
								sSlider.getValue() / 100.0f, 
								vSlider.getValue() / 100.0f);
						Color c = new Color(
								(rgb >> 16) & 0xFF,
								(rgb >> 8) & 0xFF,
								(rgb) & 0xFF,
								aSlider.getValue());
						alerter.onColorChange( c);
						
						uiToUIChange( e.getSource(), c);
						lock = false;
						
					}
				}
			};
			hSlider.addChangeListener( sliderChangeListener);
			sSlider.addChangeListener( sliderChangeListener);
			vSlider.addChangeListener( sliderChangeListener);
			aSlider.addChangeListener( sliderChangeListener);
			
			hText.getDocument().addDocumentListener(new BoundDocListener(hText));
			sText.getDocument().addDocumentListener(new BoundDocListener(sText));
			vText.getDocument().addDocumentListener(new BoundDocListener(vText));
			aText.getDocument().addDocumentListener(new BoundDocListener(aText));
		}
		
		private class BoundDocListener implements DocumentListener{
			final Object binding;
			BoundDocListener( Object binding) {
				this.binding = binding;
			}
			private void doIt() {
				if(!lock && alerter != null) {
					lock = true;
					int rgb =  Color.HSBtoRGB(
							hText.getNumber() / 360.0f, 
							sText.getNumber() / 100.0f, 
							vText.getNumber() / 100.0f);
					Color c =  new Color(
							(rgb >> 16) & 0xFF,
							(rgb >> 8) & 0xFF,
							(rgb) & 0xFF,
							aText.getNumber());
					alerter.onColorChange( c);
					
					uiToUIChange( binding, c);
					lock = false;
				}
			}
			@Override public void changedUpdate(DocumentEvent arg0) {
				doIt();
			}
			@Override public void insertUpdate(DocumentEvent arg0) {
				doIt();
			}
			@Override public void removeUpdate(DocumentEvent arg0) {
				doIt();
			}
		}
		
		private void uiToUIChange( Object source, Color c ) {
			cPanel.setBackground(c);
			float hsb[] = new float[3];
			
			if( source == hSlider) 
				hText.setText(Integer.toString(hSlider.getValue()));
			else if( source == sSlider) 
				sText.setText(Integer.toString(sSlider.getValue()));
			else if( source == vSlider) 
				vText.setText(Integer.toString(vSlider.getValue()));
			else if( source == aSlider) 
				aText.setText(Integer.toString(aSlider.getValue()));
			else if( source == hText) 
				hSlider.setValue(hText.getNumber());
			else if( source == sText) 
				sSlider.setValue(sText.getNumber());
			else if( source == vText) 
				vSlider.setValue(vText.getNumber());
			else if( source == aText) 
				aSlider.setValue(aText.getNumber());
			
		}

		private boolean lock = false;
		public void setColor( Color c) {
			if( !lock) {
				lock = true;
				
				float hsb[] = new float[3];
				Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
				hSlider.setValue(Math.round(hsb[0] * 360));
				sSlider.setValue(Math.round(hsb[1] * 100));
				vSlider.setValue(Math.round(hsb[2] * 100));
				aSlider.setValue(c.getAlpha());

				hText.setText(Integer.toString(Math.round(hsb[0] * 360)));
				sText.setText(Integer.toString(Math.round(hsb[1] * 100)));
				vText.setText(Integer.toString(Math.round(hsb[2] * 100)));
				aText.setText(Integer.toString(c.getAlpha()));
				
				cPanel.setBackground(c);
				lock = false;
			}
		}
	}
	
	private void refreshColors() {
		comp1.setColor(paletteManager.getActiveColor(0));
		comp2.setColor(paletteManager.getActiveColor(1));
		comp1hsv.setColor(paletteManager.getActiveColor(0));
		comp2hsv.setColor(paletteManager.getActiveColor(1));
	}

	// :::: MPaletteObserver
	@Override
	public void colorChanged() {
		refreshColors();
	}
}
