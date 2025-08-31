import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.util.*;

/**
 * Write a description of class Anzeige here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Zeichenfeld_fuerNeuesProjekt extends JLabel
{
    private Main main;
    private Fenster_fuerNeuesProjekt fenster;
    private int b,h;
    
    private Graphics2D g2;
    private BufferedImage bufferimage; 
    
    public Zeichenfeld_fuerNeuesProjekt(Main m, Fenster_fuerNeuesProjekt f, int b, int h)
    {
        main = m;
        fenster = f;
        this.b = b;
        this.h = h;
        initSelf();
    }
    
    private void initSelf() {
        setSize(b,h);
        setOpaque(true);
        
        bufferimage = new BufferedImage(b,h,BufferedImage.TYPE_INT_RGB);
        g2 = (Graphics2D) bufferimage.getGraphics();
        g2.setBackground(Color.black);
    }
    
    public void neuZeichnen() {
        //hier über g2 in bild bufferimage zeichnen
        g2.setPaint(Color.red);

        g2.drawRect(100,100,50,50);
        
        REPAINT();
    }

    
    public void REPAINT() {
        this.repaint();
    }
    
    public void paint(Graphics g) {
        g.drawImage(bufferimage,0,0,null);
    }
}
