package ex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class Debug
{
    private static BufferedWriter _writer;

    public static void create()
    {
        if(!Util.LOGGING_ENABLED) return;
        try
        {
            _writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Paths.get(Util.fileOutput, Util.fileLog).toString()), StandardCharsets.UTF_8));
        }
        catch(Exception e)
        {
            System.out.println("Error in Log create");
        }
    }
    public static void InsertBlank()
    {
        if(!Util.LOGGING_ENABLED) return;
        try
        {
            _writer.write(String.format("%n"));
        }
        catch(Exception e)
        {
            System.out.println("Error in Log InsertBlank");
        }
    }
    public static void Log(String msg)
    {
        if(!Util.LOGGING_ENABLED) return;
        LocalDateTime ldt = LocalDateTime.now();
        try
        {
            String _msg = String.format("%s %s%n", String.format("[%02d:%02d:%02d.%03d]", ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1000000), msg);
            _writer.write(_msg);
            _writer.flush();
        }
        catch(Exception e)
        {
            System.out.println("Error in Log Log");
            File("Logging_Error.txt", msg);
        }
    }
    public static void close()
    {
        if(!Util.LOGGING_ENABLED) return;
        try
        {
            _writer.close();
        }
        catch(Exception e)
        {
            System.out.println("Error in Log close");
        }
    }
    public static void File(String path, String msg)
    {
        try
        {
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
            w.write(msg);
            w.close();
        }
        catch(IOException ignored)
        {

        }
    }
    public static void File(String path, byte[] msg)
    {
        try
        {
            FileOutputStream w = new FileOutputStream(new File(path));
            w.write(msg);
            w.close();
        }
        catch(IOException ignored)
        {

        }
    }
}