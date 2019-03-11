package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by matrixy on 2019/3/11.
 */
public class FileTransfer extends Thread
{
    private int bandwidthBps;
    private InetSocketAddress receiverAddress;
    private CompressorManager manager;

    public FileTransfer(CompressorManager manager, int bandwidthInBytesPerSecond, InetSocketAddress receiver)
    {
        this.manager = manager;
        this.bandwidthBps = bandwidthInBytesPerSecond;
        this.receiverAddress = receiver;
    }

    public void run()
    {
        try
        {
            // 连接到接收方
            Socket socket = new Socket();

            int sendBytes = 0;
            while (true)
            {
                // 遍历全部管道
                for (;;)
                {
                    PipedReader pipedReader = null;
                    if (null == pipedReader) continue;

                    if (pipedReader.isCloseReady())
                    {
                        // 发送单个文件结束消息包
                        pipedReader.close();
                        continue;
                    }
                    int bytesReady = pipedReader.available();
                    if (bytesReady > 0)
                    {
                        // 发送一个包
                        // 计算一下已经发送了多少字节了，以及从置零起，过去了多长时间，估算一下单位时间内的发送字节数，限制一下流量
                    }
                }
            }

            // 如果管道流已经关闭，则

            // 带宽控制
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
