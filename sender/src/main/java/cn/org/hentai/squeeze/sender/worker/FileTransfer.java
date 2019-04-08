package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.protocol.PacketDecoder;
import cn.org.hentai.squeeze.common.protocol.PacketEncoder;
import cn.org.hentai.squeeze.common.util.ByteUtils;
import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.common.util.MD5;
import cn.org.hentai.squeeze.common.util.Nonce;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private PacketDecoder packetDecoder;

    public FileTransfer(CompressorManager manager, int bandwidthInBytesPerSecond, InetSocketAddress receiver)
    {
        this.manager = manager;
        this.bandwidthBps = bandwidthInBytesPerSecond;
        this.receiverAddress = receiver;
        this.packetDecoder = new PacketDecoder();

        this.setName("file-transfer");
    }

    public void run()
    {
        Socket socket = null;
        InputStream bis = null;
        OutputStream bos = null;

        try
        {
            // 连接到接收方
            socket = new Socket();
            socket.setSoTimeout(5000);
            socket.connect(receiverAddress, 5000);
            bis = socket.getInputStream();
            bos = socket.getOutputStream();

            System.out.println();

            // 先完成与服务器端的通信验证
            // 消息头[2] | 指令[1] | 消息体长度[2] | 文件id[8] | 消息体[n]
            String nonce = Nonce.generate(32);
            byte[] auth = (nonce + MD5.encode(nonce + ":::" + Configs.get("transfer-key"))).getBytes();
            bos.write(PacketEncoder.encode(Command.AUTHENTICATION, 0L, auth));
            bos.flush();

            Packet resp = null;
            while ((resp = packetDecoder.read(bis)) == null) sleep(5);

            if ((resp.seek(13).nextByte() & 0xff) != 0x00)
            {
                System.err.println("invalid transfer key");
                System.exit(11);
                return;
            }
            long sessionId = (resp.seek(5).nextLong() & 0xffffffff) << 32;

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
                    long fileId = sessionId | (manager.getFileId(i++) & 0xffffffff);
                    if (null == pipedReader) continue;

                    int bytesReady = pipedReader.available();
                    if (bytesReady > 0)
                    {
                        // 发送一个包
                        int len = pipedReader.read(buff, 0, Math.min(bytesReady, buff.length));
                        byte[] block = buff;
                        if (len != buff.length)
                        {
                            block = new byte[len];
                            System.arraycopy(buff, 0, block, 0, len);
                        }
                        bos.write(PacketEncoder.encode(Command.SEND_FILE_SEGMENT, fileId, block));
                        bos.flush();

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
                        continue;
                    }

                    if (pipedReader.isCloseReady())
                    {
                        // 发送单个文件结束消息包
                        bos.write(PacketEncoder.encode(Command.SEND_FILE_EOF, fileId, new byte[32]));
                        bos.flush();
                        pipedReader.close();

                        manager.showStatus(sendBytes, totalSendBytes);
                        continue;
                    }
                }
                if (manager.transferCompleted())
                {
                    manager.showStatus(sendBytes, totalSendBytes);
                    System.out.println();
                    System.out.println();

                    System.out.println("File(s) transfer completed, wait for server response...");

                    // 发送传输结束消息
                    bos.write(PacketEncoder.encode(Command.TRANSFER_COMPLETED, 0L, new byte[] { 0x00 }));
                    bos.flush();

                    while ((resp = packetDecoder.read(bis)) == null) sleep(5);

                    System.out.println("Done.");

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
