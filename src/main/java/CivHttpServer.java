import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/*
 * a simple static http server
 */
public class CivHttpServer {

    private static void P(String s) {
        System.out.println(s);
    }

    private static void printhelp() {
        P("Usage: java ..  CivHttpServer /port/ /redishost/ /redisport/");
        P("  /port/ : port number CivRestServer is listening");
        P("  /redishost/ : Redis host name");
        P("  /redispost/ : Redis port");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            printhelp();
            System.exit(4);
        }
        CivRestServices.setRedis(args[1], Integer.parseInt(args[2]));
        int PORT = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        CivLogger.info("Start CivRest HTTP Server, listening on port " + PORT);
        CivHttpHelper.registerService(server, new CivRestServices.ServiceRegisterAutom());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceCivData());
        CivHttpHelper.registerService(server, new CivRestServices.GetWaitingGame());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceJoinGame());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceItemizeCommand());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceExecuteCommand());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceDeleteGame());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceClearWaiting());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceAllReady());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceDeployGame());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}