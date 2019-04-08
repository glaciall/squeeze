package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.compress.ZipStreamLoopback;
import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by matrixy on 2019/3/17.
 */
public class PacketDecompressWorker extends Thread
{
    static Logger logger = LoggerFactory.getLogger(PacketDecompressWorker.class);

    ArrayBlockingQueue<Packet> packets = new ArrayBlockingQueue(64);
    Map<Long, ZipStreamLoopback> zipStreams = new HashMap();

    public void consume(Packet packet)
    {
        try
        {
            packets.put(packet);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    static AtomicInteger totalFiles = new AtomicInteger(0), finishedFiles = new AtomicInteger(0);

    public void run()
    {
        while (!this.isInterrupted())
        {
            try
            {
                Packet packet = packets.take();
                int command = packet.seek(2).nextByte() & 0xff;
                long fileId = packet.seek(5).nextLong();
                // logger.info(String.format("fileId: %d, command: %x", fileId, command));
                ZipStreamLoopback zipStream = null;
                if (!zipStreams.containsKey(fileId))
                {
                    zipStream = new ZipStreamLoopback(new PipedOutputStream());
                    zipStreams.put(fileId, zipStream);
                    new FileWriter(fileId, zipStream).start();
                    totalFiles.addAndGet(1);
                }
                else
                {
                    zipStream = zipStreams.get(fileId);
                }

                if (command == Command.SEND_FILE_EOF)
                {
                    zipStreams.remove(fileId);
                    zipStream.getLoopbackStream().close();
                    finishedFiles.addAndGet(1);
                }
                else
                {
                    try
                    {
                        zipStream.getLoopbackStream().write(packet.seek(13).nextBytes());
                    }
                    catch(IOException ex)
                    {
                        // 彻底屏蔽这里的异常，不影响使用
                        // 好像缺了几个字节也能正常的完成解压，所以流可能会被提前关闭掉
                    }
                }
            }
            catch(Exception ex)
            {
                logger.error("unpack error", ex);
            }
        }
    }
}
