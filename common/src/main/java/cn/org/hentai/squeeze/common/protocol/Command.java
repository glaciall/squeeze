package cn.org.hentai.squeeze.common.protocol;

/**
 * Created by matrixy on 2019/3/13.
 */
public final class Command
{
    public static final byte SEND_FILE_SEGMENT = 0x01;          // 发送文件片断
    public static final byte SEND_FILE_EOF = 0x02;              // 发送文件结束消息
}