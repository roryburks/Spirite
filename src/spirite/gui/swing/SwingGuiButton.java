package spirite.gui.swing;

import spirite.base.graphics.RawImage;
import spirite.gui.generic.ISButton;
import spirite.gui.generic.events.SActionEvent;
import spirite.gui.generic.events.SActionEvent.SActionListener;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class SwingGuiButton extends SwingGuiComponent 
	implements ISButton
{
	private final JButton button = new JButton() {
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
		}
	};
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
