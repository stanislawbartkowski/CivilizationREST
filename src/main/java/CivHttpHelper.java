import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;

import com.rest.restservice.RestHelper;


abstract class CivHttpHelper extends RestHelper.RestServiceHelper {

    protected CivHttpHelper(String url, String expectedMethod, boolean tokenexpected) {
        super(url, expectedMethod,tokenexpected);
    }

    protected CivHttpHelper(String url, String expectedMethod) {
        super(url, expectedMethod,false);
    }


    protected String extractAutomatedToken(HttpExchange t, boolean automready, List<String> waitinglist, String s) throws IOException {
        if (!automready) {
            produceResponse(t, Optional.of("Cannot run automated player, not registered"), RestHelper.HTTPBADREQUEST);
            return null;
        }
        String a[] = s.split(",");
        // insert at the beginning
        waitinglist.add(a[1]);
        return a[0];
    }
}
