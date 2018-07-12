import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.*;
import java.nio.FloatBuffer;

public class GLCrashTest {
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            JFrame frame = new JglpanelDemo();
            frame.pack();
            frame.setSize(100, 100);
            frame.isLocationByPlatform();
            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class JglpanelDemo extends  JFrame {
        JglpanelDemo() {
            GLProfile profile = GLProfile.getDefault();
            GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
            GLCapabilities caps = new GLCapabilities(profile);
            caps.setHardwareAccelerated(true);
            caps.setDoubleBuffered(false);
            caps.setAlphaBits(8);
            caps.setRedBits(8);
            caps.setBlueBits(8);
            caps.setGreenBits(8);
            caps.setOnscreen(false);

            GLOffscreenAutoDrawable offscreenDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), caps, new DefaultGLCapabilitiesChooser(), 1, 1);
            offscreenDrawable.display();

            GLJPanel glPanel = new GLJPanel();
            glPanel.addGLEventListener(new GLEventListener() {
                @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
                @Override public void dispose(GLAutoDrawable drawable) {}

                @Override
                public void display(GLAutoDrawable drawable) {
                    drawable.getContext().makeCurrent();
                    float array[] = {1f, 0f, 0f, 1f};
                    FloatBuffer buf = Buffers.newDirectFloatBuffer(array);
                    buf.rewind();
                    drawable.getGL().getGL2().glClearBufferfv(GL2.GL_COLOR, 0, buf);
                    drawable.getContext().release();
                }

                @Override
                public void init(GLAutoDrawable drawable) {
                    GLContext primaryContext = offscreenDrawable.getContext();

                    GLContext unusedDefaultContext = drawable.getContext();
                    unusedDefaultContext.makeCurrent();
                    drawable.setContext(null, true);


                    GLContext subContext = drawable.createContext(primaryContext);
                    subContext.makeCurrent();
                    drawable.setContext(subContext, true);
                }
            });

            add(glPanel);
        }
    }
}