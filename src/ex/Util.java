package ex;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util
{
    public static final String res_cookieUser = "ipb_member_id";
    public static final String res_cookiePass = "ipb_pass_hash";

    public static final String fileConf = "conf.txt";
    public static final String fileList = "list.txt";
    public static final String fileLog = "log.txt";
    public static final String fileOutput = "ex";

    public static final String file_Err_PageDownloaderRegexFailed = "PageDownloaderRegexFailed.txt";
    public static final String file_Err_GalleryThumbFinderRegexFailed = "GalleryThumbFinderRegexFailed.txt";

    public static String COOKIE = "";
    public static String USER_AGENT = "";

    public static boolean LOGGING_ENABLED = false;
    private static int THREAD_DELAY = 1000;
    private static int THREAD_DELAY_R = 200;
    private static int THREAD_DELAY_H = 200;
    private static int THREAD_DELAY_H_R = 200;
    public static  int MAX_ATTEMPTS = 3;

    public static long TRANSFER_THRESHOLD_TIME = 60;
    public static long TRANSFER_THRESHOLD_BYTES = 1024;

    public static String validPath(String path)
    {
        String invalidChars = "\\/:*?\"<>|";
        for (int i = 0; i < invalidChars.length(); i++)
        {
            char c = invalidChars.charAt(i);
            path = path.replace(c, '_');
        }
        return path;
    }

    public static String getCookie()
    {
        return COOKIE;
    }
    public static void parseConfig()
    {
        try
        {
            if(!Files.exists(Paths.get(Util.fileOutput, Util.fileList)))
                Files.createFile(Paths.get(Util.fileOutput, Util.fileList));
            //Load up user config.
            Properties settings = new Properties();
            InputStream settingsStream = new FileInputStream(Paths.get(fileOutput, fileConf).toString());
            settings.load(settingsStream);
            MAX_ATTEMPTS = Integer.parseInt(settings.getProperty("MAX_ATTEMPTS", "3"));
            LOGGING_ENABLED = Integer.parseInt(settings.getProperty("LOGGING_ENABLED", "0")) == 1;
            THREAD_DELAY = Integer.parseInt(settings.getProperty("THREAD_DELAY", "1000"));
            THREAD_DELAY_R = Integer.parseInt(settings.getProperty("THREAD_DELAY_R", "200"));
            THREAD_DELAY_H = Integer.parseInt(settings.getProperty("THREAD_DELAY_H", "200"));
            THREAD_DELAY_H_R = Integer.parseInt(settings.getProperty("THREAD_DELAY_H_R", "200"));
            TRANSFER_THRESHOLD_TIME = Long.parseLong(settings.getProperty("TRANSFER_THRESHOLD_TIME", "60"));
            TRANSFER_THRESHOLD_BYTES =  Long.parseLong(settings.getProperty("TRANSFER_THRESHOLD_BYTES", "1024"));
            USER_AGENT = settings.getProperty("USER_AGENT", "");
            COOKIE = String.format("%s=%s;%s=%s",Util.res_cookieUser, settings.getProperty(Util.res_cookieUser),Util.res_cookiePass,settings.getProperty(Util.res_cookiePass));
            settingsStream.close();

            //Configure URL connections to retain session state and install cookies.
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        }
        catch (Exception e)
        {
        	Debug.Log(String.format("Error reading %s", fileConf));
        }
    }
    public static void makeDir(String path)
    {
        new File(path).mkdir();
    }
    public static List<String> getLines(String path)
    {
        try
        {
            return Files.readAllLines(Paths.get(path));
        }
        catch (IOException e)
        {
            Debug.Log(String.format("Error reading %s", fileList));
        }
        return new ArrayList<>();
    }
    public static void writeLines(String path, List<String> contents)
    {
        try
        {
            Files.write(Paths.get(path), contents);
        }
        catch (IOException e)
        {
            Debug.Log(String.format("Error reading %s", path));
        }
    }
    public static Boolean isMatch(String input, String pattern)
    {
        Matcher _matcher = Pattern.compile(pattern).matcher(input);
        return _matcher.matches();
    }
    public static void launchFile(String path)
    {
        try
        {
            Desktop.getDesktop().open(new File(path));
        }
        catch (IOException e)
        {
            Debug.Log(String.format("Error opening %s", Util.fileConf));
        }
    }
    public static ArrayList<String> getRegex(String input, String pattern, int group)
    {
        ArrayList<String> _regexMatches = new ArrayList<>();
        Matcher _matcher = Pattern.compile(pattern).matcher(input);
        while (_matcher.find()) _regexMatches.add(_matcher.group(group));
        return _regexMatches;
    }
    public static int getDelay()
    {
        return THREAD_DELAY + (int) (Math.random() * THREAD_DELAY_R);
    }
    public static int getDelay_H()
    {
        return THREAD_DELAY_H + (int) (Math.random() * THREAD_DELAY_H_R);
    }

    public static void removeFile(String path)
    {
        try
        {
            Files.deleteIfExists(Paths.get(path));
        }
        catch (IOException e)
        {
            Debug.Log(String.format("Error deleting %s", path));
        }
    }

    public static Boolean fileExists(String path)
    {
        boolean b = Files.exists(Paths.get(path));
        return b;
    }
}
