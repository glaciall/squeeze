package cn.org.hentai.squeeze.common.compress;

/**
 * Created by matrixy on 2019/3/15.
 */
public enum CompressMethod
{
    zip(1, "zip"), sevenZip(2, "7zip");

    private byte code;
    private String name;
    CompressMethod(int code, String name)
    {
        this.code = (byte)code;
        this.name = name;
    }

    public String value()
    {
        return name;
    }

    public byte code()
    {
        return this.code;
    }
}
