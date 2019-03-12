package cn.org.hentai.squeeze.sender.compress;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by matrixy on 2019/3/10.
 */
public final class CompressUtil
{
    public static final String METHOD_ZIP = "zip";
    public static final String METHOD_7ZIP = "7zip";

    public static void compressAndConvertTo(String srcFilePath, int level, String method, PipedInputStream pipedReader)
    {
        File srcFile = new File(srcFilePath);
        if (METHOD_ZIP.equals(method)) compressAsZip(srcFile, level, pipedReader);
        else if (METHOD_7ZIP.equals(method)) compressAs7Zip(srcFile, level, pipedReader);
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
            zos.putNextEntry(new ZipEntry(srcFile.getAbsolutePath()));
            int len = -1;
            byte[] block = new byte[4196];
            while ((len = fis.read(block)) > -1)
            {
                zos.write(block, 0, len);
            }
            zos.flush();
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
}
