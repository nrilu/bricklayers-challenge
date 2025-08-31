import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Write a description of class Fenster here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Fenster extends JFrame
{
    public Main main = null;
    public Zeichenfeld feld = null;
    public int b,h;
    
    public Fenster(Main m, int b, int h)
    {
        main = m;
        this.b = b;
        this.h = h;
        if(!main.liveGraphics) {
            this.b=200;
            this.h=200;
        }
        initSelf();
        initComponents();
    }
    
      private void initSelf() {
        setLayout(null);
        setVisible(true);
        setSize(b,h);
        setTitle("Mauer Repair");
        setResizable(false);
        setLocation(0,0);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.white);
    }
    
    public void initComponents() {
        Font f = new Font("Georgia", Font.PLAIN, 16);
        UIManager.put("Label.font", f);
        feld = new Zeichenfeld(main, this, b,h);
        feld.setBounds(0,0,b,h);
        add(feld);
        feld.setVisible(true);
        feld.setOpaque(true);
    }
    
    //public void refreshZeichenfeld() {
    //    feld.neuZeichnen();
    //}
}
