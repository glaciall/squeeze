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
    boolean readyToClose = false;

    public ZipStreamLoopback(PipedOutputStream out) throws IOException
    {
        super(new PipedInputStream(out, 4096));
        this.out = out;
    }

    public OutputStream getLoopbackStream()
    {
        return this.out;
    }

    public void readyToClose()
    {
        readyToClose = true;
    }

    public boolean isReadyToClose()
    {
        return readyToClose;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
    }
}
