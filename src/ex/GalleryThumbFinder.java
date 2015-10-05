package ex;

import java.util.ArrayList;

public class GalleryThumbFinder
{
    private static final String str_ThumbRegexFailed = "GalleryThumbFinder failed to match thumbs, info in %s";
    private final String rgx_Thumb = "<a href=\"(http://exhentai.org/s/.*?)\">";
    private String _html;
    private ArrayList<String> _thumbURLs;

    public GalleryThumbFinder(String html)
    {
        _html = html;
    }
    public void Process()
    {
        try
        {
            _thumbURLs = Util.getRegex(_html, rgx_Thumb, 1);
        }
        catch(Exception e)
        {
            Debug.File(Util.file_Err_GalleryThumbFinderRegexFailed, _html);
            Debug.Log((String.format(str_ThumbRegexFailed, Util.file_Err_GalleryThumbFinderRegexFailed)));
            Debug.Log("GalleryThumbFinder failed to fetch thumbs.");
        }
    }
    public ArrayList<String> thumbURLs()
    {
        return _thumbURLs;
    }
}
