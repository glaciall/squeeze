package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.compress.ZipStreamLoopback;
import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

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

    public void run()
    {
        while (!this.isInterrupted())
        {
            try
            {
                Packet packet = packets.take();
                int command = packet.seek(2).nextByte() & 0xff;
                long fileId = packet.seek(5).nextLong();
                ZipStreamLoopback zipStream = null;
                if (!zipStreams.containsKey(fileId))
                {
                    zipStream = new ZipStreamLoopback(new PipedOutputStream());
                    zipStreams.put(fileId, zipStream);
                }
                else
                {
                    zipStream = zipStreams.get(fileId);
                }

                if (command == Command.SEND_FILE_EOF)
                {
                    // read from zipStream....
                    zipStreams.remove(fileId);
                }
                else
                {
                    zipStream.getLoopbackStream().write(packet.seek(13).nextBytes());
                }
            }
            catch(Exception ex)
            {
                logger.error("unpack error", ex);
            }
        }
    }
}
