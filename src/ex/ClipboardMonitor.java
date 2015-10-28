package ex;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Timer;
import java.util.TimerTask;

public class ClipboardMonitor extends TimerTask
{
    private Clipboard clipboard;
    private Timer clipboardPollTimer;
    private ClipboardCallback _callback;
    private String lastString = "";

    public ClipboardMonitor()
    {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboardPollTimer = new Timer();
        clipboardPollTimer.scheduleAtFixedRate(this, 0, 200);
    }

    @Override
    public void run()
    {
        Transferable t = clipboard.getContents(null);
        if(!t.isDataFlavorSupported(DataFlavor.stringFlavor)) return;
        try
        {
            String s = (String)t.getTransferData(DataFlavor.stringFlavor);
            if(!s.equals(lastString)) clipboardChanged(s);
        }
        catch(Exception ignored){}
    }

    private void clipboardChanged(String s)
    {
        lastString = s;
        if(_callback != null) _callback.ClipboardChanged(s);
    }

    public void setListener(ClipboardCallback callback)
    {
        _callback = callback;
    }

    public void endMonitoring()
    {
        clipboardPollTimer.cancel();
    }
}
