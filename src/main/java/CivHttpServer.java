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

import com.rest.restservice.RestStart;

/*
 * a simple static http server
 */
public class CivHttpServer extends RestStart {

    private static void P(String s) {
        System.out.println(s);
    }

    private final static String CORS = "cors";

    private static void printhelp() {
        P("Usage: java ..  CivHttpServer /port/ /redishost/ /redisport/ /cross/");
        P("  /port/ : port number CivRestServer is listening");
        P("  /redishost/ : Redis host name");
        P("  /redispost/ : Redis port");
        P("  /cross/ : CORS policy allowed");
        P("            should be value " + CORS);
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) {
            printhelp();
            System.exit(4);
        }
        CivRestServices serv = new CivRestServices();
        serv.setRedis(args[1], Integer.parseInt(args[2]));
        int PORT = Integer.parseInt(args[0]);
        if (args.length == 4 && !CORS.equals(args[3])) {
            printhelp();
            System.exit(4);
        }
        if (args.length == 4) CivHttpHelper.setCrossAllowed(true);
        RestStart(PORT, server -> serv.registerServices(server), new String[]{});
    }

}