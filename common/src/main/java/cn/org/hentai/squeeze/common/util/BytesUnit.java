package cn.org.hentai.squeeze.common.util;

/**
 * Created by matrixy on 2019/3/13.
 */
public final class BytesUnit
{
    public static String convert(long BPS)
    {
        char unit = 'k';
        double speed = 0d;
        if (BPS > 1024 * 1024 * 1024) { speed = BPS / 1024d / 1024 / 1024; unit = 'G'; }
        else if (BPS > 1024 * 1024) { speed = BPS / 1024d / 1024; unit = 'M'; }
        else speed = BPS / 1024d;
        return String.format("%.2f%s", speed, unit);
    }
}
