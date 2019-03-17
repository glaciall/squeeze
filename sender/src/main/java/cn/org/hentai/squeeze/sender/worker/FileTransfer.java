package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.util.ByteUtils;
import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.common.util.MD5;
import cn.org.hentai.squeeze.common.util.Nonce;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

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

            // 先完成与服务器端的通信验证
            // 消息头[2] | 指令[1] | 消息体长度[2] | 文件id[8] | 消息体[n]
            String nonce = Nonce.generate(32);
            Packet auth = Packet.create(13 + 64)
                    .addBytes(PACKET_HEADER_FLAG)
                    .addByte(Command.AUTHENTICATION)
                    .addShort((short)32)
                    .addLong(0L)
                    .addBytes(nonce.getBytes())
                    .addBytes(MD5.encode(nonce + ":::" + Configs.get("transfer-key")).getBytes());
            bos.write(auth.getBytes());
            bos.flush();

            if ((bis.read() & 0xff) == 0xAA && (bis.read() & 0xff) == 0xAB && (bis.read() & 0xff) == (int)Command.AUTHENTICATION);
            else
            {
                System.err.println("invalid protocol response");
                System.exit(10);
            }
            for (int i = 0; i < 10; i++) bis.read();
            if (bis.read() != 0x00)
            {
                System.err.println("invalid transfer key");
                System.exit(11);
            }

            System.out.println("|--------------------------------------------------------------|");
            System.out.println("|   Total Files  |    Send Files  |   Send Bytes  |    Speed   |");
            System.out.println("|--------------------------------------------------------------|");

            int sendBytes = 0;
            long totalSendBytes = 0;
            long stime = System.currentTimeMillis();
            byte[] buff = new byte[4096];
            while (true)
            {
                // 遍历全部管道
                int i = 0;
                int bps = bandwidthBps;
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

                        manager.showStatus(sendBytes, totalSendBytes);
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
                        sendBytes += len;
                        totalSendBytes += len;
                        long now = System.currentTimeMillis();
                        if (now - stime >= 1000)
                        {
                            manager.showStatus(sendBytes, totalSendBytes);
                            sendBytes = 0;
                            stime = System.currentTimeMillis();
                            continue;
                        }

                        if (sendBytes >= bps)
                        {
                            bos.flush();
                            manager.showStatus(sendBytes, totalSendBytes);
                            Thread.sleep(1000 - (now - stime));
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
