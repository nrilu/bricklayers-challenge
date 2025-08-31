

public class Move
{
    //Fuge zu der dieser Move gehört
    public Fuge f;
    //Neue Position zu der dieser Move seine Fuge verschieben würde
    public int pakt, pneu;
    //Gain den dieser Move bringen würde (positiv = verringerte Kollisionen)
    public int gain;
    //mtype: 0=Swap, 1=nicht-invertierter Tausch, 2=invertierter Tausch
    public int mtype;
    //Falls ein Tausch-Move: Die Länge des 3. (längsten) Blocks
    public int blong;
    //Vor- und Nachfolger dieses Moves in den Moves-Listen
    //Indices 0,1,2,3:  startlist, endlist, gainlist, longlist
    public Move[] next = new Move[4]; 
    public Move[] prev = new Move[4];  
    //Active-Flag für m2-Moves, für Tiefensuche
    public boolean active = true;
    
    public Move(Fuge f, int pneu) {
        this.f = f;
        this.pneu = pneu;
        this.pakt = f.pakt;
        mtype = 0;
    }
    
    public Move(Fuge f, int pneu, int blong, boolean invertiert) {
        this.f = f;
        this.pneu = pneu;
        this.pakt = f.pakt;
        this.blong = blong;
        if(invertiert) {
            mtype=2;
        } 
        else {
            mtype=1;
        }
    } 

    
    
}
