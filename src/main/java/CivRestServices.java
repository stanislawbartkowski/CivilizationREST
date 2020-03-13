/*
 * Copyright 2020 stanislawbartkowski@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.rest.restservice.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;

import civilization.II.interfaces.IC;
import civilization.II.interfaces.RAccess;

import com.sun.net.httpserver.HttpServer;

class CivRestServices {

    private final IC II = civilization.II.factory.Factory$.MODULE$.getI();
    private final RAccess RA = civilization.II.factory.Factory$.MODULE$.getR();

    // if automation engine is ready
    private boolean automready = false;

    // list if games waiting for automation
    private final List<String> waitinglist = new ArrayList<String>();

    // common parameters name
    private final static String CIV = "civ";
    private final static String GAMEID = "gameid";

    // logging
    void setRedis(String REDISHOST, int REDISPORT) {
        CivLogger.info("Redis host:" + REDISHOST + " port:" + REDISPORT);
        RA.getConn().setConnection(REDISHOST, REDISPORT, 0);
        II.setR(RA);
    }

    private String extractAutomatedToken(String s) {
        if (!automready)
            throw new RuntimeException("Cannot run automated player, not registered");
        String a[] = s.split(",");
        // insert at the beginning
        waitinglist.add(a[1]);
        return a[0];
    }

    class ServiceRegisterAutom extends CivHttpHelper {

        private final static String AUTOM = "autom";

        ServiceRegisterAutom() {
            super("registerautom");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.PUT, Optional.empty());
            par.addParam(AUTOM, PARAMTYPE.BOOLEAN);
            return par;
        }

        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            // do action
            automready = getLogParam(v, AUTOM);
            CivLogger.info("Automated player registered.");
            // return NODATA, OK here
            produceNODATAResponse(v);
        }
    }

    class ServiceCivData extends CivHttpHelper {

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

        private final Set<Integer> paramexpected = new HashSet<Integer>();
        private final Set<Integer> tokenexpected = new HashSet<Integer>();


        private void init() {
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
            super("civdata");
            init();
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.GET, Optional.of(RestParams.CONTENT.JSON));
            par.addParam(WHAT, PARAMTYPE.INT);
            par.addParam(PARAM, PARAMTYPE.STRING, new ParamValue(""));
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            // do action
            int what = getIntParam(v, WHAT);
            if (what < LISTOFRES || what > LASTWHAT) {
                produceResponse(v, Optional.of("Parameter what " + what + " is expected between " + LISTOFRES + " and " + LASTWHAT), RestHelper.HTTPBADREQUEST);
                return;
            }
            String param = null;
            if (tokenexpected.contains(what)) {
                Optional<String> token = getAuthorizationToken(v, true);
                if (!token.isPresent()) {
                    RestLogger.L.severe("Token not found in the header.");
                    return;
                }
                param = token.get();
                RestLogger.info(param);
            }
            if (paramexpected.contains(what)) {
                Optional<String> s = getStringParamExpected(v, PARAM);
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
                    res = extractAutomatedToken(v, automready, waitinglist, II.getData(II.REGISTEROWNERTWOGAME(), param, null));
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
            produceOKResponse(v, res);
        }
    }

    class ServiceJoinGame extends CivHttpHelper {

        ServiceJoinGame() {
            super("joingame");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.POST, Optional.of(RestParams.CONTENT.TEXT));
            par.addParam(GAMEID, PARAMTYPE.INT);
            par.addParam(CIV, PARAMTYPE.STRING);
            return par;
        }

        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            int gameid = getIntParam(v, GAMEID);
            String civ = getStringParam(v, CIV);
            String token = II.joinGame(gameid, civ);
            produceOKResponse(v, Optional.of(token));
        }
    }

    class GetWaitingGame extends CivHttpHelper {

        GetWaitingGame() {
            super("getwaiting");
        }

        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String gameid = "";
            if (!waitinglist.isEmpty()) {
                gameid = waitinglist.get(0);
                waitinglist.remove(0);
            }
            produceOKResponse(v, Optional.of(gameid));
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.GET, Optional.of(RestParams.CONTENT.JSON));
            return par;
        }


    }

    class ServiceItemizeCommand extends CivHttpHelper {

        private final static String COMMAND = "command";

        ServiceItemizeCommand() {
            super("itemize", true);
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.GET, Optional.of(RestParams.CONTENT.JSON));
            par.addParam(COMMAND, PARAMTYPE.STRING);
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String token = getAuthorizationToken(v).get();
            String command = getStringParam(v, COMMAND);
            String res = II.itemizeCommand(token, command);
            produceOKResponse(v, Optional.of(res));
        }
    }

    class ServiceExecuteCommand extends CivHttpHelper {

        private final static String ACTION = "action";
        private final static String ROW = "row";
        private final static String COL = "col";
        private final static String JSPARAM = "jsparam";

        ServiceExecuteCommand() {
            super("command", true);
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.POST, Optional.of(RestParams.CONTENT.TEXT));
            par.addParam(ACTION, PARAMTYPE.STRING);
            par.addParam(ROW, PARAMTYPE.INT);
            par.addParam(COL, PARAMTYPE.INT);
            par.addParam(JSPARAM, PARAMTYPE.STRING, new ParamValue(null));
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String token = getAuthorizationToken(v).get();
            String action = getStringParam(v, ACTION);
            int row = getIntParam(v, ROW);
            int col = getIntParam(v, COL);
            String jsparam = getStringParam(v, JSPARAM);
            String res = II.executeCommand(token, action, row, col, jsparam);
            produceOKResponse(v, res);
        }

    }

    class ServiceDeleteGame extends CivHttpHelper {

        private final static String GAMEID = "gameid";

        ServiceDeleteGame() {
            super("delete");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.DELETE, Optional.empty());
            par.addParam(GAMEID, PARAMTYPE.INT);
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            int gameid = getIntParam(v, GAMEID);
            II.deleteGame(gameid);
            produceNODATAResponse(v);
        }
    }

    class ServiceClearWaiting extends CivHttpHelper {

        ServiceClearWaiting() {
            super("clearwaitinglist");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.POST, Optional.empty());
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            waitinglist.clear();
            produceNODATAResponse(v);
        }
    }

    class ServiceAllReady extends CivHttpHelper {

        ServiceAllReady() {
            super("allready", true);
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.GET, Optional.of(RestParams.CONTENT.TEXT));
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String token = getAuthorizationToken(v).get();
            boolean res = II.allPlayersReady(token);
            produceOKResponse(v, Optional.of(Boolean.toString(res)));
        }
    }

    class ServiceDeployGame extends CivHttpHelper {

        ServiceDeployGame() {
            super("deploygame");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.POST, Optional.of(RestParams.CONTENT.JSON));
            par.addParam(CIV, PARAMTYPE.STRING);
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String civs = getStringParam(v, CIV);
            InputStream is = v.getT().getRequestBody();
            String board = RestHelper.toS(is);
            String res = II.readPlayerGameS(board, civs);
            produceOKResponse(v, res);
        }
    }

    class ResumeGame extends CivHttpHelper {

        ResumeGame() {
            super("resumegame");
        }

        @Override
        public RestParams getParams(HttpExchange httpExchange) throws IOException {
            RestParams par = produceRestParam(RestHelper.GET, Optional.of(RestParams.CONTENT.TEXT));
            par.addParam(GAMEID, PARAMTYPE.INT);
            par.addParam(CIV, PARAMTYPE.STRING);
            return par;
        }


        @Override
        public void servicehandle(RestHelper.IQueryInterface v) throws IOException {
            String civs = getStringParam(v, CIV);
            int gameid = getIntParam(v, GAMEID);
            String res = II.resumeGame(gameid, civs);
            produceOKResponse(v, res);
        }
    }

    void registerServices(HttpServer server) {
        RestHelper.registerService(server, new CivRestServices.ServiceRegisterAutom());
        RestHelper.registerService(server, new CivRestServices.ServiceCivData());
        RestHelper.registerService(server, new CivRestServices.GetWaitingGame());
        RestHelper.registerService(server, new CivRestServices.ServiceJoinGame());
        RestHelper.registerService(server, new CivRestServices.ServiceItemizeCommand());
        RestHelper.registerService(server, new CivRestServices.ServiceExecuteCommand());
        RestHelper.registerService(server, new CivRestServices.ServiceDeleteGame());
        RestHelper.registerService(server, new CivRestServices.ServiceClearWaiting());
        RestHelper.registerService(server, new CivRestServices.ServiceAllReady());
        RestHelper.registerService(server, new CivRestServices.ServiceDeployGame());
        RestHelper.registerService(server, new CivRestServices.ResumeGame());
    }

}
