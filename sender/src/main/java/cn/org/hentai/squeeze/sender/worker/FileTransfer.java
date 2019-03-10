package cn.org.hentai.squeeze.sender.worker;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by matrixy on 2019/3/11.
 */
public class FileTransfer extends Thread
{
    private InetSocketAddress receiverAddress;

    public FileTransfer(InetSocketAddress receiver)
    {
        this.receiverAddress = receiver;
    }

    public void run()
    {
        try
        {
            Socket socket = new Socket();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
