package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.util.Configs;

/**
 * Created by matrixy on 2019/3/18.
 */
public final class DecompressManager
{
    PacketDecompressWorker[] decompressWorkers;
    private DecompressManager()
    {
        int cores = Configs.getInt("unpack-thread-count", 2);
        for (int i = 0; i < cores; i++)
        {
            decompressWorkers[i] = new PacketDecompressWorker();
            decompressWorkers[i].setName("decoder-" + i);
            decompressWorkers[i].start();
        }
    }

    public void dispatch(Packet packet)
    {
        int idx = 0;
        long fileId = packet.seek(5).nextLong();
        idx = (int)((fileId & 0x7fffffffffffffffL) % decompressWorkers.length);
        decompressWorkers[idx].consume(packet.rewind());
    }

    static DecompressManager instance;
    public static synchronized DecompressManager getInstance()
    {
        if (null == instance) instance = new DecompressManager();
        return instance;
    }
}
