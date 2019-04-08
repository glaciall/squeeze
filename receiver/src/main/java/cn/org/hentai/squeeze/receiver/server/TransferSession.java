package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.protocol.PacketDecoder;
import cn.org.hentai.squeeze.common.protocol.PacketEncoder;
import cn.org.hentai.squeeze.common.util.ByteHolder;
import cn.org.hentai.squeeze.common.util.ByteUtils;
import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.common.util.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by matrixy on 2019/3/17.
 */
public class TransferSession
{
    static Logger logger = LoggerFactory.getLogger(TransferSession.class);
    static AtomicInteger sessionIdSequence = new AtomicInteger(1);

    Socket connection = null;
    PacketDecoder packetDecoder = null;
    int id;

    public TransferSession(Socket connection)
    {
        this.connection = connection;
        this.packetDecoder = new PacketDecoder();
        this.id = sessionIdSequence.addAndGet(1);
    }

    public void handle()
    {
        InputStream bis = null;
        OutputStream bos = null;
        try
        {
            bis = new BufferedInputStream(connection.getInputStream(), 4096);
            bos = new BufferedOutputStream(connection.getOutputStream(), 4096);

            Packet auth = null;
            while ((auth = packetDecoder.read(bis)) == null) sleep(500);
            auth.seek(2);
            if ((auth.nextByte() & 0xff) != (int)Command.AUTHENTICATION) throw new RuntimeException("Authenticate required.");

            int result = 0;
            auth.seek(13);
            if (MD5.encode(new String(auth.nextBytes(32)) + ":::" + Configs.get("transfer-key")).equals(new String(auth.nextBytes(32))) == false) result = 1;

            bos.write(PacketEncoder.encode(Command.AUTHENTICATION, this.id, new byte[] { (byte)(result & 0x01) }));
            bos.flush();
            if (result != 0x00) return;

            logger.info("Client: {} connected and starting to transfer files...", connection.getInetAddress());

            long idleTime = 0;
            long lastActiveTime = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted())
            {
                Packet data = packetDecoder.read(bis);
                // 分发到多个线程中进行解压或是直接保存到文件

                if (data != null)
                {
                    if (data.seek(2).nextByte() == Command.TRANSFER_COMPLETED)
                    {
                        bos.write(PacketEncoder.encode(Command.TRANSFER_COMPLETED, 0, new byte[] { 0x01 }));
                        bos.flush();

                        logger.info("transfer completed...");
                        break;
                    }
                    DecompressManager.getInstance().dispatch(data);
                    lastActiveTime = System.currentTimeMillis();
                    continue;
                }
                idleTime = System.currentTimeMillis() - lastActiveTime;
                if (idleTime >= 5000)
                {
                    logger.error("session timeout and closed...");
                    break;
                }
                Thread.sleep(1);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try { bis.close(); } catch(Exception e) { }
            try { bos.close(); } catch(Exception e) { }
            try { connection.close(); } catch(Exception e) { }
        }
    }

    private void sleep(int ms)
    {
        try { Thread.sleep(ms); } catch(Exception e) { }
    }
}
