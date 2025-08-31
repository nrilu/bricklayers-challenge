import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.util.*;
import java.awt.Font;
import java.awt.Graphics;


/**
 * Write a description of class Anzeige here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Zeichenfeld extends JLabel
{
    private Main m = null;
    private Main main;
    private Fenster fenster = null;
    private int b,h;
    
    private Graphics2D g2;
    private BufferedImage bufferimage; 
    private ImageIcon buffericon; 
    
    public int bFactor = 20; //Breite eines n=1 Blocks (Block n=3 ist dann z.B. bFactor*3 breit) /default=2
    public int xMauer = 10; //Mauer x-Startposition
    public int yMauer = 100;//Mauer y-Startposition
    public int blockh = 100; //Höhe der Reihen /default=20
    public int reihenspac = 2; //Abstand zwischen den Reihen
    
    int kollh = 20; //Höhe der Kollisionsbalken
    int kollspac = 1; //Abstand zwischen den Kollisionsbalken
    
    public int PAINT_TIME = 0; //Zählt wie viel Zeit die Graphikausgabe verbraucht
    
    public Zeichenfeld(Main m, Fenster f, int b, int h)
    {
        this.m = m;
        main = m;
        fenster = f;
        this.b = b;
        this.h = h;
        initSelf();
        g2.setFont(font);
    }
    
    private void initSelf() {
        setSize(b,h);
        setOpaque(true);
        
        bufferimage = new BufferedImage(b,h,BufferedImage.TYPE_INT_RGB);
        buffericon  = new ImageIcon(bufferimage); 
        g2 = (Graphics2D) bufferimage.getGraphics();
        g2.setBackground(Color.white);
        g2.setPaint(Color.white);
        g2.fillRect(0,0,b,h);
    }
    
    public void znReihe(int r, int x, int y, int h) {
        Fuge f = m.fugenlist[r];
        int xcurr = x;
        g2.setPaint(Color.black);
        while(f.fr != null) {
            int b = f.br;
            g2.setPaint(Color.gray);
            g2.fillRect(xcurr,y,b*bFactor,blockh);
            g2.setPaint(Color.black);
            g2.drawRect(xcurr,y,b*bFactor,blockh);
            xcurr += b*bFactor;
            f = f.fr;
        }
    }
    
    
    public void znAlleReihen() {
        if(main.liveGraphics) {
            int y = yMauer;
            for(int r=0;r<m.R;r++) {
                znReihe(r,xMauer,y,h);
                y+=blockh + reihenspac;
            }
            REPAINT();
        }
    }
    
    public void znAlleKollisionsbalken() {
        if(main.liveGraphics) {
            int x = xMauer + bFactor;
            g2.setPaint(Color.red);
            for(int i=1;i<m.P-1;i++) {
                znKollisionsbalken(i,x);
                x+=bFactor;
            }
        }
    }
    
    public void znKollisionsbalken(int i, int x) {
        int anzkoll = m.anzF[i];
        for(int j=1;j<=anzkoll;j++) {
            znEinKollBalken(x,j,false);
        }
    } 
    
    public void znEinKollBalken(int x, int collNr, boolean erase) {
        g2.setPaint(Color.red);
        if(collNr==1) {
            g2.setPaint(green);
        }
        if(erase) {
            g2.setPaint(white);
        }
        g2.fillRect(x,yMauer- collNr *(kollh+kollspac),bFactor,kollh);
    }
    
    //Updated die Kollisionsanzeige über der Mauer
    public void updateKollbalken(Move m) {
        int palt = m.pakt;
        int pneu = m.pneu;
        int xalt = xMauer + palt*bFactor;
        int xneu = xMauer + pneu*bFactor;
        int kollAlt = this.m.anzF[palt];
        int kollNeu = this.m.anzF[pneu] + 1;
        znEinKollBalken(xalt, kollAlt, true);
        znEinKollBalken(xneu, kollNeu, false);
        znLiveticker();
    }
    
    public void znLiveticker() {
        if(main.liveGraphics) {
            int yBottom = yMauer + (main.R+1)*(blockh+reihenspac);
            g2.setPaint(white);
            g2.fillRect(xMauer,yBottom-15,200,150);
            g2.setPaint(black);
            g2.drawString("Stress="+this.m.stress, xMauer, yBottom+18);
            g2.drawString("gmoves="+main.gmoves, xMauer, yBottom+36);
            g2.drawString("zmoves="+main.zmoves, xMauer, yBottom+54);
            double movesPerBlock = (main.zmoves+main.gmoves)/(1.0*main.R * main.n);
            //System.out.println(movesPerBlock);
            movesPerBlock = ((int)(movesPerBlock*10.0))/10.0;
            g2.drawString("movesPerBlock="+movesPerBlock, xMauer, yBottom+72);
            if(main.stress == 0) {
                g2.drawString("n="+main.n,xMauer,yBottom+90);
                g2.drawString(" solved in "+(main.gmoves+main.zmoves)+" moves", xMauer, yBottom+115);
                g2.drawString(" saved to "+main.savepath, xMauer, yBottom+150); 
                REPAINT();
            }
            else {
                g2.drawString("n="+main.n,xMauer,yBottom+90);
            }
        }
    }
     
    Font font = new Font("Serif", Font.PLAIN, 20);
    Color orange = Color.orange;
    Color green = Color.green;
    Color black = Color.black;
    Color blue = Color.blue;
    Color cyan = Color.cyan;
    Color verylightblue = new Color(51,204,255);
    Color gray = Color.gray;
    Color lightgray = new Color(204,204,204);
    Color darkcyan = new Color(0,150,150);
    Color white = Color.white;
    Color red = Color.red;
    Color highcol = green;
    
    //Gespeicherte Werte von letztem Move (um ihn wieder grau zu färben und immer nur aktuellen Move farbig zu haben);
    int xLast,yLast,blLast,brLast, poslast; 
    int xfalneuLast, blongLast;
    boolean lastMoveSwap=true;
    boolean firstmove = true;
    
    public void znSwapmove(Move m) {
        if(main.liveGraphics) {
            int r = m.f.r;
            int posAlt = m.f.pakt;
            int posNeu = m.pneu;
            int br = m.f.br; //alte Werte, vor Swap
            int bl = m.f.bl;
            int xAlt = xMauer + posAlt*bFactor;
            int xNeu = xMauer + posNeu*bFactor;
            int y = yMauer + r * (blockh+reihenspac);
            //Blöcke des bevorstehendesn Moves highlighten
            znBlockpaar(xAlt,y, bl, br, highcol,highcol);
            REPAINT();
            sleep(main.wtime);
            //Jetzt Swap zeigen
            taintLastMove();
            if(this.m.stress==2 && m.gain == 2) {
                REPAINT();
                sleep(main.dramatime); /** can increase this to 1..2sec for dramatic effect for the last finishing switch*/
            }
            Color cNew = green;
            if(main.momentanMoveToHoleExecute) {
                cNew = orange;
                sleep(main.moveToHoleTime);
            }
            znBlockpaar(xNeu,y, br, bl, cNew, cNew);
            updateKollbalken(m);
            REPAINT();
            //Werte speichern, um im nächsten Schritt zu wissen was letzter Move war
            xLast = xNeu;
            yLast = y;
            blLast = br; //Auch hier Swap gewollt
            brLast = bl;
            poslast = posNeu;
            lastMoveSwap=true;
        }
    }
    
    public void znTauschmove(Move m) {
        if(main.liveGraphics) {
            int r = m.f.r;
            int fpakt = m.f.pakt;
            int falpakt = this.m.ankerfuge[m.blong][r].pakt;
            int blong = m.blong;
            int br = m.f.br;
            int bl = m.f.bl;
            int xakt = xMauer + fpakt * bFactor;
            int xfal = xMauer + falpakt * bFactor;
            int y = yMauer + r * (blockh+reihenspac);
            //Alte Position vor Move highlighten
            znBlockpaar(xakt,y,bl,br,highcol,highcol);
            znBlock(xfal,y,blong,highcol);
            REPAINT();
            sleep(main.wtime);
            //Neue Position nach Move zeigen
            int xneu = xMauer + m.pneu * bFactor;
            int xLongNeu = xakt - bl*bFactor;
            if(m.mtype==2) { //Manuell hier invertieren Invert-move
                br = m.f.bl;
                bl = m.f.br;
            }
            taintLastMove();
            if(this.m.stress==2 && m.gain == 2) {
                REPAINT();
                sleep(main.dramatime);  /** can increase this to 1..2sec for dramatic effect for the last finishing switch*/
            }
            Color cNew = green;
            if(main.momentanMoveToHoleExecute) {
                cNew = orange;
                sleep(main.moveToHoleTime);
            }
            znBlockpaar(xneu,y,bl,br,cNew,cNew);
            znBlock(xLongNeu,y,blong,cNew);
            updateKollbalken(m);
            REPAINT();
            //Werte speichern, um im nächsten Schritt zu wissen was letzter Move war
            xLast = xneu;
            yLast = y;
            blLast = bl;
            brLast = br;
            poslast = m.pneu;
            xfalneuLast = xLongNeu;
            blongLast=blong;
            lastMoveSwap=false;
        }
    }
    
    //Macht die Blöcke des letzten ausgeführten Moves wieder grau, damit immer nur der aktuelle Move farbig ist.
    public void taintLastMove() {
        Color c = lightgray;
        if(lastMoveSwap && !firstmove) {
            znBlockpaar(xLast,yLast,blLast,brLast,c,c);
        }
        else if (!firstmove) {
            znBlockpaar(xLast,yLast,blLast,brLast,c,c);
            znBlock(xfalneuLast,yLast,blongLast,c);
        } else {
            firstmove = false;
        }
    }
    
    public void znBlockpaar(int xf,int y, int bl, int br, Color cl, Color cr) {
        znBlock(xf-bl*bFactor,y,bl,cl);
        znBlock(xf,y,br,cr);
    }
    
    public void znBlock(int x, int y, int b, Color c) {
        g2.setPaint(c);
        g2.fillRect(x,y,b*bFactor,blockh);
        g2.setPaint(black);
        g2.drawRect(x,y,b*bFactor,blockh);
    }
    
    //Schritt der Tiefensuche visualisieren
    public void zeichneTFSstep(Move mArrival, boolean instack) {
        if(main.znTiefensuche && main.liveGraphics) {
            Color c; 
            int bl, br;
            int xf;
            Fuge f = mArrival.f;
            if(mArrival.mtype == 0) { //Swap
                if(instack) { //Neuer Schritt in Tiefe, Move zeichnen
                    c = cyan; 
                    bl = f.br; //Mit Absicht vertauscht (simulierter Swap)
                    br = f.bl;
                    xf = xMauer + f.pakt * bFactor - br * bFactor + bl * bFactor;
                } else { //Move rückgängig machen
                    c = lightgray; 
                    bl = f.bl; //Wieder ursprünglich
                    br = f.br;
                    xf = xMauer + f.pakt * bFactor;
                }
                int y = yMauer + f.r * (blockh+reihenspac);
                znBlockpaar(xf,y,bl,br,c,c); //Mit Absicht bl,br vertauscht.
            }
            else { //Longtausch
                Move m = mArrival;
                int r = m.f.r;
                int fpakt = m.f.pakt;
                int falpakt = this.m.ankerfuge[m.blong][r].pakt;
                int blong = m.blong;
                br = m.f.br;
                bl = m.f.bl;
                int xakt = xMauer + fpakt * bFactor;
                int xfal = xMauer + falpakt * bFactor;
                int y = yMauer + r * (blockh+reihenspac);
                int xneu = xMauer + m.pneu * bFactor;
                int xLongNeu = xakt - bl*bFactor;
                if(instack) { //Tauschmove ausführen (zeichnen)
                    if(m.mtype==2) { 
                        br = m.f.bl;
                        bl = m.f.br;
                    }
                    c = darkcyan;
                    znBlockpaar(xneu,y,bl,br,c,c);
                    znBlock(xLongNeu,y,blong,c);
                } else { //Situation vor Tauschmove zeichnen
                    c = lightgray;
                    znBlockpaar(xakt,y,bl,br,c,c);
                    znBlock(xfal,y,blong,c);
                    //this.m.sleep(1000);
                }
            }
            REPAINT();
        }
    }
    
    public void zeichneTFSfuge(Fuge f, boolean initial, boolean in) {
        if(main.znTiefensuche && main.liveGraphics) {
            if(initial) {
                 int xf = xMauer + f.pakt * bFactor;
                 int y = yMauer + f.r * (blockh+reihenspac);
                 znBlockpaar(xf,y,f.bl,f.br,cyan,cyan);
            }
            if(in) {
                g2.setPaint(red);
            }
            else {
                g2.setPaint(black);
            }
            int fbreite = 3;
            int xf = xMauer + f.pakt * bFactor -1;
            int y = yMauer + f.r * (blockh+reihenspac);
            g2.fillRect(xf,y,fbreite,blockh); 
            
            REPAINT();
        }
    }
    
    public void resetZeichenfeld() {
        if(main.liveGraphics) {
            g2.setPaint(white);
            g2.fillRect(0,0,b,h);
            REPAINT();
        }
    }
    
    public void sleep(int ms) {
        this.m.sleep(ms);
    }
    
    public void znTS_Infopfeil(int pos, boolean hole, boolean erase) {
        if(main.znTiefensuche && main.liveGraphics) {
            Color c = blue;
            if(hole) {
                c = green;
            }
            if(erase) {
                c = white;
            }
            int xPos = xMauer + bFactor * pos;
            int y1 = yMauer - 2*kollh - 30;
            int y2 = yMauer - 2*kollh - 20;
            drawArrowLine(c, xPos, y1, xPos, y2, 10, 5); 
        }
    } 
    
        /**
     * https://stackoverflow.com/questions/2027613/how-to-draw-a-directed-arrow-line-in-java#2027641
     * Draw an arrow line between two points.
     * @param g the graphics component.
     * @param x1 x-position of first point.
     * @param y1 y-position of first point.
     * @param x2 x-position of second point.
     * @param y2 y-position of second point.
     * @param d  the width of the arrow.
     * @param h  the height of the arrow.
     * 
     */
    private void drawArrowLine(Color c, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;
    
        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;
    
        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;
    
        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};
        
        g2.setPaint(c);
        g2.setStroke(new BasicStroke(6));
        g2.drawLine(x1, y1, x2, y2);
        g2.fillPolygon(xpoints, ypoints, 3);
        g2.setStroke(new BasicStroke(1));
    }   
    

    public void REPAINT() {
        
        //Neue Weg, der nicht mehr flackert:
        this.repaint();
        
        //Alter Weg, der geflacktert hat:
        // BufferIcon buffericon = new ImageIcon(bufferimage); 
        // this.setIcon(buffericon);
        // this.paintImmediately(0,0,b,h);
        
        
    }
    
    public void paint(Graphics g) {
        g.drawImage(bufferimage,0,0,null);
    }
}
