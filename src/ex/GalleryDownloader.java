package ex;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryDownloader extends Task<GalleryResult>
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
    private static final String rgx_ImageCount = "Showing (\\d+) - (\\d+) of (\\d+) images";
    private static final String rgx_GalleryName = "id=\"gn\">(.*?)<";

    private String _url;
    private String _html;
    private String _pt;
    private String _gid;
    private int _imageCount;
    private int _imagesPerPage = 20;
    private String _outFol;
    private ArrayList<Download> _downloads;
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
        _downloads = new ArrayList<>();

        _galleryName.setValue(_url);
    }

    private String galleryName()
    {
        String galleryName = Util.getRegex(_html, rgx_GalleryName, 1).get(0);
        galleryName = Util.escapeHTMLString(galleryName);
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
    private void populateImageCount()
    {
        _imageCount = 0;
        try
        {
            Matcher m = Pattern.compile(rgx_ImageCount).matcher(_html);
            if(! m.find()) throw new Exception("");

            int min = Integer.parseInt(Util.getRegex(_html, rgx_ImageCount, 1).get(0));
            int max = Integer.parseInt(Util.getRegex(_html, rgx_ImageCount, 2).get(0));
            _imagesPerPage = (max - min) + 1;
            _imageCount = Integer.parseInt(Util.getRegex(_html, rgx_ImageCount, 3).get(0));
        }
        catch (Exception e)
        {
            Debug.Log(String.format(str_Gallery_Count_Error, _url));
        }
    }
    private void populateGalleryPages()
    {
        _pageURLs = new ArrayList<>(_imageCount);
        float r = _imageCount / (float)_imagesPerPage;
        int _pageCount = (int)Math.ceil(r);
        Debug.Log(String.format(str_Gallery_Images_Pages, _imageCount, _pageCount));
        String _baseURL = String.format("http://exhentai.org/g/%s/%s/?p=", _pt, _gid);
        for(int i = 0; i < _pageCount; i++)
            _pageURLs.add(_baseURL + String.valueOf(i));
    }
    private void populateThumbs()
    {
        FileNameProvider _fnp = new FileNameProvider(_imageCount);
        setGalleryState("Thumbs");
        _downloads.clear();
        for(int i = 0; i < _pageURLs.size(); i++)
        {
            _progress.set(String.format("%s/%s", i+1, _pageURLs.size()));
            ArrayList<String> _thumbURLs = findThumbsOnPage(_pageURLs.get(i));
            for(String _thumbURL : _thumbURLs)
                _downloads.add(new Download(_thumbURL,Paths.get(_outFol, _fnp.getNextName()).toString()));
            try {
                Thread.sleep(Util.getDelay_H());
            } catch (InterruptedException e) {
                setGalleryState("INTERRUPT");
                Debug.Log("SLEEP INTERRUPT IN GALLERYDOWNLOADER!");
            }
        }
    }
    @Override
    protected GalleryResult call() throws Exception
    {
        isAlive = true;
        setGalleryState("Meta");
        _html = new HTMLDownloader(_url).get();

        _progress.set("Fetched HTML");

        char c = _html.charAt(0);
        if(c == 65533)
        {
            setGalleryState("NULL HTML");
            _isComplete.setValue(true);
            return GalleryResult.FAILURE;
        }

        _galleryName.setValue(galleryName());
        if(_html == null)
        {
            setGalleryState("NULL HTML");
            _isComplete.setValue(true);
            return GalleryResult.FAILURE;
        }
        if(isGalleryRemoved())
        {
            setGalleryState("REMOVED");
            _isComplete.setValue(true);
            Debug.Log(String.format(str_Gallery_Removed, _url));
            return GalleryResult.FAILURE;
        }

        _outFol = Paths.get(Util.fileOutput, Util.fileDownloads, _galleryName.get()).toString();
        Util.makeDir(_outFol);

        populateImageCount();
        if(_imageCount == 0) return GalleryResult.FAILURE;
        populateGalleryPages();
        populateThumbs();

        setGalleryState("Downloading");
        //_progress.set(String.format("%s/%s", _currentAttempt, _imageCount));

        int i;

        while((i = getNextTask()) != -1)
        {
            Download _d = _downloads.get(i);
            setGalleryState(String.format("Trying p%s", i+1));
            if(!isAlive)
            {
                setGalleryState("INTERRUPT");
                //_isComplete.setValue(true);
                return GalleryResult.FAILURE;
            }
            DownloadResult _dr;
            try
            {
                _dr = _d.call();
                _d._dr = _dr;
            }
            catch (Exception e)
            {
                Debug.Log(String.format(str_Page_Error, _d.getURL()));
                _dr = DownloadResult.FAILURE;
            }
            if(_dr == DownloadResult.BANDWIDTH_EXCEEDED)
            {
                setGalleryState("BANDWIDTH");
                _isComplete.setValue(true);
                return GalleryResult.BANDWIDTH_EXCEEDED;
            }
            else if(_dr == DownloadResult.SUCCESS)
            {
                Debug.Log(String.format(str_Page_Successful, i));
                _progress.set(String.format("%s/%s", i+1, _imageCount));
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
    public int getNextTask()
    {
        for(int i = 0; i < _downloads.size(); i++)
        {
            Download _pd = _downloads.get(i);
            if(_pd._dr == null) return i;
            else if(_pd._dr == DownloadResult.RETRYING)
            {
                if(_pd._currentAttempt < Util.MAX_ATTEMPTS)
                {
                    _downloads.set(i, _pd.retry());
                    return i;
                }
            }
        }
        return -1;
    }
    public StringProperty getTaskProgress()
    {
        return _progress;
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
