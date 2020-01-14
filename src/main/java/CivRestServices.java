import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

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

    private static String toS(InputStream i) {
        String result;
        try {
            result = CharStreams.toString(new InputStreamReader(i, Charsets.UTF_8));
            return result;
        } catch (IOException e) {
            CivLogger.L.log(Level.SEVERE, "Error while reading the game boardd", e);
            return null;
        }
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
            super("registerautom", CivHttpHelper.PUT);
            addParam(AUTOM, CivHttpHelper.PARAMTYPE.BOOLEAN);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            // do action
            automready = getLogParam(AUTOM);
            CivLogger.info("Automated player registered.");
            // return NODATA, OK here
            produceResponse(httpExchange, "", CivHttpHelper.HTTPNODATA);
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
            super("civdata", CivHttpHelper.GET);
            addParam(WHAT, CivHttpHelper.PARAMTYPE.INT);
            addParam(PARAM, CivHttpHelper.PARAMTYPE.STRING, new CivHttpHelper.ParamValue(""));
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
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
                    res = extractAutomatedToken(httpExchange, automready, waitinglist, II.getData(II.REGISTEROWNERTWOGAME(), param, null));
                    break;

                case SINGLEGAMEWITHAUTOM:
                    // return token and gameid
                    w = II.REGISTEROWNER();
                    break;
            }
            try {
                if (res == null) res = II.getData(w, param, null);
            } catch (Exception e) {
                int a = 0;
            }
            produceOKResponse(httpExchange, res);
        }
    }

    static class ServiceJoinGame extends CivHttpHelper.RestServiceHelper {

        private final static String GAMEID = "gameid";
        private final static String CIV = "civ";

        ServiceJoinGame() {
            super("joingame", CivHttpHelper.POST);
            addParam(GAMEID, CivHttpHelper.PARAMTYPE.INT);
            addParam(CIV, CivHttpHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            int gameid = getIntParam(GAMEID);
            String civ = getStringParam(CIV);
            String token = II.joinGame(gameid, civ);
            produceOKResponse(httpExchange, token);
        }
    }

    static class GetWaitingGame extends CivHttpHelper.RestServiceHelper {

        GetWaitingGame() {
            super("getwaiting", CivHttpHelper.GET);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            String gameid = "";
            if (!waitinglist.isEmpty()) {
                gameid = waitinglist.get(0);
                waitinglist.remove(0);
            }
            produceOKResponse(httpExchange, gameid);
        }

    }

    static class ServiceItemizeCommand extends CivHttpHelper.RestServiceHelper {

        private final static String TOKEN = "token";
        private final static String COMMAND = "command";

        ServiceItemizeCommand() {
            super("itemize", CivHttpHelper.GET);
            addParam(TOKEN, CivHttpHelper.PARAMTYPE.STRING);
            addParam(COMMAND, CivHttpHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            String token = getStringParam(TOKEN);
            String command = getStringParam(COMMAND);
            String res = II.itemizeCommand(token, command);
            produceOKResponse(httpExchange, res);
        }
    }

    static class ServiceExecuteCommand extends CivHttpHelper.RestServiceHelper {

        private final static String TOKEN = "token";
        private final static String ACTION = "action";
        private final static String ROW = "row";
        private final static String COL = "col";
        private final static String JSPARAM = "jsparam";

        ServiceExecuteCommand() {
            super("command", CivHttpHelper.POST);
            addParam(TOKEN, CivHttpHelper.PARAMTYPE.STRING);
            addParam(ACTION, CivHttpHelper.PARAMTYPE.STRING);
            addParam(ROW, CivHttpHelper.PARAMTYPE.INT);
            addParam(COL, CivHttpHelper.PARAMTYPE.INT);
            addParam(JSPARAM, CivHttpHelper.PARAMTYPE.STRING, new CivHttpHelper.ParamValue(null));
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            String token = getStringParam(TOKEN);
            String action = getStringParam(ACTION);
            int row = getIntParam(ROW);
            int col = getIntParam(COL);
            String jsparam = getStringParam(JSPARAM);
            String res = II.executeCommand(token, action, row, col, jsparam);
            produceOKResponse(httpExchange, res);
        }

    }

    static class ServiceDeleteGame extends CivHttpHelper.RestServiceHelper {

        private final static String GAMEID = "gameid";

        ServiceDeleteGame() {
            super("delete", CivHttpHelper.DELETE);
            addParam(GAMEID, CivHttpHelper.PARAMTYPE.INT);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            int gameid = getIntParam(GAMEID);
            II.deleteGame(gameid);
            produceNODATAResponse(httpExchange);
        }
    }

    static class ServiceClearWaiting extends CivHttpHelper.RestServiceHelper {


        ServiceClearWaiting() {
            super("clearwaitinglist", CivHttpHelper.POST);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            waitinglist.clear();
            produceNODATAResponse(httpExchange);
        }
    }

    static class ServiceAllReady extends CivHttpHelper.RestServiceHelper {

        private final static String TOKEN = "token";

        ServiceAllReady() {
            super("allready", CivHttpHelper.GET);
            addParam(TOKEN, CivHttpHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            String token = getStringParam(TOKEN);
            boolean res = II.allPlayersReady(token);
            produceOKResponse(httpExchange, Boolean.toString(res));
        }
    }

    static class ServiceDeployGame extends CivHttpHelper.RestServiceHelper {

        private final static String CIV = "civ";

        ServiceDeployGame() {
            super("deploygame", CivHttpHelper.POST);
            addParam(CIV, CivHttpHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange) throws IOException {
            String civs = getStringParam(CIV);
            InputStream is = httpExchange.getRequestBody();
            String board = toS(is);
            String res = II.readPlayerGameS(board, civs);
            produceOKResponse(httpExchange, res);
        }
    }

}

