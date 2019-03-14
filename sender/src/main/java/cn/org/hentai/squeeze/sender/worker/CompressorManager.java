package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.common.util.BPSUnit;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.PipedInputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by matrixy on 2019/3/11.
 * 压缩线程管理器
 */
public class CompressorManager
{
    long sequence = 1;
    LinkedList<DataFile> files;

    Object lock = new Object();

    private char[] lineBuff;
    private int totalFileCount = 0, sendFileCount = 0;

    FileCompressor[] fileCompressors;
    FileTransfer fileTransfer;
    long[] fileIds;
    PipedReader[] pipedReaders;

    private CompressorManager(String method, int threads, int level, int bandWidthInBytesPerSecond, InetSocketAddress receiverAddress)
    {
        this.lineBuff = new char[67];
        this.lineBuff[66] = '\r';

        files = new LinkedList<>();
        fileCompressors = new FileCompressor[threads];
        pipedReaders = new PipedReader[threads];
        fileIds = new long[threads];
        for (int i = 0; i < threads; i++)
        {
            fileCompressors[i] = new FileCompressor(i, this, method, level);
        }
        fileTransfer = new FileTransfer(this, bandWidthInBytesPerSecond, receiverAddress);

    }

    public void start()
    {
        for (FileCompressor compressor : fileCompressors)
        {
            compressor.start();
        }
        fileTransfer.start();
    }

    public static synchronized CompressorManager init(String method, int threads, int level, int bandWidthInBytesPerSecond, InetSocketAddress receiverAddress)
    {
        return new CompressorManager(method, threads, level, bandWidthInBytesPerSecond, receiverAddress);
    }

    public void addFile(String filePath)
    {
        synchronized (files)
        {
            files.add(new DataFile(sequence++, filePath));
            files.notifyAll();
            totalFileCount += 1;
        }
    }

    public DataFile getFile()
    {
        synchronized (files)
        {
            while (files.size() == 0) try { files.wait(); } catch(Exception e) { }
            return files.removeFirst();
        }
    }

    public void watchStream(int index, long fileId, PipedReader pipedReader)
    {
        synchronized (lock)
        {
            fileIds[index] = fileId;
            pipedReaders[index] = pipedReader;
        }
    }

    public void unwatchStream(int index)
    {
        synchronized (lock)
        {
            pipedReaders[index] = null;
            fileIds[index] = 0L;
            sendFileCount += 1;
        }
    }

    public boolean transferCompleted()
    {
        return sendFileCount == totalFileCount;
    }

    public PipedReader[] getPipedReaders()
    {
        synchronized (lock)
        {
            return pipedReaders;
        }
    }

    public long getFileId(int index)
    {
        synchronized (lock)
        {
            return fileIds[index];
        }
    }

    public void showStatus(int BPS)
    {
        int compressRatio = 0;
        String c = String.valueOf(totalFileCount);
        String s = String.valueOf(sendFileCount);
        String r = String.valueOf(compressRatio + "%");
        String b = BPSUnit.convert(BPS);
        Arrays.fill(lineBuff, 1, 64, ' ');
        lineBuff[0] = '|';
        lineBuff[17] = '|';
        lineBuff[29] = '|';
        lineBuff[48] = '|';
        lineBuff[63] = '|';
        System.arraycopy(c.toCharArray(), 0, lineBuff, 18 / 2 - c.length() + 1, c.length());
        System.arraycopy(s.toCharArray(), 0, lineBuff, 18 / 2 - s.length() + 1 + 16, s.length());
        System.arraycopy(r.toCharArray(), 0, lineBuff, 20 / 2 - r.length() + 1 + 16 + 12, r.length());
        System.arraycopy(b.toCharArray(), 0, lineBuff, 24 / 2 - b.length() + 1 + 16 + 12 + 18, b.length());
        System.out.print(new String(lineBuff));
    }

    static class DataFile
    {
        public long id;
        public String path;

        public DataFile(long id, String path)
        {
            this.id = id;
            this.path = path;
        }
    }
}