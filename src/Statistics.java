import java.util.ArrayList;

// Classe che tiene traccia delle statistiche di un giocatore
public class Statistics {

    private int numPartiteGiocate;
    private int numPartiteVinte;
    private int currentStreak; //Num vittorie consecutive del momento
    private int maxStreak; // Max raggiunto di num vittorie consecutive
    private ArrayList<Integer> guessDistribution; // Lista di tentativi impiegati per arrivare alla soluzione della partita vinta

    public Statistics() {
        this.numPartiteGiocate = 0;
        this.numPartiteVinte = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.guessDistribution = new ArrayList<>();
    }

    public int getNumPartiteGiocate() {
        return numPartiteGiocate;
    }

    public void incNumPartiteGiocate() {
        this.numPartiteGiocate++;
    }

    public int getNumPartiteVinte() {
        return numPartiteVinte;
    }

    public void incNumPartiteVinte() {
        this.numPartiteVinte++;
    }

    // Percentale di partite vinte
    public int getPercentVinte() {
        if (numPartiteGiocate == 0) // Per evitare la divisione per zero
            return 0;
        return (numPartiteVinte * 100) / numPartiteGiocate;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    // Aggiorna le variabili currentStreak e maxStreak a seconda di come e' andata la partita
    public void updateStreak(boolean won) {
        if (won) {
            currentStreak++;
            if (currentStreak > maxStreak)
                maxStreak = currentStreak;
        } else
            currentStreak = 0;
    }

    public int getMaxStreak() { return maxStreak;    }

    public ArrayList<Integer> getGuessDistribution() {
        return guessDistribution;
    }

    public void addGuessDistribution(Integer element) {
        this.guessDistribution.add(element);
    }
}
