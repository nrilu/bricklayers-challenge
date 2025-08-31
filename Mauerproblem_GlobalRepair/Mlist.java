
import java.util.*;

/**
 * The class Mlist acts as an array, with entries Mlist[i].
 * Mlist[i] = A double-linked list containing some number of moves
 */
public class Mlist
{
    public int lt; //Der Listtyp. 0=startlist, 1=endlist, 2=gainlist, 3=longlist
    public int len; //Anzahl der Listen
    public int[] n; //akt. Anzahl an Moves je Liste i 
    public Move[] head;
    public Move[] tail;
    public Move mlast=null; //Der letzte zurückgegeben Move von getnext().
    public int gainIndexShift; //Gainliste hat einen Index-Shift, damit auch negative Gains einen positiven Index haben. 
    public Main main;
    
    public Mlist(int ltype,int len, Main main) {
        this.lt = ltype;
        this.len = len;
        this.main = main;
        gainIndexShift = main.gainIndexShift;
        n = new int[len];
        head = new Move[len];
        tail = new Move[len];
    }

    //Fügt einen Move an ans Ende der i-ten Linked List ein.
    public void insert(Move m, int i) {
        if(lt==2) {
            i=i+gainIndexShift; 
        }
        if (n[i] == 0) {
            head[i] = m;
        }
        else if (n[i] == 1) {
            tail[i] = m;
            m.next[lt] = head[i];
            head[i].prev[lt] = m;
        }
        else {
            Move mNext = tail[i];
            tail[i] = m;
            m.next[lt] = mNext;
            mNext.prev[lt] = m;
        }
        n[i]++;
    }
    
    //Löscht den Move aus der i-ten Linked List.
    //Prüft Sonderfälle falls der Move ganz am Anfang (Head) oder ganz am Ende (Tail) der Liste ist 
    public void remove(Move m, int i) {
        if(lt==2) {
            i=i+gainIndexShift;
        }
        if(n[i]==1) {
            //Der zu löschende Move muss vorne im Head sein, die Liste besteht nur aus diesem einen.
            if(head[i] != m) {
                System.out.println("Error, remove(m,i) wollte Move löschen der nicht mehr da war");
            }
            head[i] = null; 
            //Im Falle der Gainlist: Müssen jetzt den nächst-besten verfügbaren Gain rausfinden, nachdem der bisher beste ausgeführt und gelöscht wurde.
            if(lt==2 && i == main.currMaxgain + gainIndexShift) {
               for(int j=i-1; true; j--) {
                   if(n[j]>0) {
                       main.currMaxgain = j - gainIndexShift;
                       break;
                   }
               }
            }
        }
        else if(head[i] == m) { 
            //Zu löschender Move ist ganz vorne. Es gibt dahinter mind. noch einen weiteren Move
            head[i] = m.prev[lt]; //nächster Move rückt auf
            head[i].next[lt] = null; //Aufrücker hat keinen Vorgänger mehr
            if(n[i] == 2) {
                //Sondefall bei n=2, da kommt der Nachrücker direkt aus dem Tail
                tail[i] = null; //
            }
        }
        else if(tail[i] == m) {
            //Zu löschender Move ist ganz hinten. 
            if(n[i] == 2) {
                //Tail wird nicht neu gefüllt weil der einzige nächste Move bereits im Head ist
                tail[i] = null;
                head[i].prev[lt] = null;
            }
            else {
                //Tail wird neu aufgefüllt mit dem nächst-letzten Move in der Reihe
                tail[i] = m.next[lt];
                tail[i].prev[lt] = null;
            }
        }
        else {
            //Zu löschender Move ist irgendwo in der Mitte der Liste.
            //Verbinder seine Vor- und Nachfolger miteinander, löscht ihn effektiv aus Liste.
            m.next[lt].prev[lt] = m.prev[lt];
            m.prev[lt].next[lt] = m.next[lt];
            m.next[lt] = null;
            m.prev[lt] = null;
        }
        n[i]--;
    }
   
    //Geht die i-te Liste durch und gibt immer den nächsten Move zurück.
    //Beim ersten Aufruf wird mit dem head[i] begonnen.
    //Wenn Liste ganz durch ist wird mlast=null zurückgegeben, das Zeichen dass die Liste durch ist.
    public Move getnext(int i) {
        if(lt==2) {
            i=i+gainIndexShift;
        }
        if(mlast==null) {
            mlast = head[i];
        }
        else {
            mlast = mlast.prev[lt];
        }
        return mlast;
    }
    
    public int printmax=8; //Maximaler Gain für den die Moves gezeigt werden. Macht die graphische Anzeige kompakter indem nicht gesamte Liste gezeigt wird. 
    public void gprint() {
        for(int i=gainIndexShift+printmax; i >= gainIndexShift-printmax;i=i-2) {
            String s=i-gainIndexShift+": ";
            if(n[i]==1) {
                s+="x";
            }
            else if(n[i]>0) {
                Move m0 = head[i];
                int j=0;
                s+="x";
                while(m0.prev[lt] != tail[i]) {
                    m0 = m0.prev[lt];
                    s+="x";
                    j++;
                    if(j>50) {
                        s+=".";
                        break;
                    }
                }
                s+="x";
            }
            else {
                s="-";
            }
            System.out.println(s);
        }
    }
}
