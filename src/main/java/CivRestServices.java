import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;

import civilization.II.interfaces.IC;
import civilization.II.interfaces.RAccess;

import com.rest.restservice.RestHelper;


public class CivRestServices {

    private static final IC II = civilization.II.factory.Factory$.MODULE$.getI();
    private static final RAccess RA = civilization.II.factory.Factory$.MODULE$.getR();

    // if automation engine is ready
    private static boolean automready = false;

    // list if games waiting for automation
    private static final List<String> waitinglist = new ArrayList<String>();


    // logging
    static void setRedis(String REDISHOST, int REDISPORT) {
        CivLogger.info("Redis host:" + REDISHOST + " port:" + REDISPORT);
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

    static class ServiceRegisterAutom extends CivHttpHelper {

        private final static String AUTOM = "autom";

        ServiceRegisterAutom() {
            super("registerautom", RestHelper.PUT);
            addParam(AUTOM, RestHelper.PARAMTYPE.BOOLEAN);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            // do action
            automready = getLogParam(v, AUTOM);
            CivLogger.info("Automated player registered.");
            // return NODATA, OK here
            produceNODATAResponse(httpExchange);
        }
    }

    static class ServiceCivData extends CivHttpHelper {

        private final static String WHAT = "what";
        private final static String PARAM = "param";

        private final static int LISTOFRES = 0;
        private final static int REGISTEROWNER = 1;
        private final static int GETBOARD = 2;
        private final static int GETGAMES = 3;
        private final static int UNREGISTERTOKEN = 4;
        private final static int WAITINGGAMES = 5;
        private final static int TWOPLAYERSGAME = 6;
        private final static int GETJOURNAL = 7;
        private final static int TWOPLAYERSGAMEWITHAUTOM = 8;
        private final static int SINGLEGAMEWITHAUTOM = 9;

        private final static Set<Integer> paramexpected = new HashSet<Integer>();
        private final static Set<Integer> tokenexpected = new HashSet<Integer>();


        static {
            paramexpected.add(REGISTEROWNER);
            paramexpected.add(TWOPLAYERSGAME);
            paramexpected.add(TWOPLAYERSGAMEWITHAUTOM);
            paramexpected.add(SINGLEGAMEWITHAUTOM);

            tokenexpected.add(GETBOARD);
            tokenexpected.add(UNREGISTERTOKEN);
            tokenexpected.add(GETJOURNAL);
        }


        private final int LASTWHAT = 9;

        ServiceCivData() {
            super("civdata", RestHelper.GET);
            addParam(WHAT, RestHelper.PARAMTYPE.INT);
            addParam(PARAM, RestHelper.PARAMTYPE.STRING, new RestHelper.ParamValue(""));
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            // do action
            int what = getIntParam(v, WHAT);
            if (what < LISTOFRES || what > LASTWHAT) {
                produceResponse(httpExchange, Optional.of("Parameter what " + what + " is expected between " + LISTOFRES + " and " + LASTWHAT), RestHelper.HTTPBADREQUEST);
                return;
            }
            String param = null;
            if (tokenexpected.contains(what)) {
                Optional<String> token = getAuthorizationToken(httpExchange, true);
                if (!token.isPresent()) return;
                param = token.get();
            }
            if (paramexpected.contains(what)) {
                Optional<String> s = getStringParamExpected(httpExchange, v, PARAM);
                if (!s.isPresent()) return;
                param = s.get();
            }
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
                CivLogger.error("Error thrown from Civilization Engine", e);
            }
            produceOKResponse(httpExchange, res);
        }
    }

    static class ServiceJoinGame extends CivHttpHelper {

        private final static String GAMEID = "gameid";
        private final static String CIV = "civ";

        ServiceJoinGame() {
            super("joingame", RestHelper.POST);
            addParam(GAMEID, RestHelper.PARAMTYPE.INT);
            addParam(CIV, RestHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            int gameid = getIntParam(v, GAMEID);
            String civ = getStringParam(v, CIV);
            String token = II.joinGame(gameid, civ);
            produceOKResponse(httpExchange, Optional.of(token));
        }
    }

    static class GetWaitingGame extends CivHttpHelper {

        GetWaitingGame() {
            super("getwaiting", RestHelper.GET);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            String gameid = "";
            if (!waitinglist.isEmpty()) {
                gameid = waitinglist.get(0);
                waitinglist.remove(0);
            }
            produceOKResponse(httpExchange, Optional.of(gameid));
        }

    }

    static class ServiceItemizeCommand extends CivHttpHelper {

        private final static String COMMAND = "command";

        ServiceItemizeCommand() {
            super("itemize", RestHelper.GET, true);
            addParam(COMMAND, RestHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            String token = getAuthorizationToken(httpExchange).get();
            String command = getStringParam(v, COMMAND);
            String res = II.itemizeCommand(token, command);
            produceOKResponse(httpExchange, Optional.of(res));
        }
    }

    static class ServiceExecuteCommand extends CivHttpHelper {

        private final static String ACTION = "action";
        private final static String ROW = "row";
        private final static String COL = "col";
        private final static String JSPARAM = "jsparam";

        ServiceExecuteCommand() {
            super("command", RestHelper.POST, true);
            addParam(ACTION, RestHelper.PARAMTYPE.STRING);
            addParam(ROW, RestHelper.PARAMTYPE.INT);
            addParam(COL, RestHelper.PARAMTYPE.INT);
            addParam(JSPARAM, RestHelper.PARAMTYPE.STRING, new RestHelper.ParamValue(null));
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            String token = getAuthorizationToken(httpExchange).get();
            String action = getStringParam(v, ACTION);
            int row = getIntParam(v, ROW);
            int col = getIntParam(v, COL);
            String jsparam = getStringParam(v, JSPARAM);
            String res = II.executeCommand(token, action, row, col, jsparam);
            produceOKResponse(httpExchange, res);
        }

    }

    static class ServiceDeleteGame extends CivHttpHelper {

        private final static String GAMEID = "gameid";

        ServiceDeleteGame() {
            super("delete", RestHelper.DELETE);
            addParam(GAMEID, RestHelper.PARAMTYPE.INT);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            int gameid = getIntParam(v, GAMEID);
            II.deleteGame(gameid);
            produceNODATAResponse(httpExchange);
        }
    }

    static class ServiceClearWaiting extends CivHttpHelper {


        ServiceClearWaiting() {
            super("clearwaitinglist", RestHelper.POST);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            waitinglist.clear();
            produceNODATAResponse(httpExchange);
        }
    }

    static class ServiceAllReady extends CivHttpHelper {

        ServiceAllReady() {
            super("allready", RestHelper.GET, true);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            String token = getAuthorizationToken(httpExchange).get();
            boolean res = II.allPlayersReady(token);
            produceOKResponse(httpExchange, Optional.of(Boolean.toString(res)));
        }
    }

    static class ServiceDeployGame extends CivHttpHelper {

        private final static String CIV = "civ";

        ServiceDeployGame() {
            super("deploygame", RestHelper.POST);
            addParam(CIV, RestHelper.PARAMTYPE.STRING);
        }

        @Override
        public void servicehandle(HttpExchange httpExchange, RestHelper.IQeuryInterface v) throws IOException {
            String civs = getStringParam(v, CIV);
            InputStream is = httpExchange.getRequestBody();
            String board = toS(is);
            String res = II.readPlayerGameS(board, civs);
            produceOKResponse(httpExchange, res);
        }
    }

}

