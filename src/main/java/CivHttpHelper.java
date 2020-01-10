import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

class CivHttpHelper {

    private static final String RESTPREFIX = "/rest/";

    static final String POST = "POST";
    static final String GET = "GET";
    static final String PUT = "PUT";
    static final String DELETE = "DELETE";

    static final int HTTPOK = 200;
    static final int HTTPNODATA = 204;
    static final int HTTPMETHODNOTALLOWED = 405;
    static final int HTTPBADREQUEST = 400;


    enum PARAMTYPE {
        BOOLEAN, INT, STRING
    }

    static class RestParam {
        final PARAMTYPE ptype;
        final boolean obligatory;

        RestParam(PARAMTYPE ptype, boolean obligatory) {
            this.ptype = ptype;
            this.obligatory = obligatory;
        }
    }

    static class ParamValue {
        final boolean logvalue;
        final int intvalue;
        final String stringvalue;

        ParamValue(boolean logvalue) {
            this.logvalue = logvalue;
            this.intvalue = -1;
            this.stringvalue = null;
        }

        ParamValue(int intvalue) {
            this.intvalue = intvalue;
            this.logvalue = false;
            this.stringvalue = null;
        }

        ParamValue(String stringvalue) {
            this.intvalue = -1;
            this.logvalue = false;
            this.stringvalue = stringvalue;
        }

    }

    abstract static class RestServiceHelper implements HttpHandler {
        final String url;
        final String expectedMethod;
        final Map<String, RestParam> params = new HashMap<String, RestParam>();
        int successResponse;
        final Map<String, ParamValue> values = new HashMap<String, ParamValue>();

        RestServiceHelper(String url, String expectedMethod) {
            this.url = url;
            this.expectedMethod = expectedMethod;
        }

        protected void addParam(String paramName, PARAMTYPE ptype) {
            params.put(paramName, new RestParam(ptype, true));
        }

        protected void produceResponse(HttpExchange t, String message, int HTTPResponse) throws IOException {
            byte[] response = message.getBytes();
            t.sendResponseHeaders(HTTPResponse, response.length);
            try (OutputStream os = t.getResponseBody()) {
                os.write(response);
            }
        }

        private boolean verifyMethod(HttpExchange t) throws IOException {

            if (expectedMethod.equals(t.getRequestMethod())) return true;
            String message = expectedMethod + " method expected, " + t.getRequestMethod() + " provided.";
            produceResponse(t, message, HTTPMETHODNOTALLOWED);
            return false;
        }

        protected boolean verifyURL(HttpExchange t) throws IOException {
            CivLogger.debug(t.getRequestMethod() + " " + t.getRequestURI().getQuery());
            if (!verifyMethod(t)) return false;
            // verify param
            // check if parameters allowed
            String[] q = t.getRequestURI().getQuery().split("&");
            for (String qline : q) {
                String[] vv = qline.split("=");
                String s = vv[0];
                String val = vv.length == 1 ? "" : vv[1];
                if (!params.containsKey(s)) {
                    produceResponse(t, "Parameter " + s + " not expected.", HTTPBADREQUEST);
                    return false;
                }
                // get value
                RestParam rpara = params.get(s);
                switch (rpara.ptype) {
                    case BOOLEAN: {
                        if (val.equals("true") || val.equals("false")) {
                            values.put(s, new ParamValue(val.equals("true")));
                            break;
                        }
                        // incorrect true or false
                        produceResponse(t, "Parameter " + s + "?" + val + " true or false expected", HTTPBADREQUEST);
                        return false;
                    }
                    case INT: {
                        try {
                            int i = Integer.parseInt(val);
                            values.put(s, new ParamValue(i));
                            break;
                        } catch (NumberFormatException e) {
                            produceResponse(t, "Parameter " + s + "?" + val + " incorrect integer value", HTTPBADREQUEST);
                            return false;
                        }
                    }
                    case STRING: {
                        values.put(s, new ParamValue(val));
                        break;
                    }
                }
            } // for
            // verify obligatory params
            for (String s : params.keySet()) {
                if (params.get(s).obligatory && !values.containsKey(s)) {
                    produceResponse(t, "Parameter " + s + " not found in url", HTTPBADREQUEST);
                    return false;
                }
            }

            return true;
        }

        protected String extractAutomatedToken(HttpExchange t, boolean automready, List<String> waitinglist, String s) throws IOException {
            if (!automready) {
                produceResponse(t, "Cannot run automated player, not registered", HTTPBADREQUEST);
                return null;
            }
            String a[] = s.split(",");
            // insert at the beginning
            waitinglist.add(a[1]);
            return a[0];
        }


        boolean getLogParam(String param) {
            return values.get(param).logvalue;
        }

        int getIntParam(String param) {
            return values.get(param).intvalue;
        }

        String getStringParam(String param) {
            return values.get(param).stringvalue;
        }

    }

    static void registerService(HttpServer server, RestServiceHelper service) {
        CivLogger.info("Register service: " + service.url);
        server.createContext(RESTPREFIX + service.url, service);
    }

}
