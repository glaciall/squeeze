package cn.org.hentai.squeeze.sender.util;

import java.io.File;

/**
 * Created by matrixy on 2019/3/10.
 * 文件遍历
 */
public final class FileTraverser
{
    public interface Callback
    {
        void found(File file);
    }

    public static void traverse(File path, Callback callback)
    {
        if (path.isFile()) { callback.found(path); return; }
        for (File file : path.listFiles())
        {
            traverse(file, callback);
        }
    }
}
