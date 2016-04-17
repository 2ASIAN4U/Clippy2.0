import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;

import static java.awt.GraphicsDevice.WindowTranslucency.*;

public class TranscluentWindow {

    public static void main(String[] args) {
        new TranscluentWindow();
    }

    public TranscluentWindow() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception ex) {
                    }

                    JWindow frame = new JWindow();
                    frame.setAlwaysOnTop(true);
                    frame.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getClickCount() == 2) {
                                SwingUtilities.getWindowAncestor(e.getComponent()).dispose();
                            }
                        }
                    });
                    frame.setBackground(new Color(0,0,0,0));
                    frame.setContentPane(new TranslucentPane());
                    frame.add(new JLabel(new ImageIcon(ImageIO.read(getClass().getResource("/TestEmblem.png")))));
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    for(int i=0;i<10;i++){
                    	frame.setLocation(i*100, i*100);
                    	try {
                    	    Thread.sleep(1000);                 //1000 milliseconds is one second.
                    	} catch(InterruptedException ex) {
                    	    Thread.currentThread().interrupt();
                    	}
                    }
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
    }

    public class TranslucentPane extends JPanel {

        public TranslucentPane() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.SrcOver.derive(0f));
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());

        }

    }
    

}