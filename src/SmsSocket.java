import java.io.Serializable;
import java.util.ArrayList;

// Classe usata per creare oggetti messaggio (SmsSocket) che possono essere inviati tra client e server
public class SmsSocket implements Serializable {
    private static final long serialVersionUID = 1;

    private Instruction instruction; // Comando da eseguire (da client a server)
    private String username; // Per login e register (da client a server)
    private String password; // Per login e register (da client a server)
    private String word; // Variabile usata dal client per dire al server la "parola tentavivo" della partita (da client a server)

    private ArrayList<String> statusAttemptWordList;//lista status dei char di un tentativo (da server a client)
    private StringBuilder statistics; // Statistiche del giocatore (da server a client)
    private int codice; // Determina l'esito del'operazione o richiesta (da server a client)

    public SmsSocket(Instruction instruction, String username, String password, ArrayList<String> statusAttemptWordList, String word, StringBuilder statistics, int codice) {
        this.instruction = instruction;
        this.codice = codice;
        this.username = username;
        this.password = password;
        this.word = word;
        this.statusAttemptWordList = statusAttemptWordList;
        this.statistics = statistics;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public int getCodice() {
        return codice;
    }

    public String getWord() {
        return word;
    }

    public ArrayList<String> getStatusAttemptWordList() { return statusAttemptWordList; }

    public StringBuilder getStatistics() {
        return statistics;
    }

}
