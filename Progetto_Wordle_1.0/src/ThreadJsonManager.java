import java.io.IOException;

// Per ogni tot tempo (delayJsonUpdate) viene aggiornato il file "users.json" con utenti registrati
public class ThreadJsonManager implements Runnable{

    private JsonUserList jsonUserList; // Lista degli utenti registrati al gioco Wordle

    public ThreadJsonManager(JsonUserList jsonUserList) {
        this.jsonUserList = jsonUserList;
    }

    public void run() {
        try {
            jsonUserList.saveUserListInJsonFile();
            System.out.println("Update users.json Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
