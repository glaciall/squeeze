package cn.org.hentai.squeeze.common.util;

/**
 * Created by matrixy on 2019/3/13.
 */
public final class BPSUnit
{
    public static String convert(int BPS)
    {
        char unit = 'k';
        float speed = 0;
        if (BPS > 1024 * 1024 * 1024) { speed = BPS / 1024f / 1024 / 1024; unit = 'G'; }
        else if (BPS > 1024 * 1024) { speed = BPS / 1024f / 1024; unit = 'M'; }
        else speed = BPS / 1024f;
        return String.format("%.2f%s", speed, unit);
    }
}
