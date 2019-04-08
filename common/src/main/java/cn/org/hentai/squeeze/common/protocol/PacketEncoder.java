package cn.org.hentai.squeeze.common.protocol;

/**
 * Created by matrixy on 2019/3/21.
 */
public final class PacketEncoder
{
    public static byte[] encode(byte command, long fileId, byte[] body)
    {
        return Packet.create(2 + 1 + 2 + 8 + body.length)
                .addShort((short)0xAAAB)
                .addByte(command)
                .addShort((short)body.length)
                .addLong(fileId)
                .addBytes(body)
                .getBytes();
    }
}
