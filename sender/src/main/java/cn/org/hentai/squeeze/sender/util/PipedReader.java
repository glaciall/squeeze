package cn.org.hentai.squeeze.sender.util;

import java.io.IOException;
import java.io.PipedInputStream;

/**
 * Created by matrixy on 2019/3/12.
 */
public class PipedReader extends PipedInputStream
{
    private boolean readyToClose = false;
    private Object lock = new Object();
    private boolean closed = false;

    public PipedReader(int pipeSize)
    {
        super(pipeSize);
    }

    public boolean isCloseReady()
    {
        return readyToClose;
    }

    public void waitForClose()
    {
        readyToClose = true;
        synchronized (lock)
        {
            while (!closed) try { lock.wait(); } catch(Exception e) { }
        }
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        closed = true;
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }
}
