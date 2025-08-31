
/**
 * Eine Fuge in einer Reihe r
 */
public class Fuge
{
    //Reihe in der die Fuge ist
    public int r;
    //Position der Fuge in ihrer Reihe 
    public int pakt;
    //Blöcke links und rechts der Fuge
    public int bl, br;
    //Nachbarfugen links und rechts 
    public Fuge fl,fr;
    //Moves die diese Fuge machen kann;
    //m0: Lokaler Swap (bl<-->br)
    //m1: Weiter Tausch, nicht invertiert (bl+br <--> b3)
    //m2: Weiter Tausch, invertiert (br+bl <--> b3)
    public Move m0, m1, m2;
    //Sonderfall wenn Fuge nur Dummy ist
    //(betrifft Positionen p=0 und p=pmax, ganz am Anfang und am Ende der Mauer)
    public boolean dummy;
    //Active-Flag für Tiefensuche
    public boolean active = true;
    
    public Fuge(int r, int pakt, int bl, int br, boolean dummy)
    {
        this.r = r;
        this.pakt = pakt;
        this.bl = bl;
        this.br = br;
        this.dummy = dummy;
    }

}
