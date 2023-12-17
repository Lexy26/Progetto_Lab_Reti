import java.util.*;

// Classe per tenere traccia delle informazioni legate ad un Utente
class User {
    private String username;
    private String password;

    private Statistics statistics; // Statistiche di gioco
    private Vector<String> wordPlayedList; // Lista di parole giocate da quando si e' registrato

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.statistics = new Statistics();
        this.wordPlayedList = new Vector<>();
    }

    public Statistics getStatistics() {
        return statistics;
    }

    // Aggiunge "parola giocata" alla lista delle parole giocate dall'utente nel gioco Wordle
    public void addToWordPlayedList(String word) {
        this.wordPlayedList.add(word);
    }

    // Verifica se una parola è già stata giocata dall'utente in Wordle
    public boolean isWordAlreadyPlayed(String word) {
        if (word == null) return false;
        else return wordPlayedList.contains(word);
    }

    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }
    
}

