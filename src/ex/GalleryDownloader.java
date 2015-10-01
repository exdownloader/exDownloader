package ex;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryDownloader
{
    private static final String str_Gallery_Images_Pages = "GalleryDownloader found %s image(s) across %s page(s).";
    private static final String str_Gallery_Removed = "GalleryDownloader found an empty gallery at %s";
    private static final String str_Page_Error = "GalleryDownloader failed to download page %s";
    private static final String str_Gallery_Thumb_Error = "GalleryDownloader failed to request HTML for %s";
    private static final String str_Gallery_Count_Error = "GalleryDownloader failed to match image count for %s";
    private static final String str_Page_Successful = "GalleryDownloader successfully downloaded page %s";
    private static final String str_Gallery_Successful = "GalleryDownloader successfully downloaded gallery.";
    private static final String str_Retry_Exceeded = "GalleryDownloader exceeded retry limit for page %s";

    private static final String rgx_ID = "(http://)?exhentai.org/g/(\\w+)/(\\w+)/?";
    private static final String rgx_GalleryRemoved = "<title>GalleryDownloader Not Available";
    private static final String rgx_Gallery = "http://exhentai.org/g/(\\w+/){2}\\?p=(\\d+)";
    private static final String rgx_ImageCount = "Showing \\d+ - \\d+ of (\\d+) images";
    private static final String rgx_GalleryName = "id=\"gn\">(.*?)<";

    private String _url;
    private String _html;
    private String _pt;
    private String _gid;
    private int _imageCount;
    private String _outFol;
    private ArrayList<PageDownloader> _pages;
    private ArrayList<String> _pageURLs;

    private StringProperty _galleryName = new SimpleStringProperty("");
    private StringProperty _galleryState = new SimpleStringProperty("");
    private BooleanProperty _isComplete = new SimpleBooleanProperty(false);
    private StringProperty _progress = new SimpleStringProperty("Idle");

    public Boolean isAlive = true;

    public GalleryDownloader(String url)
    {
        Matcher m = Pattern.compile(rgx_ID).matcher(url);
        if(!m.find()) return;
        _pt = m.group(2);
        _gid = m.group(3);

        _url = url;
        _pages = new ArrayList<>();
        _pageURLs = new ArrayList<>();

        _galleryName.setValue(_url);
    }

    private String galleryName()
    {
        String galleryName = Util.getRegex(_html, rgx_GalleryName, 1).get(0);
        galleryName = galleryName.replaceAll("\\(.*?\\)", "");
        while (galleryName.contains("  "))
            galleryName = galleryName.replaceAll("  ", " ");
        Matcher m1 = Pattern.compile("\\[ *(.+?) *\\]").matcher(galleryName);
        while (m1.find()) galleryName = galleryName.replace(m1.group(0), String.format("[%s]", m1.group(1)));
        galleryName = galleryName.trim();
        return Util.validPath(galleryName);
    }

    private boolean isGalleryRemoved()
    {
        ArrayList<String> _matches = Util.getRegex(_html, rgx_GalleryRemoved, 0);
        return _matches.size() > 0;
    }

    private int findPageCount()
    {
        ArrayList<String> pageNumbers = Util.getRegex(_html, rgx_Gallery, 2);
        int maxPages = 0;
        for(String p : pageNumbers)
        {
            int parsedMax = Integer.parseInt(p);
            if(parsedMax > maxPages)
                maxPages = parsedMax;
        }
        return maxPages;
    }

    private ArrayList<String> findThumbsOnPage(String _pageURL)
    {
        String _pageHTML = new HTMLDownloader(_pageURL).get();
        if(_pageHTML == null)
        {
            Debug.Log(String.format(str_Gallery_Thumb_Error, _pageURL));
            return null;
        }

        GalleryThumbFinder _gtf = new GalleryThumbFinder(_pageHTML);
        _gtf.Process();
        return _gtf.thumbURLs();
    }

    public GalleryResult Download() throws Exception
    {
        setGalleryState("Meta");
        _html = new HTMLDownloader(_url).get();

        _progress.set("Fetched HTML");

        char c = _html.charAt(0);
        if(c == 65533)
        {
            Fail("BAD COOKIE");
            return GalleryResult.FAILURE;
        }

        _galleryName.setValue(galleryName());
        if(_html == null)
        {
            Fail("NULL HTML");
            return GalleryResult.FAILURE;
        }
        if(isGalleryRemoved())
        {
            Fail("REMOVED");
            Debug.Log(String.format(str_Gallery_Removed, _url));
            return GalleryResult.FAILURE;
        }

        _outFol = Paths.get(Util.fileOutput, Util.fileDownloads, _galleryName.get()).toString();
        Util.makeDir(_outFol);

        try
        {
            _imageCount = Integer.parseInt(Util.getRegex(_html, rgx_ImageCount, 1).get(0));
        }
        catch (Exception e)
        {
            Debug.Log(String.format(str_Gallery_Count_Error, _url));
            return GalleryResult.FAILURE;
        }
        if(_imageCount == 0)
        {
            Fail("COUNT ERROR");
            return GalleryResult.FAILURE;
        }
        int _pageCount = findPageCount();

        Debug.Log(String.format(str_Gallery_Images_Pages, _imageCount, _pageCount + 1));

        String _baseURL = String.format("http://exhentai.org/g/%s/%s/?p=", _pt, _gid);
        for(int i = 0; i <= _pageCount; i++)
            _pageURLs.add(_baseURL + String.valueOf(i));
        FileNameProvider _fnp = new FileNameProvider(_imageCount);
        setGalleryState("Thumbs");
        for(int i = 0; i < _pageURLs.size(); i++)
        {
            _progress.set(String.format("%s/%s", i+1, _pageURLs.size()));
            ArrayList<String> _thumbURLs = findThumbsOnPage(_pageURLs.get(i));
            for(String _thumbURL : _thumbURLs)
                _pages.add(new PageDownloader(_thumbURL,Paths.get(_outFol, _fnp.getNextName()).toString()));
            try {
                Thread.sleep(Util.getDelay_H());
            } catch (InterruptedException e) {
                setGalleryState("INTERRUPT");
                Debug.Log("SLEEP INTERRUPT IN GALLERYDOWNLOADER!");
            }
        }
        setGalleryState("Downloading");
        int _currentAttempt = 0;
        int _successfulAttempts = 0;
        _progress.set(String.format("%s/%s", _currentAttempt, _imageCount));
        while(_pages.size() > 0)
        {
            setGalleryState(String.format("Trying p%s", _currentAttempt+1));
            PageDownloader _pd = _pages.get(0);
            if(!isAlive)
            {
                Fail("INTERRUPT");
                return GalleryResult.FAILURE;
            }
            DownloadResult _dr;
            try
            {
                _dr = _pd.call();
            }
            catch (Exception e)
            {
                Debug.Log(String.format(str_Page_Error, _pd.getPageURL()));
                _dr = DownloadResult.FAILURE;
            }
            if(_dr == DownloadResult.BANDWIDTH_EXCEEDED)
            {
                Fail("BANDWIDTH");
                return GalleryResult.BANDWIDTH_EXCEEDED;
            }
            else if(_dr == DownloadResult.FAILURE)
            {
                if(_pd._currentAttempt >= Util.MAX_ATTEMPTS)
                {
                    setGalleryState("PAGE ERROR");
                    Debug.Log(String.format(str_Retry_Exceeded, _pd.getPageURL()));
                    _pages.remove(_pd);
                    _currentAttempt++;
                    //return GalleryResult.FAILURE;
                }
                Util.removeFile(_pd.getFilePath());
            }
            else
            {
                _currentAttempt++;
                _successfulAttempts++;
                _pages.remove(_pd);
                Debug.Log(String.format(str_Page_Successful, _currentAttempt));
                _progress.set(String.format("%s/%s", _successfulAttempts, _imageCount));
                try {
                    if(_dr != DownloadResult.SKIPPED)
                        Thread.sleep(Util.getDelay());
                } catch (InterruptedException e)
                {
                    setGalleryState("INTERRUPT");
                    Debug.Log("SLEEP INTERRUPT IN GALLERYDOWNLOADER!");
                }
            }
        }
        setGalleryState("Done");
        _isComplete.setValue(true);
        _progress.set(String.format("%s/%s", _imageCount, _imageCount));
        Debug.Log(str_Gallery_Successful);
        return GalleryResult.SUCCESS;
    }
    public StringProperty getTaskProgress()
    {
        return _progress;
    }
    private void Fail(String s)
    {
        setGalleryState(s);
        _isComplete.setValue(true);
    }
    private void setGalleryState(String s)
    {
        Debug.Log(String.format("GalleryDownloader state: %s", s));
        _galleryState.set(s);
    }

    public StringProperty getGalleryName() { return _galleryName; }
    public BooleanProperty getIsComplete() { return _isComplete; }
    public StringProperty getGalleryState() { return _galleryState; }

    public String getURL() {
        return _url;
    }
}
