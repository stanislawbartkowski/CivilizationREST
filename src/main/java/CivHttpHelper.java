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

import com.rest.restservice.RestParams;

import java.io.IOException;
import java.util.*;

import com.rest.restservice.RestHelper;


abstract class CivHttpHelper extends RestHelper.RestServiceHelper {

    private final static String REST = "rest/";

    private static boolean crossAllowed = false;

    static void setCrossAllowed(boolean crossAllowed) {
        CivHttpHelper.crossAllowed = crossAllowed;
    }

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

        RestParams res = new RestParams(requestMethod,responseContent,crossAllowed,mallowed, Optional.empty());
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
