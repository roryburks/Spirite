package spirite.gui.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

import spirite.base.graphics.RawImage;
import spirite.gui.generic.SButton;
import spirite.gui.generic.events.SActionEvent;
import spirite.gui.generic.events.SActionEvent.SActionListener;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;

public class SwingGuiButton extends SwingGuiComponent 
	implements SButton
{
	private final JButton button = new JButton() ;
	private Color bgColor2 = Color.WHITE;
	private Color bgColor1 = Color.LIGHT_GRAY;
	
	public SwingGuiButton() {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
	}

	@Override
	public JComponent getComponent() {
		return button;
	}

	@Override
	public void setIcon(RawImage img) {
		button.setIcon(new ImageIcon(HybridUtil.convert(img, ImageBI.class).img));
	}
	

	Map<SActionListener,ActionListener> actionListeners = new HashMap<>();
	@Override
	public void addActionListner(SActionListener listener) {
		if( actionListeners.containsKey(listener))
			button.removeActionListener(actionListeners.get(listener));

		ActionListener pipeListener = (e) -> {
			listener.actionPerformed(new SActionEvent(e.getActionCommand()));
		};
		
		button.addActionListener(pipeListener);
	}
	@Override
	public void removeActionListener(SActionListener listener) {
		if( actionListeners.containsKey(listener)) {
			button.removeActionListener(actionListeners.get(listener));
			actionListeners.remove(listener);
		}
	}
}
