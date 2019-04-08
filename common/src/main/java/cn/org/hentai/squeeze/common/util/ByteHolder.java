package cn.org.hentai.squeeze.common.util;

import java.util.Arrays;

/**
 * Created by matrixy on 2018-06-15.
 */
public class ByteHolder
{
    int offset = 0;
    int size = 0;
    byte[] buffer = null;

    public ByteHolder(int bufferSize)
    {
        this.buffer = new byte[bufferSize];
    }

    public int size()
    {
        return this.size;
    }

    public void write(byte[] data)
    {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length)
    {
        while (this.offset + length > buffer.length)
        {
            try { Thread.sleep(1); } catch(Exception e) { }
        }

        // 复制一下内容
        System.arraycopy(data, offset, buffer, this.offset, length);

        this.offset += length;
        this.size += length;
    }

    public byte[] array()
    {
        return Arrays.copyOf(this.buffer, this.size);
    }

    public void write(byte b)
    {
        this.buffer[offset++] = b;
        this.size += 1;
    }

    public void sliceInto(byte[] dest, int length)
    {
        if (length > this.size) throw new RuntimeException(String.format("exceed max length: %d / %d", length, size));
        System.arraycopy(this.buffer, 0, dest, 0, length);
        // 往前挪length个位
        System.arraycopy(this.buffer, length, this.buffer, 0, this.size - length);
        this.offset -= length;
        this.size -= length;
    }

    /**
     * 从buffer的position位置起，复制length个字节到dest数组中
     * @param dest 目标数组
     * @param position buffer的开始位置
     * @param length 从buffer复制的字节数
     */
    public void getBytes(byte[] dest, int position, int length)
    {
        System.arraycopy(this.buffer, position, dest, 0, length);
    }

    public void slice(int length)
    {
        // 往前挪length个位
        System.arraycopy(this.buffer, length, this.buffer, 0, this.size - length);
        this.offset -= length;
        this.size -= length;
    }

    public byte get(int position)
    {
        return this.buffer[position];
    }

    public int getInt(int position)
    {
        return ByteUtils.getInt(this.buffer, position, 4);
    }

    public int getShort(int position)
    {
        return (((buffer[position] & 0xff) << 8) | (buffer[position + 1] & 0xff)) & 0xffff;
    }

    public void clear()
    {
        this.offset = 0;
        this.size = 0;
    }
}