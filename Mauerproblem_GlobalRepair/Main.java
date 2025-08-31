import java.util.*;
import java.io.*;

/**
 * Niccolò Rigi-Luperti
 * Idee: ~Aug 2018
 * Ausgearbeitet: Nov-Dez 2020
 */
public class Main
{
    //Parameter
    public int n = 36; //Desired Wall size
    
    public static boolean liveGraphics =  false; //live solving animation 
    public int wtime = 500; //wait time in milliseconds for solving animation
    
    //Größen
    public int gainIndexShift = 1000; //Maximal betrachtete Gains 
    public int R = (int)(n/2) + 1; //Anzahl Reihen
    public int P = n*(n+1)/2 + 1; //Anzahl Positionen entlang Mauer, an denen Fugen sein könnten (inklusive Dummy-Positionen)
    public int F = R*(n-1); //Anzahl tatsächlich vorhandene Fugen  
    
    //Datenstrukturen
    public int[] anzF = new int[P];
    public Mlist startlist = new Mlist(0,P,this);
    public Mlist endlist = new Mlist(1,P,this);
    public Mlist gainlist = new Mlist(2,gainIndexShift*2 + 1, this);
    public Mlist[] longlist = new Mlist[R];
    public Fuge[] fugenlist = new Fuge[R]; //Erste Fuge je Reihe 
    public Fuge[][] ankerfuge = new Fuge[n+1][R]; //Ankerfuge zu jedem Block je Reihe
    
    //Livezähler
    public int stress = 0; //Stress der Mauer
    public int currMaxgain = 0; //Gain des aktuell besten verfübaren Moves 
    public int gmoves = 0; //Anzahl ausgeführte Moves mit gain > 0
    public int zmoves = 0; //Anzahl ausgeführte Moves mit gain == 0
    public int NR = 0; //Durchnummerierung der Moves
    public int LASTNR = 0;
    public int lastKollPos = 0; //Position der Kollisoin bei stress==2
    public double mpb = 0; //Avg. exectued Moves per Block
    public double t0 = 0;
    public double CALC_TIME = 0; //Runtime vom tatsächlichen Algorithmus, Graphik rausgerechnet
    public int lastStress = 999999999;
    
    //Statistikzähler
    double tPre6 = 0;
    double t6 = 0;
    double t4 = 0;
    double t2 = 0;
    double searchDur = 0;
    int searches = 0;
    double last6toTotalSum=0;
    boolean finished=true;
    double succSolTime=0;
    double succSolNmb=0;
    double succMPB=0;
    double failCounter=0;
    double avgSolTime = 0; 
    
    //Graphik & Utils
    public Fenster fenst = new Fenster(this,1600,850);
    PrintWriter out;
    SystemInfo SysInfo = new SystemInfo();
    public long file_timetag; //Timetag nanoTime() um log und save-file unique zu machen
    
    //Zeichendauern (ms)
    public int sleeptime = 0; //Tiefensuche 
    
    public int dramatime = 1500; //Time-delay for very last finishing move 
    public int moveToHoleTime = 0; //Move to Hole Exectue
    
    //Hyper-Parameter
    int runs = 1;
    double mpbMax = 10; //moves per block(?) Glaube Abbruchkriterium falls zu lange blöcke bewegt ohne Fortschritt
    double cutFactor=2;
    //double redoTime = 0.060; //sec
    
    //Tiefensuche-Extra (falls searchOnset>0)
    int searchOnset = 0; //Ob zusätzlich Tiefensuche gemacht werden soll sobald Stress < searchOnset. Keine Tiefensuche bei searchOnset=0.
    int searchPeriod = (int) (n*10);
    int maxSteps = 1000; 
    int maxdepth = 10; 
    
    
    public Main()
    {
        resetNewSize();
        runNewSize(n);
        
        
        /**  Full-Performance-Benchmark
        int[] N = {700,800,900,1000,1200,1400,1600,1800,2000};
        for(int i=0;i<N.length;i++) {
            
            
            if(N[i]>=1000) {
                runs=3;
            }
            
            resetNewSize();
            runNewSize(N[i]);
            resetNewSize();
            runNewSize(N[i]+1);
        }
        */
    }
    
    public void runNewSize(int n) {
        this.n = n;
        try {
            file_timetag = System.nanoTime();
            out = new PrintWriter("logs/log_n="+n+"_"+file_timetag+".txt");
        } catch(Exception e) {
            System.out.println("I/O error");
        }
        double tt0 = System.nanoTime();
        print("n="+n);
        print(runs+" runs");
        print(mpbMax+" mpbMax");
        //print(avgSolTime+ "sec allowed first duration");
        print(cutFactor+" cutFactor");
        print(searchOnset+" SearchOnset");
        if(searchOnset>0) {
            print(searchPeriod+" Period");
            print(maxSteps+" Steps");
            print(maxdepth+" Depth");
        }
        print("liveGraphics "+liveGraphics);
        print(">>>");
        double tLastStart=0;
        avgSolTime = 999999;
        for(int j=0;j<runs;j++) {
            resetNewTry();
            //Initialisieren
            t0 = System.nanoTime();
            if(finished) {
                tLastStart=t0;
                finished=false;
            }
            tPre6 = System.currentTimeMillis();
            initMauer();
            fenst.feld.znAlleReihen();
            fenst.feld.znAlleKollisionsbalken();
            //Lösen
            while(stress > 0 && mpb < mpbMax && (System.nanoTime()-t0)/1000000000.0 < avgSolTime*cutFactor) {
                doNextMove();
                
                if(stress < lastStress &&(stress%100==0 || stress < 100)) {
                    lastStress=stress;
                    //print("stress:"+stress+"  gainmoves:"+gmoves+"   zeromoves:"+zmoves);
                }
                if(stress<=searchOnset && (zmoves+gmoves)%searchPeriod == 0) {
                    searches++;
                    double tS = System.currentTimeMillis();
                    doHoleSearches();
                    searchDur+= System.currentTimeMillis() - tS;
                }
                if(stress==6 && t6==0) {
                    t6 = System.currentTimeMillis();
                    if(avgSolTime==999999) {
                        avgSolTime = cutFactor*(t6-tPre6)/1000.0; //Heuristik zum Abschätzen einer sinnvollen erlaubten Laufzeit. Zeit für den letzten Schritt (stress 6 -> 0)
                        //print("heuristik avgSolTime "+avgSolTime);
                        if(avgSolTime <0.01) {
                            avgSolTime = 0.01;
                        }
                        //sollte nur noch höchstens so langen dauern wie die Laufzeit von Beginn bis zu stress==6.
                    }
                }
                if(stress==4 && t4==0) {
                    t4 = System.currentTimeMillis();
                }
                if(stress==2 && t2==0) {
                    t2 = System.currentTimeMillis();
                }
            }
            
            //Lösung verwalten
            double dur = (System.nanoTime() - t0)/1000000000.0; //runtime in sec
            //double PAINT_TIME = fenst.feld.PAINT_TIME /  1000000000.0; //reine Zeichen-Zeit in sec
            //CALC_TIME = dur - PAINT_TIME; //reine Rechenzeit für eigentlichen Algorithmus
            
            //print(""+dur);
            //dur = ((int)(dur*100.0))/100.0;
            double durPre6 = (t6 - tPre6)/1000.0;
            double dur6 = (t4-t6) / 1000.0;
            double dur4 = (t2-t4) / 1000.0;
            double dur2 = (System.currentTimeMillis() - t2) / 1000.0;
            double dur6to0 = (System.currentTimeMillis() - t6) / 1000.0;
            double last6ToTotal = dur6to0 / dur;
            if(last6ToTotal<1) {
                last6toTotalSum += last6ToTotal;
            }
            //print(""+last6toTotalSum);
            
            /*
            print("No.Searches: "+searches);
            print("durPre6:"+durPre6);
            print("dur6:   "+dur6);
            print("dur4:   "+dur4);
            print("dur2:   "+dur2);
            print("searchDur:"+(searchDur/1000.0));
            print("durSum: "+((durPre6+dur6+dur4+dur2)));
            print("last6ToTotal: "+last6ToTotal);
            print("last3tototalSUM: "+last6toTotalSum);
            */
            if(stress==0) {
                finished=true;
                
                double mpbRounded = ((int)(mpb*10))/10.0; //total moves per block
                //print((gmoves+zmoves)+" ("+mpbRounded+" mpb) ("+dur+" sec)");
                double totalDur = (System.nanoTime()-tLastStart)/1000000000.0;
                totalDur = ((int)(totalDur*1000.0))/1000.0;
                
                verifyResults();
                print("g moves "+gmoves);
                print("z moves "+zmoves);
                
                print("");
                print("total dur: "+totalDur+" s");
                succSolTime+=dur;
                succSolNmb++;
                avgSolTime=succSolTime/succSolNmb;
                //print("new avgSolTime "+avgSolTime);
                succMPB+=mpb;
                mauerToFile(); /** Speichern **/
                
                fenst.feld.znLiveticker();
            }
            else {
                //print("    redo ("+dur+" sec)");
                j--; //Durchgang wiederholen
                failCounter++;
            }
        }
        double durTotal = (System.nanoTime()-tt0)/1000000000;
        double durAvg = ((int)((durTotal/runs)*100000.0))/100000.0;
        double last6ToTotalAvg = last6toTotalSum/(1.0*runs);
        double failRatio = failCounter/(failCounter+runs);
        double avgsuccMPB = succMPB/runs;
        avgSolTime = ((int)(avgSolTime*1000.0))/1000.0;
        last6ToTotalAvg = ((int)(last6ToTotalAvg*1000.0))/1000.0;
        failRatio = ((int)(failRatio*1000.0))/1000.0;
        avgsuccMPB = ((int)(avgsuccMPB*1000.0))/1000.0;
        //print("RawCalcTime  "+CALC_TIME);
        //print("GraphicsTime "+fenst.feld.PAINT_TIME /  1000000000.0);
        
        print("solved n="+n);
        //print("AvgDuration "+durAvg+" s");
        print("SingleSol   "+avgSolTime+" s");
        //print("avgSuccMPB  "+avgsuccMPB);
        //print("last3Anteil "+last6ToTotalAvg);
        print("failRatio   "+failRatio);
        print("");
        print("");
        out.flush();
        out.close();
    }
    
    public void print(String s) {
        System.out.println(s);
        out.println(s);
    }
    
    public void resetNewSize() {
        searchDur = 0;
        searches = 0;
        last6toTotalSum=0;
        finished=true;
        succSolTime=0;
        succSolNmb=0;
        succMPB=0;
        failCounter=0;
    }
    
    public void resetNewTry() {
        //Feste Werte
        gainIndexShift = 1000; //Maximal betrachtete Gains 
        R = (int)(n/2) + 1; //Anzahl Reihen
        P = n*(n+1)/2 + 1; //Anzahl Positionen entlang Mauer, an denen Fugen sein könnten (inklusive Dummy-Positionen)
        F = R*(n-1); //Anzahl tatsächlich vorhandene Fugen  
    
        //Datenstrukturen
        anzF = new int[P];
        startlist = new Mlist(0,P,this);
        endlist = new Mlist(1,P,this);
        gainlist = new Mlist(2,gainIndexShift*2 + 1, this);
        longlist = new Mlist[R];
        fugenlist = new Fuge[R]; //Erste Fuge je Reihe 
        ankerfuge = new Fuge[n+1][R]; //Ankerfuge zu jedem Block je Reihe
    
        //System.out.println(SysInfo.MemInfo());
        
        //Livezähler
        stress = 0; //Stress der Mauer
        currMaxgain = 0; //Gain des aktuell besten verfübaren Moves 
        gmoves = 0; //Anzahl ausgeführte Moves mit gain > 0
        zmoves = 0; //Anzahl ausgeführte Moves mit gain == 0
        NR = 0;
        LASTNR = 0;
        lastKollPos = 0; //Position der Kollisoin bei stress==2
        mpb = 0;
        
        tPre6 = 0;
        t6 = 0;
        t4 = 0;
        t2 = 0;
        searchDur = 0;
        searches = 0;
        //Graphik
        fenst.feld.resetZeichenfeld();
        
        //Hilfs-Strings für Konsolenausgabe
        if(spaces==null) {
            spaces = new String[200];
            String s="";
            for(int i=0;i<spaces.length;i++) {
                s+=" ";
                spaces[i] = s;
            }
        }
    }
    
    public void doNextMove() {
        Move m = gainlist.head[currMaxgain+gainIndexShift];
        boolean wasGainMove = m.gain > 0;
        execute(m);
        if(wasGainMove) {
            gmoves++;
        } else {
            zmoves++;
        }
        mpb = (zmoves+gmoves)/(1.0*R*n);
    }
    
    public void initMauer() {
        //Blocklängen 1..n erzeugen
        ArrayList<Integer> b = new ArrayList<Integer>(n);
        for(int i=1; i<=n; i++) {
            b.add(i);
        }
        //Longlists iniitieren
        for(int r=0;r<R; r++) {
            longlist[r] = new Mlist(3,n+1,this);
        }
        //Mauer Reihe für Reihe aufbauen
        for(int r=0; r<R; r++) {
            Collections.shuffle(b); //Reihe randomisieren
            Fuge f0 = new Fuge(r, 0, 0, b.get(0), true); //Erste Fuge = Dummyfuge
            fugenlist[r] = f0;
            ankerfuge[b.get(0)][r] = f0;
            int pakt = b.get(0);
            Fuge fprev = f0;
            for(int i=1; i<n; i++)  {
                Fuge f = new Fuge(r, pakt, b.get(i-1), b.get(i), false);
                anzF[pakt]++;
                f.fl = fprev;
                fprev.fr = f;
                ankerfuge[b.get(i)][r] = f;
                fprev = f;
                pakt += b.get(i);
            }
            Fuge fend = new Fuge(r,pakt,b.get(n-1),0,true);
            fend.fl = fprev;
            fprev.fr = fend;
            if(pakt != P-1) {
                print("Error pakt != P-1 !");
            }
        }
        //Stress initiieren
        stress = 0;
        for(int p=1; p<=P-1; p++) {
            stress += stress(anzF[p]);     
        }
        //Korrigiere Stress für die auch noch am Ende vorhandenen F Fugen die immer da sein werden
        stress = stress - F;  
        //Moves initiieren
        for(int r=0; r<R; r++) {
            Fuge f = fugenlist[r].fr;
            while(f.fr != null) {
                recalcMoves(f);
                f = f.fr;
            }
        }
        if(succSolNmb==0) {
            print("Memory after initialisation:");
            print(SysInfo.MemInfo());
        }
        //showMauer();
    }
    
    public void clearConsole() {
        System.out.print('\u000C');
    }
    
    public void execute(Move m) {
        if(m.mtype==0) {
            executeSwapmove(m);
        }
        else {
            executeLongmove(m);
        }
    }
    
    public void executeSwapmove(Move m) {
        fenst.feld.znSwapmove(m);
        anzF[m.pakt]--;  
        anzF[m.pneu]++;
        stress -=m.gain;
        Fuge f = m.f;
        //print("executed SWAP: r "+f.r+", swap "+f.bl+"<->"+f.br+", fpos "+f.pakt+"("+m.pakt+")-->"+m.pneu+"  gain="+m.gain+"  NR("+m.nr+")");
        //print("               flpos="+f.fl.pakt+",  frpos="+f.fr.pakt);
        f.pakt = m.pneu;
        int blTemp = f.bl;
        int brTemp = f.br;
        f.bl = brTemp; //Swap an Fuge
        f.br = blTemp;
        f.fl.br = f.bl; //Swap mitteilen an Nachbarn
        f.fr.bl = f.br;
        ankerfuge[f.bl][f.r] = f.fl; //Geswappte Blöcke haben neue Ankerfuge
        ankerfuge[f.br][f.r] = f;
        
        //Neue Moves für Fugen denen die Blöcke verändert wurden
        recalcMoves(f);
        recalcMoves(f.fl);
        recalcMoves(f.fr);
        recalcAllLongtauschs(blTemp, f.r);
        recalcAllLongtauschs(brTemp, f.r);
        //Neue Gains für Moves bei denen die Start- oder Zielposition jetzt eine neue Fugenanzahl hat
        updateAllGains(m.pakt, m.pneu);
    }
    
    public void executeLongmove(Move m) {
        fenst.feld.znTauschmove(m);
        anzF[m.pakt]--; 
        anzF[m.pneu]++;
        stress -= m.gain;
        Fuge f = m.f;
        
        f.pakt = m.pneu;
        Fuge fl = f.fl;
        Fuge fr = f.fr;
        int bl = f.bl;
        int br = f.br;
        int blong = m.blong;
        Fuge fal = ankerfuge[blong][f.r];
        Fuge far = fal.fr;
        //Blong einfügen wo vorher f war
        f.fl.br = blong;
        f.fr.bl = blong;
        f.fl.fr = fr;
        f.fr.fl = fl;
        ankerfuge[blong][f.r] = fl;
        //f einfügen wo vorher blong war
        if(m.mtype == 2) { //Blöcke von f vor dem Tausch swappen falls inverse Longtausch. 
            f.bl = br; //Müssen die Blöcke am Körper der Fuge selbst swappen! Nicht nur als temporäre Werte hier in der Methode.
            f.br = bl;
        }
        fal.br = f.bl;
        far.bl = f.br;
        fal.fr = f;
        far.fl = f;
        f.fl = fal;
        f.fr = far;
        ankerfuge[f.bl][f.r] = fal;
        ankerfuge[f.br][f.r] = f;
        //Neue Moves
        recalcMoves(f);
        recalcMoves(f.fl);
        recalcMoves(f.fr);
        recalcMoves(fl);
        recalcMoves(fr);
        recalcAllLongtauschs(bl,f.r);
        recalcAllLongtauschs(br,f.r);
        recalcAllLongtauschs(blong,f.r);
        //Neus Gains
        updateAllGains(m.pakt, m.pneu);
    }
    
    public void sanity(Fuge f) {
        if(f.pakt == 1) {
        
        } 
        if(!f.dummy && (f.pakt < f.fl.pakt || f.pakt > f.fr.pakt)) {
            print("Fugen fpakt problem");
        }
    } 
    
    public void completeSanity() {
        for(int r=0;r<R;r++) {
            Fuge f = fugenlist[r].fr;
            while(f.fr != null) {
                sanity(f);
                if(f.m0 != null && (f.m0.pneu < 1 || f.m0.pneu >= P-1)) {
                    int a = 0;
                }
                if(f.m1 != null && (f.m1.pneu < 1 || f.m1.pneu >= P-1)) {
                    int a = 0;
                }
                if(f.m2 != null && (f.m2.pneu < 1 || f.m2.pneu >= P-1)) {
                    int a = 0;
                }
                f = f.fr;
            }
        }
    }
    
    //Fügt einen neuen Move in alle Listen ein
    public void insertInLists(Move m) {
        startlist.insert(m,m.pakt);
        endlist.insert(m,m.pneu);
        /*Erkenntnis aus früherem Debugging: HIER KEINE GAINLIST INSERT!!!! PASSIERT BEREITS BEI GAINUPDATE() !! SONST DOPPELTES INSERTEN !!!*/ //gainlist.insert(m,m.gain);
        //Bei der longlist reicht einer der beiden Longmoves als Repräsentant, wir nehmen den m1.
        if(m.mtype == 1) {
            longlist[m.f.r].insert(m, m.blong); 
        }
    }
    
    public void removeFromAllLists(Move m) {
        startlist.remove(m,m.pakt);
        endlist.remove(m,m.pneu);
        gainlist.remove(m,m.gain);
        if(m.mtype==1) {
            longlist[m.f.r].remove(m,m.blong);
        }
    }
    
    public void recalcMoves(Fuge f) {
        if(!f.dummy) {
            recalcSwapmove(f);
            recalcLongtauschs(f);
        }
    }
    
    //Berechnet für eine Fuge ihren Swapmove neu
    public void recalcSwapmove(Fuge f) {
        //Bisherigen Move löschen
        if(f.m0 != null) {
            removeFromAllLists(f.m0);
            f.m0 = null;
        }
        //Neuen Move generieren
        int pneu = f.pakt - f.bl + f.br;
        Move m0 = new Move(f,pneu);
        NR++;
        f.m0 = m0;
        updateGain(m0,true);
        insertInLists(m0);
    }
    
    //Berechnet für eine Fuge ihre beiden Longtauschs-Moves neu, falls diese möglich sind
    public void recalcLongtauschs(Fuge f) {
        //Bisherige Moves löschen
        if(f.m1 != null) {
            removeFromAllLists(f.m1);
            f.m1 = null; 
        }
        if(f.m2 != null) {
            removeFromAllLists(f.m2);
            f.m2 = null;
        }
        //Neuen Move generieren. Longtausch nur möglich wenn beiden kurzen Blöcke durch einen längeren Blöck ausgetauscht werden können (bl+br <= n)
        int blong = f.bl + f.br;
        if(blong <= n && !f.dummy) {
            Fuge fal = ankerfuge[blong][f.r];
            int pneu1 = fal.pakt + f.bl; //Nicht-invertiertr Move  (m1)
            int pneu2 = fal.pakt + f.br; //Invertierter Longtausch (m2)
            Move m1 = new Move(f,pneu1,blong,false);
            Move m2 = new Move(f,pneu2,blong,true);
            f.m1 = m1;
            f.m2 = m2;
            updateGain(m1,true);
            updateGain(m2,true);
            insertInLists(m1);
            insertInLists(m2);
            NR = NR+2;
        }
    }    
    
    //Wahlt in einer Reihe alle Fugen aus die einen Longtausch mit dem Ziel blong hatten und berechnet ihre Longtausch neu 
    //(weil sich die Position von blong geändert hat und damit auch die Longtauschs).
    public void recalcAllLongtauschs(int blong, int r) {
        int anz = longlist[r].n[blong];
        int i = 0;
        while(longlist[r].getnext(blong) != null) {
            recalcLongtauschs(longlist[r].mlast.f);
            i++;
            //Jeder recalcLongtausch() löscht den entsprechenden Move aus der Longlist und fügt ans Ende der Longlist den neu berechneten Move der selben Fuge ein.
            //Wir müssen deshalb genau dann aufhören zu löschen wenn wir den letzten alten Move gelöscht haben. Die Liste ist dann einmal ganz erneuert.
            if(i==anz) { 
                longlist[r].mlast = null;
                break;
            }
        }
    }
    
    //Berechnet für all diejenigen Moves ihre Gains neu die eine Position mit dem letzten executeten Move geteilt haben.
    //Diese Moves bleiben von ihrer Struktur her bestehen, aber ihre Start- oder Zielposition hat jetzt eine Fugen mehr oder weniger, das ändert ihren Gain.
    public void updateAllGains(int pakt, int pneu) {
        //4 Kombinationsmöglickeiten. Executeter Move hat zwei Positionen verändert (pakt, pneu) und restliche Moves könnten starten als auch enden auf einer der beiden.  
        while(startlist.getnext(pakt) != null) {
            updateGain(startlist.mlast,false);
        }
        while(startlist.getnext(pneu) != null) {
            updateGain(startlist.mlast,false);
        }
        while(endlist.getnext(pakt) != null) {
            updateGain(endlist.mlast,false);
        }
        while(endlist.getnext(pneu) != null) {
            updateGain(endlist.mlast,false);
        }
        //Etwas unschön programmiert: Der Befehl mylist.getnext(i) setzt in mylist eine Variable mlast auf den nächsten Eintrag von der i-ten Liste in mylist
        //Auf dieses geänderte mlast wird dann hier direkt zugegriffen.
    }
    
    //Berechnet den Gain für einen Move neu. Rechnet dafür hyptothetisch durch wie sich der Stress der Mauer ändern würde wenn man den Move executen würde.
    public void updateGain(Move m, boolean completelyNew) {
        if(!completelyNew) {
            gainlist.remove(m,m.gain);
        }
        int pakt = m.pakt;
        int pneu = m.pneu;
        int A = anzF[pakt];
        int B = anzF[pneu];
        int dStress_leave  = stress(A-1) - stress(A);
        int dStress_arrive = stress(B+1) - stress(B);
        int gain = - (dStress_leave + dStress_arrive);
        m.gain = gain;
        gainlist.insert(m,m.gain);
        if(gain > currMaxgain) {
            currMaxgain = gain;
        }
    }
    
    //Kostenfunktion für den Stress. i = Anzahl Fugen an einer Position. 
    public int stress(int i) {
        return i*i;
    } 
    
    
    
    //Tiefensuche von Kollisionen zu Löchern
    int depth;
    int steps;
    int foundDepth;
    boolean foundHole = false;
    int holePos = 0;
    boolean momentanMoveToHoleExecute=false;
    ArrayList<Move> movesToHole;
    String[] spaces;
    public static boolean znTiefensuche = true;
    public static boolean znMovesToHole = true;
    
    //Startet von allen Positionen der Mauer an denen es eine Kollision gibt eine Hole-Search für die beiden Fugen der Kollision
    public void doHoleSearches() {
        for(int i=1;i<P-1;i++) {
                if(anzF[i]>1) {
                    startHoleSearch(i,false); //Lösungsuche für erste Fuge
                    if(anzF[i]>1) {
                        startHoleSearch(i,true); //Lösungssuche für zweite Fuge, falls erste sich nicht lösen lies
                    }
                }
        }
        
    }
    
    public void startHoleSearch(int pos, boolean second) {
        //Parameter resetten
        depth = -1;
        steps=0;
        foundHole = false;
        foundDepth = 0;
        movesToHole = null;
        fenst.feld.znTS_Infopfeil(pos,false,false);
        if(liveGraphics) {
            String s = "1.";
            if(second) {
                s = "2.";
            }
            System.out.println("Start "+s+" search at "+pos+", anzF="+anzF[pos]);
        }
        //Suche starten
        searchHole(pos, null, true, second);
        //Suchergebnis auswerten
        if(!foundHole) {
            //print("steps: "+steps);
        }
        if(foundHole) {
            //sleep(10000);
            fenst.feld.znTS_Infopfeil(holePos,true,false);
            //Alle Moves des erfolgreichen Tiefensuche-Pfads ausführen
            if(znMovesToHole) {
                momentanMoveToHoleExecute=true;
            }    
            //System.out.println(movesToHole);
            Collections.reverse(movesToHole);
            //System.out.println(movesToHole);
            for(Move m : movesToHole){
                //System.out.println("stress:"+stress+" next move gain:"+m.gain);
                while(startlist.getnext(m.pakt) != null) {
                    Move mKand = startlist.mlast;
                    if(mKand.mtype == m.mtype && mKand.pneu == m.pneu && mKand.f.r == m.f.r && mKand!=m) {
                        m = mKand;
                        System.out.println("");
                        System.out.println("Neuen Kandidaten für gespeicherten Move gefunden");
                        System.out.println("");
                        startlist.mlast = null;
                        break;
                    }
                }
                
                if(m.gain==-2) {
                    System.out.println(" # # # # # Warning, next move gain = "+m.gain);
                    System.out.println("");
                    sleep(2000);
                }
                //Prüfen ob die Bewegung von Move m mittlwerile schon durch einen neuen geupdatete Move abgebildet wird 
                //(selbe Start-Ziel Bewegung, aber möglicherweise neue Fugen-Randbedingungen die im neuen Move korrekt abgebildet sind, im alten aber nicht mehr). 
                
                
                execute(m);
                fenst.feld.znLiveticker();
            }
            momentanMoveToHoleExecute = false;
            fenst.feld.znTS_Infopfeil(holePos,true,true);
            
            if(liveGraphics) {
                System.out.println("stress:"+stress);
                verifyResults();
            }
        }
        fenst.feld.znTS_Infopfeil(pos,false,true);
    }
    
    //Tiefensuche. Nimmt den aktuellen statischen Zustand aller Moves und schaut ob es einen verbindenen Weg zwischen Kollisionen und Löchern gibt.
    //Hier werden Moves weder erzeugt noch vernichtet, die Suche geht sie nur "kalt" simuliert durch und markiert besuchte als inaktiv.
    //Dadurch dünnen die vorhandenen Moves immer mehr aus je tiefer der Suchpfad geht.
    public void searchHole(int pos, Move mLast, boolean initial, boolean second) {
        depth++;
        steps++;
        if(!initial) {
            //print("step "+steps+"  depth "+depth+" "+spaces[depth]+pos+"(m"+mLast.mtype+")");
        }
        if(!initial) {
            fenst.feld.zeichneTFSstep(mLast,true);
            sleep(sleeptime);
        }
        
        if(anzF[pos] == 0) {
            foundHole = true;
            holePos = pos;
            movesToHole = new ArrayList<Move>(depth);
            movesToHole.add(mLast);
            if(liveGraphics) {
                print("steps: "+steps+" at "+depth+" depth  # # # # # # found Hole # # # # #");
            }
        }
        else if (!initial && anzF[pos] >= 2) {
            //Auf andere Kollision gestoßen. Diesen Aufruf abbrechen, ausser es ist der allererste Aufruf, denn da starten wir ja extra bei einer Kollision.
        }
        else if(depth > maxdepth || steps > maxSteps) {
            //Suche lokal abbrechen, maximale Suchtiefe erreicht
        }
        else { //genau eine Fuge an neuer Position
            Fuge f = startlist.head[pos].f;
            if(initial && second) { 
                //Sonderfall zweite Suche: Starten die Suche von der zweiten statt der ersten Fuge der Kollisionsstelle
                boolean changed = false;
                while(startlist.getnext(pos) != null) {
                    if(startlist.mlast.f != f) {
                        f = startlist.mlast.f;
                        startlist.mlast = null;
                        changed = true;
                        break;
                    }
                }
                if(!changed) { //Sanity Check
                    System.out.println("Error Tiefensuche falsche Fugenauswahl bei Start: Zweite Fuge = Erste Fuge");
                }
            }
            fenst.feld.zeichneTFSfuge(f,initial,true);  
            sleep(sleeptime);
            if(f.active) {
                //Jede Fuge kann nur einmal entlang eines Pfades benutzt werden
                f.active = false;
                f.fl.active = false;
                f.fr.active = false;
                //Bewegte Blöcke stehen auch nicht mehr für zukünftige Longtauschs zur Verfügung
                setLongtauschActivity(f.bl, f.r, false);
                setLongtauschActivity(f.br, f.r, false);
                searchHole(f.m0.pneu, f.m0, false, second);
                if(!foundHole && f.m1 != null && f.m1.active) {
                    Fuge fal = ankerfuge[f.m1.blong][f.r];
                    fal.active = false;
                    fal.fr.active = false;
                    setLongtauschActivity(f.m1.blong, f.r, false);
                    searchHole(f.m1.pneu, f.m1, false, second);
                    if(!foundHole) {
                        searchHole(f.m2.pneu, f.m2, false, second);
                    }
                    //Rekursion rückgängig
                    fal.active = true;
                    fal.fr.active = true;
                    setLongtauschActivity(f.m1.blong, f.r, true);
                }
                //Rekursion rückgängig
                f.active = true;
                f.fl.active = true;
                f.fr.active = true;
                setLongtauschActivity(f.bl, f.r, true);
                setLongtauschActivity(f.br, f.r, true);
                if(foundHole && !initial) {
                    movesToHole.add(mLast);
                    //print(spaces[depth]+" # # # found Hole # # #");
                }
            }
            fenst.feld.zeichneTFSfuge(f,initial,false);
            sleep(sleeptime);
        }
        if(!initial && !foundHole) {
            fenst.feld.zeichneTFSstep(mLast,false);
            sleep(sleeptime);
        }
        depth--;
    }
    
    
    public void setLongtauschActivity(int blong, int r, boolean setActive) {
        while(longlist[r].getnext(blong) != null) {
            longlist[r].mlast.active = setActive;
        }
    }
    
    
    public void verifyResults() {
        completeSanity();
        boolean noColl = true;
        boolean fugPosRight = true;
        boolean fugSumRight = true;
        boolean blocksum = true;
        int[] fcount = new int[P];
        int fugsum = 0;
        for(int r=0; r<R; r++) {
            Fuge f = fugenlist[r].fr;
            int bsum = f.bl;
            while(f.fr != null) {
                fcount[f.pakt]++;
                fugsum++;
                bsum += f.br;
                if(f.pakt < f.fl.pakt || f.pakt > f.fr.pakt) {
                    fugPosRight = false;
                } 
                f = f.fr;
            }
            if(bsum != P-1) {
                blocksum = false;
            }
        }
        for(int i=1; i<P-1;i++) {
            if(fcount[i] > 1) {
                noColl = false;
            }
        }
        if(fugsum != F) {
            fugSumRight = false;
        }
        if(!noColl || !fugPosRight || !fugSumRight || !blocksum) {
            print("Solution Verify Warning");
            print(noColl+" (noCollisions)");
            print(fugPosRight+" (relativeFugPos)");
            print(fugSumRight+" (fugSum)");
            print(blocksum+" (blockSum)");
        }
        else {
            print("Result verify check passed, is a solution.");
        }
    }
    
    
    
    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {}
    }
    
    public String savepath;
    
    public void mauerToFile() {
        try {
            savepath = "saves/n="+n+"_timetag"+file_timetag+".txt";
            PrintWriter mFile = new PrintWriter(savepath);
            mFile.println("n="+n);
            for(int r=0;r<R;r++) {
                String s = "";
                Fuge f = fugenlist[r];
                while(f.fr != null) {
                    s+=f.br+"-";
                    f = f.fr;
                }
                mFile.println(s);
            }
            mFile.flush();
            mFile.close();
        } catch(Exception e) {
            System.out.println("I/O error");
        }       
    }
    
    public void readFileIn() {
        try {
          File myObj = new File("saves/n=300_1213703504720900.txt");
          Scanner myReader = new Scanner(myObj);
          while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            System.out.println(data);
          }
          myReader.close();
        }
        catch (FileNotFoundException e) {
          System.out.println("An error occurred.");
          e.printStackTrace();
        }
    }
    
    public void showMovelists() {
        int showMaxGain = 8;
        for(int i=gainIndexShift+showMaxGain; i>gainIndexShift-showMaxGain; i--) {
            int anzMoves = gainlist.n[i];
            String s = (i-gainIndexShift)+": ";
            for(int j=0; j<anzMoves; j++) {
                s+="x";
            }
            print(s);
        }
    }
    
    public void showMauer() {
        showCollisions();
        for(int r=0;r<R;r++) {
            String s = "";
            Fuge f = fugenlist[r];
            while(f.fr != null) {
                s+=ascii(f.br);
                f = f.fr;
            }
            print(s);
        }
    }
    
    public String ascii(int b) {
        if(b<10) {
            return "-0"+b;
        }
        return "-"+b;
    }
    
    public void countMovesInLists() {
        int n0=0;
        int n1=0;
        int n2=0;
        for(int i=0; i<startlist.len; i++) {
            n0 += startlist.n[i];
            n1 += endlist.n[i];
        }
        for(int i=0; i<gainlist.len; i++) {
            n2 += gainlist.n[i];
        }
        //print("MOVECOUNT: "+n0+"  "+n1+"  "+n2);
    }
    
    public void showCollisions() {
        int collrows = 4;
        for(int i=collrows;i>0;i--) {
            String s="";
            for(int p=1;p<P-1;p++) {
                if(anzF[p]>=i) {
                    s+=anzF[p];
                }
                else {
                    s+=" ";
                }
            }
            print(s);
        }
    }
}
