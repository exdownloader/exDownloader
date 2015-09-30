package ex;

import java.util.ArrayList;

public class ExInfo
{
    private final String str_HomeURL = "http://g.e-hentai.org/home.php";

    private final String rgx_Current = "currently at <strong.*?>(\\d+)";
    private final String rgx_Limit = "limit of <strong.*?>(\\d+)";
    private final String rgx_RecoverRate = "rate of <strong.*?>(\\d+)";

    public int limitCurrent = -1;
    public int limitMax = -1;
    public int limitRecoverRate = -1;

    public ExInfo()
    {

    }
    public void populateInfo()
    {
        HTMLDownloader _hd = new HTMLDownloader(str_HomeURL);
        String html = _hd.get();
        ArrayList<String> lstCurrent = Util.getRegex(html, rgx_Current, 1);
        ArrayList<String> lstLimit = Util.getRegex(html, rgx_Limit, 1);
        ArrayList<String> lstRecover = Util.getRegex(html, rgx_RecoverRate, 1);
        if(lstCurrent.size() > 0) limitCurrent = Integer.parseInt(lstCurrent.get(0));
        if(lstLimit.size() > 0) limitMax = Integer.parseInt(lstLimit.get(0));
        if(lstRecover.size() > 0) limitRecoverRate = Integer.parseInt(lstRecover.get(0));
    }
}
