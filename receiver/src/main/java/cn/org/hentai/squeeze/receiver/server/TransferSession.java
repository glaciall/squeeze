package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.protocol.Command;
import cn.org.hentai.squeeze.common.protocol.Packet;
import cn.org.hentai.squeeze.common.util.ByteHolder;
import cn.org.hentai.squeeze.common.util.Configs;
import cn.org.hentai.squeeze.common.util.MD5;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by matrixy on 2019/3/17.
 */
public class TransferSession extends Thread
{
    Socket connection = null;

    public TransferSession(Socket connection)
    {
        this.connection = connection;
    }

    public void run()
    {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try
        {
            bis = new BufferedInputStream(connection.getInputStream(), 4096);
            bos = new BufferedOutputStream(connection.getOutputStream(), 4096);

            Packet auth = read(bis);
            auth.seek(2);
            if ((auth.nextByte() & 0xff) != (int)Command.AUTHENTICATION) throw new RuntimeException("Authenticate required.");

            int result = 0;
            auth.seek(13);
            if (MD5.encode(new String(auth.nextBytes(32)) + ":::" + Configs.get("transfer-key")).equals(new String(auth.nextBytes(32))) == false) result = 1;

            Packet resp = Packet.create(14)
                    .addShort((short)0xAAAB)
                    .addByte(Command.AUTHENTICATION)
                    .addByte((byte)0x01)
                    .addLong(0L)
                    .addByte((byte)(result & 0x01));

            bos.write(resp.getBytes());
            bos.flush();
            if (result != 0x00) return;

            while (true)
            {
                Packet data = read(bis);
                // 分发到多个线程中进行解压或是直接保存到文件
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return;
        }
    }

    ByteHolder buffer = new ByteHolder(45056);
    private Packet read(InputStream is) throws Exception
    {
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
        int pLen = buffer.getInt(3);
        if (buffer.size() < pLen) return null;
        byte[] p = new byte[13 + pLen];
        buffer.sliceInto(p, 13 + pLen);
        if (p[0] == 0xAA && p[1] == 0xAB) ;
        else throw new RuntimeException("invalid protocol header");
        return Packet.create(p);
    }
}
