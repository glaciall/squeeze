package cn.org.hentai.squeeze.sender.util;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by matrixy on 2017/8/14.
 */
public final class Configs
{
    static Properties properties = new Properties();

    public static void init(String configFilePath)
    {
        try
        {
            properties.load(Configs.class.getResourceAsStream(configFilePath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String get(String key)
    {
        Object val = properties.get(key);
        if (null == val) return null;
        else return String.valueOf(val).trim();
    }

    public static String get(String key, String defaultVal)
    {
        Object val = properties.get(key);
        if (null == val) return defaultVal;
        else return String.valueOf(val).trim();
    }

    public static int getInt(String key, int defaultVal)
    {
        String val = get(key, String.valueOf(defaultVal));
        return Integer.parseInt(val);
    }
}
