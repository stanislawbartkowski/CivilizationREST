import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import civilization.II.interfaces.IC;
import civilization.II.interfaces.RAccess;

public class CivRestServices {

    private static final IC II = civilization.II.factory.Factory$.MODULE$.getI();
    private static final RAccess RA = civilization.II.factory.Factory$.MODULE$.getR();

    private static final String REDISHOST = "thinkde";
    private static final int REDISPORT = 6379;

    // if automation engine is ready
    private static boolean automready = false;

    // list if games waiting for automation
    private static final List<String> waitinglist = new ArrayList<String>();


    // logging
    static {
        RA.getConn().setConnection(REDISHOST, REDISPORT, 0);
        II.setR(RA);
    }

    private static String extractAutomatedToken(String s) {
        if (!automready)
            throw new RuntimeException("Cannot run automated player, not registered");
        String a[] = s.split(",");
        // insert at the beginning
        waitinglist.add(a[1]);
        return a[0];
    }

    static class ServiceRegisterAutom extends CivHttpHelper.RestServiceHelper {

        private final static String AUTOM = "autom";

        ServiceRegisterAutom() {
            super("registerautom",CivHttpHelper.PUT);
            addParam(AUTOM,CivHttpHelper.PARAMTYPE.BOOLEAN);
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            if (!verifyURL(httpExchange)) return;
            // do action
            automready = getLogParam(AUTOM);
            CivLogger.info("Automated player registered.");
            // return NODATA, OK here
            produceResponse(httpExchange,"",CivHttpHelper.HTTPNODATA);
        }
    }

    static class ServiceCivData extends CivHttpHelper.RestServiceHelper {

        private final static String WHAT = "what";
        private final static String PARAM = "param";

        private final int LISTOFRES = 0;
        private final int REGISTEROWNER = 1;
        private final int GETBOARD = 2;
        private final int GETGAMES = 3;
        private final int UNREGISTERTOKEN = 4;
        private final int WAITINGGAMES = 5;
        private final int TWOPLAYERSGAME = 6;
        private final int GETJOURNAL = 7;
        private final int TWOPLAYERSGAMEWITHAUTOM = 8;
        private final int SINGLEGAMEWITHAUTOM = 9;

        ServiceCivData() {
            super("civdata",CivHttpHelper.GET);
            addParam(WHAT,CivHttpHelper.PARAMTYPE.INT);
            addParam(PARAM,CivHttpHelper.PARAMTYPE.STRING);
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            if (!verifyURL(httpExchange)) return;
            // do action
            int what = getIntParam(WHAT);
            if (what < 0 || what > 9) {
                produceResponse(httpExchange, "Parameter what " + what + " is expected between 0 and 0", CivHttpHelper.HTTPBADREQUEST);
                return;
            }
            String param = getStringParam(PARAM);
            String res = null;
            CivLogger.info("Get data " + what);
            int w = -1;
            switch (what) {
                case LISTOFRES:
                    w = II.LISTOFRES();
                    break;
                case REGISTEROWNER:
                    w = II.REGISTEROWNER();
                    break;
                case GETBOARD:
                    w = II.GETBOARDGAME();
                    break;
                case GETGAMES:
                    w = II.LISTOFGAMES();
                    break;
                case UNREGISTERTOKEN:
                    w = II.UNREGISTERTOKEN();
                    break;
                case WAITINGGAMES:
                    w = II.LISTOFWAITINGGAMES();
                    break;
                case TWOPLAYERSGAME:
                    w = II.REGISTEROWNERTWOGAME();
                    break;
                case GETJOURNAL:
                    w = II.GETJOURNAL();
                    break;
                case TWOPLAYERSGAMEWITHAUTOM:
                    res =  extractAutomatedToken(httpExchange,automready,waitinglist,II.getData(II.REGISTEROWNERTWOGAME(), param, null));
                    break;

                case SINGLEGAMEWITHAUTOM:
                    // return token and gameid
                    w = II.REGISTEROWNER();
                    break;
            }
            if (res == null) res =  II.getData(w, param, null);
            produceResponse(httpExchange,res,CivHttpHelper.HTTPOK);
        }
    }

}
