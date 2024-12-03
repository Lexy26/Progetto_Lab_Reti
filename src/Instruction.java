import java.io.Serializable;

// Tag per identificare le richieste fatte al server
public enum Instruction implements Serializable {
    LOGIN,
    REGISTER,
    PLAY,
    LOGOUT,
    LOGOUT_PLAY,
    SHARE,
    SEND_ME_STAT,
    SEND_WORD
}
