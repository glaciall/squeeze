package cn.org.hentai.squeeze.common.compress;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by matrixy on 2019/3/10.
 */
public final class CompressUtil
{
    public static void compressAndConvertTo(String basePath, String srcFilePath, int level, String method, PipedInputStream pipedReader)
    {
        File srcFile = new File(srcFilePath);
        if (CompressMethod.zip.name().equals(method)) compressAsZip(basePath, srcFile, level, pipedReader);
        else if (CompressMethod.sevenZip.name().equals(method)) compressAs7Zip(basePath, srcFile, level, pipedReader);
        else throw new RuntimeException("unsupported compress method");
    }

    private static void compressAsZip(String basePath, File srcFile, int level, PipedInputStream pis)
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
            ZipEntry entry = new ZipEntry(convertEntryName(basePath, srcFile.getAbsolutePath()));
            zos.putNextEntry(entry);
            int len = -1;
            byte[] block = new byte[4096];
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

    private static void compressAs7Zip(String basePath, File srcFile, int level, PipedInputStream pis)
    {
        return;
    }

    // 转为以basePath作为起始目录的相对路径
    private static String convertEntryName(String basePath, String absolutePath)
    {
        absolutePath = absolutePath.substring(basePath.length());
        StringBuilder sb = new StringBuilder(absolutePath.length());
        for (int i = 0; i < absolutePath.length(); i++)
        {
            char p = absolutePath.charAt(i);
            if (p == '\\') sb.append('/');
            else sb.append(p);
        }
        return sb.toString();
    }
}
