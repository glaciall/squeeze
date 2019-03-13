package cn.org.hentai.squeeze.common.util;

/**
 * Created by matrixy on 2019/3/13.
 */
public final class BPSUnit
{
    public static String convert(int BPS)
    {
        char unit = 'k';
        if (BPS > 1024 * 1024 * 1024) { BPS = BPS / 1024 / 1024 / 1024; unit = 'G'; }
        if (BPS > 1024 * 1024) { BPS = BPS / 1024 / 1024; unit = 'M'; }
        else BPS = BPS / 1024;
        return String.valueOf(BPS) + unit;
    }
}
