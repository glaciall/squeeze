package cn.org.hentai.squeeze.receiver;

import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.receiver.server.TransferSession;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by matrixy on 2019/3/14.
 */
public class Receiver
{
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

        startServer();
    }

    private static void startServer() throws Exception
    {
        ServerSocket server = new ServerSocket(Configs.getInt("server.port", 19000), 5);
        while (true)
        {
            Socket socket = server.accept();
            new TransferSession(socket).start();
        }
    }
}
