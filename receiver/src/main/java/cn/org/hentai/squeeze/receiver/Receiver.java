package cn.org.hentai.squeeze.receiver;

import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.receiver.server.TransferSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * Created by matrixy on 2019/3/14.
 */
public class Receiver
{
    static Logger logger = LoggerFactory.getLogger(Receiver.class);

    public static void main(String[] args) throws Exception
    {
        Configs.init("/conf.properties");

        Signal.handle(new Signal("TERM"), new SignalHandler()
        {
            @Override
            public void handle(Signal signal)
            {
                System.out.println("Exiting...");
                System.exit(0);
            }
        });

        showBanner();
        startServer();
    }

    private static void startServer() throws Exception
    {
        ServerSocket server = new ServerSocket(Configs.getInt("server.port", 19000), 5);
        while (true)
        {
            Socket socket = server.accept();
            new TransferSession(socket).handle();
        }
    }

    private static void showBanner()
    {
        String banner = "                                     __   \n" +
                " _____                               \\ \\  \n" +
                "|   __|___ _ _ ___ ___ ___ ___    ___ \\ \\ \n" +
                "|__   | . | | | -_| -_|- _| -_|  |___| > >\n" +
                "|_____|_  |___|___|___|___|___|       / / \n" +
                "        |_|                          /_/  \n";
        System.out.println(banner);

        logger.info("Squeeze started at: " + new Date().toLocaleString());
    }
}
