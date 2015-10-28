package ex;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Timer;
import java.util.TimerTask;

public class FileMonitor extends TimerTask
{
    private FileCallback _callback;
    private FileTime lastTime;
    private Path filePath;
    private Timer filePollTimer;

    public FileMonitor(String path)
    {
        filePath = Paths.get(path);
        lastTime = getFileTime();
        filePollTimer = new Timer();
        filePollTimer.scheduleAtFixedRate(this, 0, 500);
    }
    private FileTime getFileTime()
    {
        try
        {
            return Files.getLastModifiedTime(filePath);
        }catch (Exception ignored){}
        return null;
    }

    @Override
    public void run()
    {
        try
        {
            FileTime d = getFileTime();
            if(d == null) return;
            if(d.compareTo(lastTime) == 0) return;
            lastTime = d;
            if(_callback != null) _callback.FileUpdated(filePath);
        }catch (Exception ignored){}
    }

    public void setListener(FileCallback callback)
    {
        _callback = callback;
    }

    public void endMonitoring()
    {
        filePollTimer.cancel();
    }
}
