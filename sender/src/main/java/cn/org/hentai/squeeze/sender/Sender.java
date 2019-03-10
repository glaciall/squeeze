package cn.org.hentai.squeeze.sender;

import cn.org.hentai.squeeze.sender.util.Configs;
import cn.org.hentai.squeeze.sender.util.FileTraverser;
import cn.org.hentai.squeeze.sender.worker.FileTransfer;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/3/10.
 */
public class Sender
{
    public static void main(String[] args) throws Exception
    {
        // 初始化
        Configs.init("/conf.properties");

        // 参数解析
        int level = getIntArgument(args, "level", Configs.getInt("default-compress-level", -1));
        int threads = getIntArgument(args, "threads", Configs.getInt("default-thread-count", Runtime.getRuntime().availableProcessors()));
        String bandWidth = getArgument(args, "bandwidth", Configs.get("default-band-width"));
        String method = getArgument(args, "method", Configs.get("default-compress-method", "zip"));
        String receiver = getArgument(args, "receiver", Configs.get("default-receiver"));

        if (level < 0 || level > 9) { showErrorAndExit("invalid level parameter, please input a number between 0 ~ 9"); showHelpAndExit(1); return; }
        if (threads < 1) { showErrorAndExit("invalid thread count"); showHelpAndExit(2); return; }
        if (bandWidth != null && bandWidth.matches("(?is)^\\d+[kmg]$") == false) { showErrorAndExit("invalid bandwidth limitation"); showHelpAndExit(3); return; }
        if (method.matches("^7?zip$") == false) { showErrorAndExit("unsupported compress method"); showHelpAndExit(4); return; }
        if (receiver == null || receiver.length() == 0) { showErrorAndExit("missing <receiver> parameter"); showHelpAndExit(5); return; };
        InetSocketAddress receiverAddress = null;
        try
        {
            String port = receiver.replaceAll("^([\\s\\S]+?)(:(\\d+))?$", "$3");
            if (port.equals(receiver) || !port.matches("\\d+")) port = "9000";
            receiverAddress = new InetSocketAddress(receiver.replaceAll("^([\\s\\S]+?)(:\\d+)?$", "$1"), Integer.parseInt(port));
        }
        catch(Exception e)
        {
            e.printStackTrace();
            showErrorAndExit(7, "invalid receiver address");
            return;
        }

        LinkedList<String> srcFiles = new LinkedList<>();
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.charAt(0) != '-') srcFiles.add(arg);
        }
        if (srcFiles.size() == 0) { showErrorAndExit("missing source file(s) parameter"); showHelpAndExit(6); return; }

        // 启动压缩线程、传输线程
        new FileTransfer(receiverAddress).start();

        // TODO: 支持文件通配符
        FileTraverser.Callback fileSeeker = new FileTraverser.Callback()
        {
            @Override
            public void found(File file)
            {
                System.out.println("Found: " + file.getAbsolutePath());
            }
        };
        for (String filePath : srcFiles)
        {
            File file = new File(filePath);
            FileTraverser.traverse(file, fileSeeker);
        }
    }

    private static int getIntArgument(String[] args, String prefix, int defaultValue)
    {
        return Integer.parseInt(getArgument(args, prefix, String.valueOf(defaultValue)));
    }

    private static String getArgument(String[] args, String prefix, String defaultValue)
    {
        prefix = "--" + prefix + "=";
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith(prefix))
            {
                return arg.substring(prefix.length()).trim();
            }
        }
        return defaultValue;
    }

    private static void showErrorAndExit(String message)
    {
        System.err.println("Error: " + message);
    }

    private static void showErrorAndExit(int code, String message)
    {
        showErrorAndExit(message);
        System.exit(code);
    }

    private static void showHelpAndExit(int exitCode)
    {
        System.err.println();
        System.err.println("Usage: [OPTION]... <FILE>...");
        System.err.println("Compress and send file(s) to receiver");
        System.err.println("");
        System.err.println("    --level=级别       压缩级别(0 ~ 9), 0: 最快, 9: 最小但是最慢");
        System.err.println("    --threads=线程数   压缩线程数，默认为CPU核心数");
        System.err.println("    --bandwidth=带宽   带宽限制，单位可为k、m、g");
        System.err.println("    --method=方式      压缩方式，如zip或7zip");
        System.err.println("    --receiver=接收方  如：IP/域名:端口，端口默认为9000");
        System.err.println("");
        System.exit(exitCode);
    }
}
