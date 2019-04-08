package cn.org.hentai.squeeze.common.protocol;

import cn.org.hentai.squeeze.common.util.ByteHolder;
import cn.org.hentai.squeeze.common.util.ByteUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by matrixy on 2019/3/20.
 */
public class Test
{
    public static void main(String[] args) throws Exception
    {
        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream(pos);

        new Thread()
        {
            public void run()
            {
                try
                {
                    ServerSocket server = new ServerSocket(1024);
                    Socket conn = server.accept();
                    InputStream in = conn.getInputStream();
                    int len = -1;
                    byte[] block = new byte[64];
                    while ((len = in.read(block)) > -1)
                    {
                        pos.write(block, 0, len);
                        if ("exit".equals(new String(block, 0, len).trim()))
                            break;
                    }
                    pos.close();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }.start();

        int len = -1;
        byte[] block = new byte[32];
        while ((len = pis.read(block)) > -1)
        {
            // do nothing here...
            System.out.println("> " + new String(block, 0, len).trim());
        }
        pis.close();
        System.out.println("# 正常结束啦");
    }
}
