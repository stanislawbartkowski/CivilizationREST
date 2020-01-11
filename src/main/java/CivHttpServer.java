import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/*
 * a simple static http server
 */
public class CivHttpServer {


    private static int PORT = 8000;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        CivLogger.info("Start CivRest HTTP Server, listening on port " + PORT);
        CivHttpHelper.registerService(server, new CivRestServices.ServiceRegisterAutom());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceCivData());
        CivHttpHelper.registerService(server, new CivRestServices.GetWaitingGame());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceJoinGame());
        CivHttpHelper.registerService(server, new CivRestServices.ServiceItemizeCommand());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}