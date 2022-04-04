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

import com.rest.restservice.RestLogger;
import com.rest.restservice.RestStart;
import org.apache.commons.cli.*;
import java.util.logging.Level;


/*
 * a simple static http server
 */
public class CivHttpServer extends RestStart {

    private static void P(String s) {
        CivLogger.info(s);
    }

    private static final String PORT="p";
    private static final String REDISPORT="rp";
    private static final String REDISHOST = "rh";
    private static final String NOCORS = "nocors";

    public static class RestParams {

        private final CommandLine cmd;

        RestParams(CommandLine cmd) {
            this.cmd = cmd;
        }

        public int getPORT() {
            return Integer.parseInt(cmd.getOptionValue(PORT));
        }

        public int getREDISPORT() {
            return Integer.parseInt(cmd.getOptionValue(REDISPORT));
        }

        public String getREDISHOST() {
            return cmd.getOptionValue(REDISHOST);
        }

        public boolean isNOCORS() {
            return cmd.hasOption(NOCORS);
        }

    }

    private static RestParams buildCmd(String[] args) {

        final Options options = new Options();
        Option port = Option.builder(PORT).desc("Port number").numberOfArgs(1).type(Integer.class).numberOfArgs(1).required().build();
        Option redhost = Option.builder(REDISHOST).desc("Redis hostname").numberOfArgs(1).required().build();
        Option redport = Option.builder(REDISPORT).desc("Redis port number").numberOfArgs(1).type(Integer.class).required().build();
        Option cors = Option.builder(NOCORS).desc("CORS not allowed (default allowed)").build();
        options.addOption(port).addOption(redhost).addOption(redport).addOption(cors);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            return new RestParams(cmd);
        } catch (ParseException e) {
            P(CivRestServices.CIVVERSION);
            RestLogger.L.log(Level.SEVERE,e.getMessage(),e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java /parameters/", options);
            System.exit(4);
            return null;
        }
    }


    public static void main(String[] args) throws Exception {
        P(CivRestServices.CIVVERSION);
        P(CivRestServices.RESTVERSION);
        RestParams res = buildCmd(args);
        P(String.format("Port:%d",res.getPORT()));
        P(String.format("Redis host: %s",res.getREDISHOST()));
        P(String.format("Redis port: %d",res.getREDISPORT()));
        P(res.isNOCORS() ? "CORS not allowed" : "CORS allowed");
        if (! res.isNOCORS()) CivHttpHelper.setCrossAllowed(true);
        CivRestServices serv = new CivRestServices();
        serv.setRedis(res.getREDISHOST(), res.getREDISPORT());
        RestStart(res.getPORT(), server -> serv.registerServices(server), new String[]{});
    }

}
