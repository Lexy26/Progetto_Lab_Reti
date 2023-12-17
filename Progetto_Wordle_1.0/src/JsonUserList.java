import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Vector;


// Classe Monitor che gestisce il database degli utenti del gioco wordle
public class JsonUserList {

    private Vector<User> userList; // Lista contenente il database degli utenti del gioco wordle
    private String jsonFile; // File 'users.json' contenente tutti gli utenti del gioco

    public JsonUserList(String jsonFile) {
        this.jsonFile = jsonFile;
        userList = new Vector<>();
    }

    // Aggiunge nuovo utente registrato
    public synchronized void insertUser(User user) {
        userList.add(user);
    }

    // Controlla se la lista e' vuota
    public synchronized boolean isEmptyJsonUserList() {
        return userList == null;
    }

    // Controlla se esiste gia' il nome utente
    public synchronized boolean checkUsername(String username) {
        for (User tmpUser : userList)
            if (username.equals(tmpUser.getUsername()))
                return true;
        return false;
    }

    // Controlla se nel sistema esiste il giocatore con 'username' e 'password' dati
    public synchronized boolean checkUserExistance(String username, String password) {
        for (User tmpUser : userList)
            if (username.equals(tmpUser.getUsername()) && password.equals(tmpUser.getPassword()))
                return true;
        return false;
    }

    // Prende dalla lista l'utente richiesto
    public synchronized User getUser(String username) {
        for (User tmpUser : userList)
            if (username.equals(tmpUser.getUsername()))
                return tmpUser;
        return null;
    }

    // Aggiorna la lista con l'utente aggiornato
    public synchronized void updateUser(User user) {
        for (int i = 0; i < userList.size(); i++) {
            User tmpUser = userList.get(i);
            if (user.equals(tmpUser)) {
                userList.set(i, user);
                break;
            }
        }
    }

    // Salva la lista aggiornata del file users.json
    public synchronized void saveUserListInJsonFile() throws IOException {
        FileWriter fileWriter = new FileWriter(jsonFile);
        Gson gson = new Gson();
        String tmpUserList = gson.toJson(userList);
        fileWriter.write(tmpUserList);
        fileWriter.close();

    }

    // Crea la lista a partire dagli utenti presenti sul file users.json (appena viene acceso il server)
    public synchronized void createUserListFromJsonFile() throws IOException {
        FileReader fileReader = new FileReader(jsonFile);
        Gson gson = new Gson();
        Type userListType = new TypeToken<Vector<User>>() {}.getType();
        // Deserializzazione della stringa JSON nella lista degli utenti
        userList = gson.fromJson(fileReader, userListType);
        // Se nel file non c'e' nessun utente allora crea una lista vuota
        if (userList == null) userList = new Vector<>();
        fileReader.close();
    }
}
