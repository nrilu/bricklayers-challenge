import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Write a description of class Fenster here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Fenster_fuerNeuesProjekt extends JFrame
{
    public Main main;
    private Zeichenfeld_fuerNeuesProjekt feld;
    public int b,h;
    
    public Fenster_fuerNeuesProjekt(Main m, int b, int h)
    {
        main = m;
        this.b = b;
        this.h = h;
        initSelf();
        initComponents();
    }
    
      private void initSelf() {
        setLayout(null);
        setVisible(true);
        setSize(920,840);
        setTitle("Mauer Repair");
        setResizable(false);
        setLocation(200,20);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.black);
    }
    
    public void initComponents() {
        Font f = new Font("Georgia", Font.PLAIN, 16);
        UIManager.put("Label.font", f);
        feld = new Zeichenfeld_fuerNeuesProjekt(main, this, b,h);
        feld.setBounds(20,180,b,h);
        add(feld);
        feld.setVisible(true);
        feld.setOpaque(true);
    }
    
    public void refreshZeichenfeld() {
        feld.neuZeichnen();
    }
}
