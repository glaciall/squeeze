package cn.org.hentai.squeeze.receiver.server;

import cn.org.hentai.squeeze.common.compress.ZipStreamLoopback;
import cn.org.hentai.squeeze.common.util.Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;

/**
 * Created by matrixy on 2019/3/20.
 */
public class FileWriter extends Thread
{
    static Logger logger = LoggerFactory.getLogger(FileWriter.class);

    long fileId;
    ZipStreamLoopback zipDecoder;

    public FileWriter(long fileId, ZipStreamLoopback zipDecoder)
    {
        this.fileId = fileId;
        this.zipDecoder = zipDecoder;
        this.setName("file-writer-" + fileId);
    }

    public void run()
    {
        String pathname = null;
        FileOutputStream fos = null;
        String unpackPath = Configs.get("unpack-path");
        int readBytes = 0;

        try
        {
            ZipEntry entry = null;
            while ((entry = zipDecoder.getNextEntry()) != null)
            {
                byte[] buff = new byte[4096];

                File file = null;
                pathname = entry.getName();
                int seperatorIndex = pathname.indexOf('/');
                if (seperatorIndex > -1)
                {
                    new File(unpackPath + File.separator + pathname.substring(0, pathname.lastIndexOf('/'))).mkdirs();
                }
                file = new File(unpackPath + File.separator + entry.getName());

                fos = new FileOutputStream(file);
                int len = -1;
                while (zipDecoder.available() > 0)
                {
                    len = zipDecoder.read(buff, 0, Math.min(zipDecoder.available(), buff.length));
                    if (len == -1) break;
                    readBytes += len;
                    fos.write(buff, 0, len);
                }
                zipDecoder.closeEntry();

                logger.info("unpack: {}, {}, size: {}", fileId, pathname, readBytes);
                break;
            }
        }
        catch(Exception ex)
        {
            logger.error("zip entry decode error: {}", pathname, ex);
        }
        finally
        {
            try { fos.close(); } catch(Exception e) { }
            try { zipDecoder.close(); } catch(Exception e) { }
        }
    }
}
