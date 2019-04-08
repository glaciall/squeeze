package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.common.util.BytesUnit;
import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/3/11.
 * 压缩线程管理器
 */
public class CompressorManager
{
    int sequence = 1;
    LinkedList<DataFile> files;

    Object lock = new Object();

    private char[] lineBuff;
    private int totalFileCount = 0, sendFileCount = 0;
    private long totalFileBytes = 0L;

    FileCompressor[] fileCompressors;
    FileTransfer fileTransfer;
    int[] fileIds;
    PipedReader[] pipedReaders;

    private CompressorManager(String method, int threads, int level, int bandWidthInBytesPerSecond, InetSocketAddress receiverAddress)
    {
        this.lineBuff = new char[67];
        this.lineBuff[66] = '\r';

        files = new LinkedList<>();
        fileCompressors = new FileCompressor[threads];
        pipedReaders = new PipedReader[threads];
        fileIds = new int[threads];
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

    public void addFile(String basePath, String filePath, long fileBytes)
    {
        synchronized (files)
        {
            files.add(new DataFile(sequence++, filePath, basePath));
            files.notifyAll();
            totalFileCount += 1;
            totalFileBytes += fileBytes;
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

    public int getTotalFileCount()
    {
        return totalFileCount;
    }

    public long getTotalFileBytes()
    {
        return totalFileBytes;
    }

    public void watchStream(int index, int fileId, PipedReader pipedReader)
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
            fileIds[index] = 0;
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

    public int getFileId(int index)
    {
        synchronized (lock)
        {
            return fileIds[index];
        }
    }

    public void showStatus(int BPS, long totalSendBytes)
    {
        String c = String.valueOf(totalFileCount);
        String s = String.valueOf(sendFileCount);
        String t = BytesUnit.convert(totalSendBytes);
        String b = BytesUnit.convert(BPS);
        Arrays.fill(lineBuff, 1, 64, ' ');
        lineBuff[0] = '|';
        lineBuff[17] = '|';
        lineBuff[34] = '|';
        lineBuff[50] = '|';
        lineBuff[63] = '|';
        System.arraycopy(c.toCharArray(), 0, lineBuff, 22 / 2 - c.length() + 1, c.length());
        System.arraycopy(s.toCharArray(), 0, lineBuff, 22 / 2 - s.length() + 1 + 16, s.length());
        System.arraycopy(t.toCharArray(), 0, lineBuff, 20 / 2 - t.length() + 1 + 16 + 18, t.length());
        System.arraycopy(b.toCharArray(), 0, lineBuff, 20 / 2 - b.length() + 1 + 16 + 12 + 21, b.length());
        System.out.print(new String(lineBuff));
    }

    static class DataFile
    {
        public int id;
        public String path;
        public String basePath;

        public DataFile(int id, String path, String basePath)
        {
            this.id = id;
            this.path = path;
            this.basePath = basePath;
        }
    }
}