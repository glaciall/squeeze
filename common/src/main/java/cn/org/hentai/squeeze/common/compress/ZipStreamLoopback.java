package cn.org.hentai.squeeze.common.compress;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by matrixy on 2019/3/18.
 */
public class ZipStreamLoopback extends ZipInputStream
{
    PipedOutputStream out = null;

    public ZipStreamLoopback(PipedOutputStream out) throws IOException
    {
        super(new PipedInputStream(out, 4096));
        this.out = out;
    }

    public OutputStream getLoopbackStream()
    {
        return this.out;
    }

    public static void main(String[] args) throws Exception
    {
        ZipStreamLoopback zip = new ZipStreamLoopback(new PipedOutputStream());

        new Thread()
        {
            public void run()
            {
                try
                {
                    FileInputStream fis = new FileInputStream("E:\\test\\test.zip");
                    OutputStream writer = zip.getLoopbackStream();
                    int len = -1;
                    byte[] block = new byte[4096];
                    while ((len = fis.read(block)) > -1)
                    {
                        writer.write(block, 0, len);
                        Thread.sleep(500);
                    }
                    fis.close();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }.start();

        ZipEntry entry = null;
        while ((entry = zip.getNextEntry()) != null)
        {
            System.out.println("unpack: " + entry.getName());
            FileOutputStream fos = new FileOutputStream("E:\\test\\" + entry.getName());
            int len = -1;
            byte[] block = new byte[512];
            while ((len = zip.read(block)) > -1)
            {
                fos.write(block, 0, len);
            }
            fos.close();
        }
        zip.close();
    }
}
