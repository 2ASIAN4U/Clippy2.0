import java.awt.*;
import java.util.*;
import java.util.Arrays;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;
import java.io.*;
import sun.audio.*;
import javax.swing.*;
import java.applet.*;
import java.net.*;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.*;

import net.java.balloontip.*;
import net.java.balloontip.utils.ToolTipUtils;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;

public class Square{

    int thresh = 50;
    IplImage img = null;
    IplImage img0 = null;
    CvMemStorage storage = null;
    String wndname = "Square Detection Demo";
    ArrayList platforms = new ArrayList<Tuple>();

    CanvasFrame canvas = null;
    OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
        double dx1 = pt1.x() - pt0.x();
        double dy1 = pt1.y() - pt0.y();
        double dx2 = pt2.x() - pt0.x();
        double dy2 = pt2.y() - pt0.y();

        return (dx1*dx2 + dy1*dy2) / Math.sqrt((dx1*dx1 + dy1*dy1) * (dx2*dx2 + dy2*dy2) + 1e-10);
    }
    CvSeq findSquares4(IplImage img, CvMemStorage storage) {
        int i, c, l, N = 11;
        CvSize sz = cvSize(img.width() & -2, img.height() & -2);
        IplImage timg = cvCloneImage(img); // make a copy of input image
        IplImage gray = cvCreateImage(sz, 8, 1);
        IplImage pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), 8, 3);
        IplImage tgray = null;
        CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
        cvSetImageROI(timg, cvRect(0, 0, sz.width(), sz.height()));
        cvPyrDown(timg, pyr, 7);
        cvPyrUp(pyr, timg, 7);
        tgray = cvCreateImage(sz, 8, 1);
        for (c = 0; c < 3; c++) {
            cvSetImageCOI(timg, c+1);
            cvCopy(timg, tgray);
            for (l = 0; l < N; l++) {
                if (l == 0) {
                    cvCanny(tgray, gray, 0, thresh, 5);
                    cvDilate(gray, gray, null, 1);
                } else {
                    cvThreshold(tgray, gray, (l+1)*255/N, 255, CV_THRESH_BINARY);
                }
                CvSeq contours = new CvSeq();
                cvFindContours(gray, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0,0));

                while (contours != null && !contours.isNull()) {
                    CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
                    if(result.total() == 4 && Math.abs(cvContourArea(result, CV_WHOLE_SEQ, 0)) > 1000 && cvCheckContourConvexity(result) != 0) {
                        double s = 0.0, t = 0.0;

                        for( i = 0; i < 5; i++ ) {
                            if( i >= 2 ) {
                                t = Math.abs(angle(new CvPoint(cvGetSeqElem(result, i)),
                                        new CvPoint(cvGetSeqElem(result, i-2)),
                                        new CvPoint(cvGetSeqElem(result, i-1))));
                                s = s > t ? s : t;
                            }
                        }
                        if (s < 0.3)
                            for( i = 0; i < 4; i++ ) {
                                cvSeqPush(squares, cvGetSeqElem(result, i));
                            }
                    }
                    contours = contours.h_next();
                }
            }
        }
        cvReleaseImage(gray);
        cvReleaseImage(pyr);
        cvReleaseImage(tgray);
        cvReleaseImage(timg);

        return squares;
    }
    void drawSquares(IplImage img, CvSeq squares) {
        IplImage cpy = cvCloneImage(img);
        int i = 0;
        CvSlice slice = new CvSlice(squares);
         platforms.clear();
         for(i = 0; i < squares.total(); i += 4) {
             CvPoint rect = new CvPoint(4);
             IntPointer count = new IntPointer(1).put(4);
             // get the 4 corner slice from the "super"-slice
             cvCvtSeqToArray(squares, rect, slice.start_index(i).end_index(i + 4));
             cvPolyLine(cpy, rect.position(0), count, 1, 1, CV_RGB(0,255,0), 3, CV_AA, 0);
             System.out.println(rect);
             int[] temp = {rect.get(1),rect.get(3),rect.get(5),rect.get(7)};
             Arrays.sort(temp);
             if((temp[3]-5 <= temp[2]) && (temp[2] <= temp[3]+5)){
            	 int[] temp2 = {rect.get(0),rect.get(2),rect.get(4),rect.get(6)};
                 Arrays.sort(temp2);
                 platforms.add(new Tuple<>((int)temp[2],(int)(temp2[3]-temp2[1])/2, (int)(temp2[3]-temp2[1])/2 + temp2[1]));
             }
         }
         System.out.println(platforms);
        cvReleaseImage(cpy);
    }
    String names[] = new String[]{"Capture.png"};

    public static void main(String args[]) throws Exception {
        new Square().main();
    }

    public void main() throws InterruptedException, AWTException {
        int i; // , c;
        Java2DFrameConverter frameBufferedImageConverter = new Java2DFrameConverter();
        OpenCVFrameConverter.ToIplImage frameIplImageConverter = new OpenCVFrameConverter.ToIplImage();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Random rgen = new Random();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();
        storage = cvCreateMemStorage(0);
        boolean sysexit = false;
        Thread t = new Thread(new Runnable() {
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
                    JLabel clippy = new JLabel(new ImageIcon(ImageIO.read(getClass().getResource("/Clippy.png"))));
                    frame.add(clippy);
                    BalloonTip Popup = new BalloonTip(clippy, "I'm Clippy, would you like some help?");
                    frame.add(Popup);
                    try
	      			{
                    	InputStream inputStream = getClass().getResourceAsStream("/Open.wav");
	      			    AudioStream audioStream = new AudioStream(inputStream);
	      			    AudioPlayer.player.start(audioStream);
	      			}
	      			catch (Exception e){}
                    frame.setSize(512,512);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    int x = 1500;
                    int y = 2000;
                    int momentum = 0;
                    int ymom = 0;
                    
                    while(true){
                    	boolean contact = false;
                    	for(int i=0;i<platforms.size();i++){
                    		if(( ((Tuple)platforms.get(i)).x()-2 <= y) && (y <= ((Tuple)platforms.get(i)).x()+2) && ((Tuple) platforms.get(i)).y() >= Math.abs(((Tuple)platforms.get(i)).z() - x)){
                    			System.out.print( Math.abs(((Tuple)platforms.get(i)).z()-x) + " <|> ");
                    			if(((Tuple)platforms.get(i)).x() <= y && (y <= ((Tuple)platforms.get(i)).x()+2))
        							y--;
                    			contact = true;
                    		}
                    	}
                    	if(height == y){
                			contact = true;
                		}
                    	if(!contact){
                    		y++;
                    	}
                    	int mousex = (int)MouseInfo.getPointerInfo().getLocation().getX();
                    	int mousey = (int)MouseInfo.getPointerInfo().getLocation().getY();
                    	if(mousex-200 >= x)
                    		momentum++;
                    	else if(mousex+200 <= x)
                    		momentum--;
                    	else{
                    	if(momentum < 0)
                    		momentum++;
                    	else
                    		momentum--;
                    	if(mousey <= y)
                    		if(ymom == 0){
                    			try
                    			  {
                    			    InputStream inputStream = getClass().getResourceAsStream("/Whoosh.wav");
                    			    AudioStream audioStream = new AudioStream(inputStream);
                    			    AudioPlayer.player.start(audioStream);
                    			  }
                    			  catch (Exception e){}
                    			ymom = 11;
                    			
                    		}
                    		momentum = 0;
                    	}
                    	if(Math.abs(momentum) >= 2){
                    		if(momentum < 0)
                    			momentum = -2;
                    		else
                    			momentum = 2;
                    	}
                    	if(ymom > 0 && !contact)
                    		ymom--;
                    	x+=momentum;
                    	y-=ymom;
                    	frame.setLocation(x, y);
                    	System.out.println(x+" | "+y + " | " + momentum + " | " + ymom + " | " + contact);
                    	Thread.sleep(5);
                    	int ran = rgen.nextInt(5000);
                    	switch(ran){
                    		case 0: Popup = new BalloonTip(clippy, "I see you are using a computer, would you like some help?"); break;
                    		case 1: Popup = new BalloonTip(clippy, "I see you have a keyboard, would you like some help?"); break;
                    		case 2: Popup = new BalloonTip(clippy, "I see you installed the internet, would you like some help?"); break;
                    		case 3: Popup = new BalloonTip(clippy, "I see you are staring at a screen, would you like some help?"); break;
                    		case 4: Popup = new BalloonTip(clippy, "I see you are annoyed at me, would you like some help?"); break;
                    		case 5: Popup = new BalloonTip(clippy, "I see you have a messy desktop, would you like some help?"); break;
                    		case 6: Popup = new BalloonTip(clippy, "I see you are clicking on icons, would you like some help?"); break;
                    		case 7: Popup = new BalloonTip(clippy, "I see you are surfing the web, would you like some help?"); break;
                    		case 8: Popup = new BalloonTip(clippy, "I see you find me useless, would you like some help?"); break;
                    		case 9: Popup = new BalloonTip(clippy, "I see you don't want help, would you like some help?"); break;
                    		default: break;
                    	}
                    	if(ran<10){
                    	  try
              			  {
              			    InputStream inputStream = getClass().getResourceAsStream("/Clippy.wav");
              			    AudioStream audioStream = new AudioStream(inputStream);
              			    AudioPlayer.player.start(audioStream);
              			  }
              			  catch (Exception e){}
                    	}
                        frame.add(Popup);
                    }
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
        t.start();
        while(!sysexit){
        	Rectangle screenRectangle = new Rectangle(screenSize);
        	Robot robot = new Robot();
      	    BufferedImage image = robot.createScreenCapture(screenRectangle);
      	    try {
				ImageIO.write(image, "png", new File("bin/Capture.png"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            String filePathAndName = Square.class.getClassLoader().getResource("Capture.png").getPath();
            filePathAndName = filePathAndName == null || filePathAndName.isEmpty() ? "Capture.png" : filePathAndName;
            filePathAndName = filePathAndName.substring(1, filePathAndName.length());
            System.err.println(filePathAndName);
            img0 = cvLoadImage(filePathAndName, 1);
            if (img0 == null) {
                System.err.println("Couldn't load " + "Capture.png");
                continue;
            }
            img = cvCloneImage(img0);
            drawSquares(img, findSquares4(img, storage));
            cvReleaseImage(img);
            cvReleaseImage(img0);
            cvClearMemStorage(storage);
            Thread.sleep(10);
        }
        if (canvas != null) {
            canvas.dispose();
        }
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
