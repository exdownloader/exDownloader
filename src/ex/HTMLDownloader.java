package ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTMLDownloader
{
    private static final String str_Grabbing = "HTMLDownloader Grabbing HTML from %s.";
    private static final String str_Grabbed = "HTMLDownloader Grabbed HTML from %s.";
    private static final String str_Failed = "HTMLDownloader failed to grab HTML from %s.";


    private String _url;

    public int responseCode = -1;
    public HTMLDownloader(String url)
    {
        _url = url;
    }

    private String requestHTML(String url) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection)(new URL(url).openConnection());
        connection.setRequestProperty("User-Agent", Util.USER_AGENT);
        connection.addRequestProperty("Cookie", Util.getCookie());

        responseCode = connection.getResponseCode();
        return readString(connection.getInputStream());
    }
    private static String readString(InputStream is) throws IOException
    {
        // Borrowed from:
        // http://stackoverflow.com/questions/1891606/read-text-from-inputstream
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true)
        {
            int n = r.read(buf);
            if (n == -1) break;
            s.append(buf, 0, n);
        }
        return s.toString();
    }

    public String get()
    {
        try
        {
            Debug.Log(String.format(str_Grabbing, _url));
            String _html = requestHTML(_url);
            Debug.Log(String.format(str_Grabbed, _url));
            return _html;
        }
        catch (IOException e)
        {
            Debug.Log(String.format(str_Failed, _url));
        }
        return null;
    }
}