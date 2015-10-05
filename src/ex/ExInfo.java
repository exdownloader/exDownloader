package ex;

import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExInfo extends Task<String>
{
    private final String str_Prefix = "ExInfo: %s";
    private final String str_Fetching = "fetching...";
    private final String str_Error = "error fetching.";
    private final String str_Format = "%s/%s, recovery/min = %s.";

    private final String str_HomeURL = "http://g.e-hentai.org/home.php";

    private final String rgx_Current = "currently at <strong.*?>(\\d+)";
    private final String rgx_Limit = "limit of <strong.*?>(\\d+)";
    private final String rgx_RecoverRate = "rate of <strong.*?>(\\d+)";

    public int limitCurrent = -1;
    public int limitMax = -1;
    public int limitRecoverRate = -1;

    private AtomicBoolean _isBusy = new AtomicBoolean(false);

    public ExInfo()
    {
        setString("unknown, click to refresh.");
    }

    @Override
    protected String call() throws Exception
    {
        populateInfo();
        return messageProperty().get();
    }

    public void populateInfo()
    {
        if(_isBusy.get()) return;
        _isBusy.set(true);
        setString(str_Fetching);
        HTMLDownloader _hd = new HTMLDownloader(str_HomeURL);
        String html = _hd.get();
        if(html == null)
            setString(str_Error);
        else
        {
            ArrayList<String> lstCurrent = Util.getRegex(html, rgx_Current, 1);
            ArrayList<String> lstLimit = Util.getRegex(html, rgx_Limit, 1);
            ArrayList<String> lstRecover = Util.getRegex(html, rgx_RecoverRate, 1);
            if (lstCurrent.size() > 0) limitCurrent = Integer.parseInt(lstCurrent.get(0));
            if (lstLimit.size() > 0) limitMax = Integer.parseInt(lstLimit.get(0));
            if (lstRecover.size() > 0) limitRecoverRate = Integer.parseInt(lstRecover.get(0));
            setString(String.format(str_Format, limitCurrent, limitMax, limitRecoverRate));
            if(lstCurrent.size() == 0 || lstLimit.size() == 0 || lstRecover.size() == 0)
                setString(str_Error);
        }
        _isBusy.set(false);
    }

    private void setString(String msg)
    {
        updateMessage(String.format(str_Prefix, msg));
    }
}
