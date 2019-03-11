package cn.org.hentai.squeeze.sender.worker;

import cn.org.hentai.squeeze.sender.util.PipedReader;

import java.io.PipedInputStream;
import java.net.InetSocketAddress;
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
    LinkedBlockingQueue<CompressedBlock> compressedBlocks;

    FileCompressor[] fileCompressors;
    FileTransfer fileTransfer;
    long[] fileIds;
    PipedReader[] pipedReaders;

    private CompressorManager(String method, int threads, int level, int bandWidthInBytesPerSecond, InetSocketAddress receiverAddress)
    {
        files = new LinkedList<>();
        compressedBlocks = new LinkedBlockingQueue(10000);
        fileCompressors = new FileCompressor[threads];
        pipedReaders = new PipedReader[threads];
        fileIds = new long[threads];
        for (int i = 0; i < threads; i++)
        {
            fileCompressors[i] = new FileCompressor(i, this, method, level);
            fileCompressors[i].start();
        }
        fileTransfer = new FileTransfer(this, bandWidthInBytesPerSecond, receiverAddress);
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
        pipedReaders[index] = pipedReader;
        fileIds[index] = fileId;
    }

    public CompressedBlock getCompressedBlock()
    {
        return compressedBlocks.peek();
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

    static class CompressedBlock
    {
        public long fileId;
        public byte[] block;

        public CompressedBlock(long fid, byte[] block)
        {
            this.fileId = fid;
            this.block = block;
        }
    }
}