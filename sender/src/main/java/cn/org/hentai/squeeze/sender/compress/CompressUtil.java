package cn.org.hentai.squeeze.sender.compress;

import cn.org.hentai.squeeze.sender.util.Configs;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by matrixy on 2019/3/10.
 */
public final class CompressUtil
{
    public static final String METHOD_ZIP = "zip";
    public static final String METHOD_7ZIP = "7zip";

    public static void compressAndWriteTo(File srcFile, int level, String method, PipedInputStream pis)
    {
        if (METHOD_ZIP.equals(method)) compressAsZip(srcFile, level, pis);
        else if (METHOD_7ZIP.equals(method)) compressAs7Zip(srcFile, level, pis);
        else throw new RuntimeException("unsupported compress method");
    }

    private static void compressAsZip(File srcFile, int level, PipedInputStream pis)
    {
        FileInputStream fis = null;
        PipedOutputStream pos = null;
        ZipOutputStream zos = null;
        try
        {
            fis = new FileInputStream(srcFile);
            pos = new PipedOutputStream(pis);
            zos = new ZipOutputStream(pos);
            zos.setLevel(5);
            zos.putNextEntry(new ZipEntry("segment"));
            int len = -1;
            byte[] block = new byte[4196];
            while ((len = fis.read(block)) > -1)
            {
                zos.write(block, 0, len);
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try { fis.close(); } catch(Exception e) { }
            try { zos.close(); } catch(Exception e) { }
            try { pos.close(); } catch(Exception e) { }
        }
    }

    private static void compressAs7Zip(File srcFile, int level, PipedInputStream pis)
    {
        return;
    }

    public static void main(String[] args) throws Exception
    {
        Configs.init("/conf.properties");

        File srcFile = new File("E:\\test\\test.dat");
        System.out.println(srcFile.length());
        final PipedInputStream pis = new PipedInputStream(4096);

        new Thread()
        {
            public void run()
            {
                try
                {
                    FileOutputStream fos = new FileOutputStream("E:\\test\\fuckoff.zip");

                    int len = -1;
                    byte[] block = new byte[4096];
                    while ((len = pis.read(block)) > -1)
                    {
                        fos.write(block, 0, len);
                    }
                    fos.close();
                    pis.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();

        compressAndWriteTo(srcFile, 5, METHOD_ZIP, pis);
    }
}
