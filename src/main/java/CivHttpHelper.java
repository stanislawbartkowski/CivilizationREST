import com.rest.restservice.RestParams;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;

import com.rest.restservice.RestHelper;


abstract class CivHttpHelper extends RestHelper.RestServiceHelper {

    private final static String REST = "rest/";
    private final boolean crossAllowed = false;

    protected CivHttpHelper(String url, boolean tokenexpected) {
        super(REST + url, tokenexpected);
    }

    protected CivHttpHelper(String url) {
        super(REST+url, false);
    }

    protected RestParams produceRestParam(String requestMethod, Optional<RestParams.CONTENT> responseContent) {

        List<String> mallowed = new ArrayList<String>();
        mallowed.add(RestHelper.PUT);
        mallowed.add(RestHelper.GET);
        mallowed.add(RestHelper.DELETE);
        mallowed.add(RestHelper.POST);

        RestParams res = new RestParams(requestMethod,responseContent,crossAllowed,mallowed);
        return res;
    }


    protected String extractAutomatedToken(RestHelper.IQueryInterface v, boolean automready, List<String> waitinglist, String s) throws IOException {
        if (!automready) {
            produceResponse(v, Optional.of("Cannot run automated player, not registered"), RestHelper.HTTPBADREQUEST);
            return null;
        }
        String a[] = s.split(",");
        // insert at the beginning
        waitinglist.add(a[1]);
        return a[0];
    }
}
