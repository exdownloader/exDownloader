package ex;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;

public class ExInfo
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

    public StringProperty _infoString = new SimpleStringProperty("");
    private boolean _isBusy = false;

    public ExInfo()
    {
        set("unknown, click to refresh.");
    }

    public void populateInfo()
    {
        if(_isBusy) return;
        _isBusy = true;
        set(str_Fetching);
        HTMLDownloader _hd = new HTMLDownloader(str_HomeURL);
        String html = _hd.get();
        if(html == null)
            set(str_Error);
        else
        {
            ArrayList<String> lstCurrent = Util.getRegex(html, rgx_Current, 1);
            ArrayList<String> lstLimit = Util.getRegex(html, rgx_Limit, 1);
            ArrayList<String> lstRecover = Util.getRegex(html, rgx_RecoverRate, 1);
            if (lstCurrent.size() > 0) limitCurrent = Integer.parseInt(lstCurrent.get(0));
            if (lstLimit.size() > 0) limitMax = Integer.parseInt(lstLimit.get(0));
            if (lstRecover.size() > 0) limitRecoverRate = Integer.parseInt(lstRecover.get(0));
            set(String.format(str_Format,limitCurrent, limitMax, limitRecoverRate));
            if(lstCurrent.size() == 0 || lstLimit.size() == 0 || lstRecover.size() == 0)
                set(str_Error);
        }
        _isBusy = false;
    }

    private void set(String msg)
    {
        _infoString.set(String.format(str_Prefix, msg));
    }
}
