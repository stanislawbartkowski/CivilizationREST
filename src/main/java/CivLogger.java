import java.util.logging.Level;
import java.util.logging.Logger;

public class CivLogger {
    static Logger L = Logger.getLogger("com.civilization.ui.server");

    public static void info(String s) {
        L.info(s);
    }

    public static void debug(String s) {
//        L.log(Level.FINE,s);
        info(s);
    }
}
