package cn.org.hentai.squeeze.common.protocol;

import cn.org.hentai.squeeze.common.util.ByteHolder;
import cn.org.hentai.squeeze.common.util.ByteUtils;

import java.io.InputStream;

/**
 * Created by matrixy on 2019/3/20.
 */
public final class PacketDecoder
{
    ByteHolder buffer;

    public PacketDecoder(int bufSize)
    {
        buffer = new ByteHolder(bufSize);
    }

    public PacketDecoder()
    {
        this(655360);
    }

    public Packet read(InputStream is) throws Exception
    {
        Packet packet = decode();
        if (packet != null) return packet;

        int readyBytes = is.available();
        if (readyBytes > 0)
        {
            byte[] block = new byte[readyBytes];
            int len = is.read(block, 0, readyBytes);
            buffer.write(block, 0, len);
        }

        return decode();
    }

    private Packet decode()
    {
        if (buffer.size() < 13) return null;
        int pLen = buffer.getShort(3) & 0xffff;
        if (buffer.size() < pLen + 13) return null;
        byte[] p = new byte[13 + pLen];
        buffer.sliceInto(p, 13 + pLen);
        if ((p[0] & 0xff) == 0xAA && (p[1] & 0xff) == 0xAB) ;
        else throw new RuntimeException("invalid protocol header: " + ByteUtils.toString(p, 13));
        return Packet.create(p);
    }
}
