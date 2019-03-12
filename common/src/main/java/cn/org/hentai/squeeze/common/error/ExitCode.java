package cn.org.hentai.squeeze.common.error;

/**
 * Created by matrixy on 2019/3/13.
 */
public final class ExitCode
{
    public static final int OK = 0x00;
    public static final int INVALID_LEVEL = 0x01;
    public static final int INVALID_THREAD_COUNT = 0x02;
    public static final int INVALID_BANDWIDTH = 0x03;
    public static final int UNSUPPORTED_COMPRESS_METHOD = 0x04;
    public static final int MISSING_RECEIVER = 0x05;
    public static final int MISSING_SOURCE_FILE = 0x06;
    public static final int INVALID_RECEIVER = 0x07;
    public static final int NETWORK_ERROR = 0x08;
}
