package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.util.BPSUnit;
import cn.org.hentai.squeeze.common.util.ByteUtils;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

import static cn.org.hentai.squeeze.common.error.ExitCode.NETWORK_ERROR;

/**
 * Created by matrixy on 2019/3/11.
 */
public class FileTransfer extends Thread
{
    static final byte[] PACKET_HEADER_FLAG = new byte[] { (byte)0xAA, (byte)0xAB };

    private int bandwidthBps;
    private InetSocketAddress receiverAddress;
    private CompressorManager manager;

    public FileTransfer(CompressorManager manager, int bandwidthInBytesPerSecond, InetSocketAddress receiver)
    {
        this.manager = manager;
        this.bandwidthBps = bandwidthInBytesPerSecond;
        this.receiverAddress = receiver;

        this.setName("file-transfer");
    }

    public void run()
    {
        Socket socket = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try
        {
            // 连接到接收方
            socket = new Socket();
            socket.setSoTimeout(5000);
            socket.setReceiveBufferSize(40960);
            socket.setSendBufferSize(40960);
            socket.connect(receiverAddress);
            bis = new BufferedInputStream(socket.getInputStream(), 1024 * 100);
            bos = new BufferedOutputStream(socket.getOutputStream(), 1024 * 100);

            System.out.println();
            System.out.println("|--------------------------------------------------------------|");
            System.out.println("|   Total Files  |    Send   |  Compress Ratio  |     Speed    |");
            System.out.println("|--------------------------------------------------------------|");

            int sendBytes = 0;
            long stime = System.currentTimeMillis();
            byte[] buff = new byte[4096];
            while (true)
            {
                // 遍历全部管道
                int i = 0;
                long lTime = System.nanoTime();
                int bps = bandwidthBps / 5;
                for (PipedReader pipedReader : manager.getPipedReaders())
                {
                    long fileId = manager.getFileId(i++);
                    if (null == pipedReader) continue;

                    if (pipedReader.isCloseReady())
                    {
                        // 发送单个文件结束消息包
                        bos.write(PACKET_HEADER_FLAG);
                        bos.write(Command.SEND_FILE_EOF);
                        bos.write(0x00);
                        bos.write(0x00);
                        bos.write(ByteUtils.toBytes(fileId));
                        // TODO: 发送原始内容MD5指纹
                        bos.flush();
                        pipedReader.close();

                        manager.showStatus(sendBytes * 5);
                        continue;
                    }
                    int bytesReady = pipedReader.available();
                    if (bytesReady > 0)
                    {
                        // 发送一个包
                        int len = pipedReader.read(buff, 0, Math.min(bytesReady, buff.length));
                        int pLen = len + 8;
                        bos.write(PACKET_HEADER_FLAG);                 // 协议头
                        bos.write(Command.SEND_FILE_SEGMENT);
                        bos.write((pLen >> 8) & 0xff);              // 消息体长度，两字节
                        bos.write(pLen & 0xff);
                        bos.write(ByteUtils.toBytes(fileId));          // file id
                        bos.write(buff, 0, len);                   // 压缩数据片断

                        // 每秒发送字节量控制
                        sendBytes += bytesReady;
                        long now = System.currentTimeMillis();
                        if (now - stime >= 200)
                        {
                            manager.showStatus(sendBytes * 5);
                            sendBytes = 0;
                            stime = System.currentTimeMillis();
                            continue;
                        }

                        if (sendBytes >= bps)
                        {
                            bos.flush();
                            manager.showStatus(sendBytes * 5);
                            Thread.sleep(200 - (now - stime));
                            sendBytes = 0;
                            stime = System.currentTimeMillis();
                        }
                    }
                }
                if (manager.transferCompleted())
                {
                    System.out.println();
                    System.out.println();
                    System.out.println("File(s) transfer completed.");
                    System.exit(0);
                    return;
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.exit(NETWORK_ERROR);
        }
        finally
        {
            try { bis.close(); } catch(Exception e) { }
            try { bos.close(); } catch(Exception e) { }
            try { socket.close(); } catch(Exception e) { }
        }
    }
}
