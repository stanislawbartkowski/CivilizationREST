import com.sun.net.httpserver.HttpServer;

import com.rest.restservice.RestStart;
import com.rest.restservice.RestHelper;

/*
 * a simple static http server
 */
public class CivHttpServer extends RestStart {

    private static void P(String s) {
        System.out.println(s);
    }

    private final static String CORS="cors";

    private static void printhelp() {
        P("Usage: java ..  CivHttpServer /port/ /redishost/ /redisport/ /cross/");
        P("  /port/ : port number CivRestServer is listening");
        P("  /redishost/ : Redis host name");
        P("  /redispost/ : Redis port");
        P("  /cross/ : CORS policy allowed");
        P("            should be value " + CORS);
    }

    static void registerServices(HttpServer server) {
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
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) {
            printhelp();
            System.exit(4);
        }
        CivRestServices.setRedis(args[1], Integer.parseInt(args[2]));
        int PORT = Integer.parseInt(args[0]);
        if (args.length == 4 && !CORS.equals(args[3])) {
            printhelp();
            System.exit(4);
        }
        if (args.length == 4) CivHttpHelper.setCrossAllowed(true);
        RestStart(PORT, CivHttpServer::registerServices);
    }

}