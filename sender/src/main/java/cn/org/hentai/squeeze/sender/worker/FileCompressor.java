package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.sender.compress.CompressUtil;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.PipedInputStream;

/**
 * Created by matrixy on 2019/3/11.
 */
public class FileCompressor extends Thread
{
    String method;
    int level;
    int index;
    CompressorManager manager;
    PipedReader pipedReader;

    public FileCompressor(int index, CompressorManager manager, String method, int level)
    {
        this.index = index;
        this.manager = manager;
        this.method = method;
        this.level = level;

        this.setName("compressor-" + index);
    }

    public void run()
    {
        while (!this.isInterrupted())
        {
            try
            {
                CompressorManager.DataFile file = manager.getFile();
                // System.out.println(String.format("Compressor[%6d - %6d]: %s", index, file.id, file.path));
                pipedReader = new PipedReader(40960);
                manager.watchStream(index, file.id, pipedReader);
                CompressUtil.compressAndConvertTo(file.path, level, method, pipedReader);

                pipedReader.waitForClose();
                pipedReader = null;
                manager.unwatchStream(index);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
