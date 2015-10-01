package ex;

import javafx.concurrent.Task;

import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PageDownloader extends Task<DownloadResult> implements OnProgressListener
{
    private static final String rgx_ValidURL = "http://exhentai.org/s(/\\w+){2}-\\d+";
    private static final String rgx_Page = "\"img\" src=\"(http://\\S+)\"";
    private static final String rgx_PageFull = "\"(http://exhentai.org/fullimg.php\\S*?)\"";

    private static final String str_BandwidthExceeded = "http://exhentai.org/img/509.gif";
    private static final String str_InvalidURL = "PageDownloader found invalid URL: %s";
    private static final String str_ImageRegexFailed = "PageDownloader failed to match image for %s, info in %s";
    private static final String str_Downloading = "PageDownloader downloading file from: %s";
    private static final String str_Downloaded = "PageDownloader downloaded file from: %s";
    private static final String str_Skipped = "PageDownloader skipped existing file: %s";
    private static final String str_Download_Failed = "PageDownloader failed to download file from: %s";
    private static final String str_Download_Idle = "PageDownloader failed idle check, transferring %s bytes over %sms at %s%%";

    public int _currentAttempt;
    private String _pageURL;
    private String _path;
    private boolean _cancelled = false;

    private Timer downloadIdleTimer;
    private TimerTask downloadIdleCheck;

    private FileOutputStream _fos;
    private long _bytesTransferred;
    private double _percentage;

    public PageDownloader(String pageURL, String path)
    {
        _pageURL = pageURL;
        _path = path;
        validateURL();
    }

    private void validateURL()
    {
        if(!Util.isMatch(_pageURL, rgx_ValidURL))
        {
            Debug.Log(String.format(str_InvalidURL, _pageURL));
            _pageURL = null;
        }
    }

    public String getPageURL()
    {
        return _pageURL;
    }
    public String getFilePath()
    {
        return _path;
    }

    private String getImageLink() throws Exception
    {
        HTMLDownloader _pageDownloader = new HTMLDownloader(_pageURL);
        String _pageHTML = _pageDownloader.get();
        if(Util.getRegex(_pageHTML, str_BandwidthExceeded, 0).size() > 0)
        {
            return null;
        }
        ArrayList<String> _imageLink = Util.getRegex(_pageHTML,rgx_PageFull, 1);
        if(_imageLink.size() == 0) _imageLink =  Util.getRegex(_pageHTML,rgx_Page, 1);
        if(_imageLink.size() == 0)
        {
            Debug.File(Util.file_Err_PageDownloaderRegexFailed, _pageHTML);
            throw new Exception(String.format(str_ImageRegexFailed, _pageURL, Util.file_Err_PageDownloaderRegexFailed));
        }
        return _imageLink.get(0).replace("&amp;", "&");
    }

    private String getExtension(String url)
    {
        if(!url.contains("fullimg.php")) return url.substring(url.lastIndexOf(".") + 1);
        return "jpg";
    }

    private void cancelIdleCheck()
    {
        try{
            if(downloadIdleCheck != null) downloadIdleCheck.cancel();
        } catch (Exception ignored) { }
    }

    private void rescheduleIdleCheck()
    {
        cancelIdleCheck();
        downloadIdleCheck = new TimerTask()
        {
            @Override
            public void run()
            {
                failedIdleCheck();
            }
        };
        downloadIdleTimer.schedule(downloadIdleCheck, Util.TRANSFER_THRESHOLD_TIME*1000, Util.TRANSFER_THRESHOLD_TIME*1000);
    }

    private void failedIdleCheck()
    {
        cancelIdleCheck();
        _cancelled = true;
        Debug.Log(String.format(str_Download_Idle, _bytesTransferred, Util.TRANSFER_THRESHOLD_TIME, _percentage));
    }

    @Override
    public void onProgress(Object object, double percentage)
    {
        if(downloadIdleCheck != null)
        {
            _percentage = percentage;
            _bytesTransferred = (long) object;
            if(_bytesTransferred >= Util.TRANSFER_THRESHOLD_BYTES)
            {
                rescheduleIdleCheck();
            }
        }
        updateProgress(percentage, 100.0);
    }

    @Override
    protected DownloadResult call() throws Exception
    {
        if(Util.exFileExists(_path))
        {
            Debug.Log(String.format(str_Skipped, _path));
            return DownloadResult.SKIPPED;
        }
        if(_pageURL == null) return DownloadResult.FAILURE;
        try
        {
            _cancelled = false;
            _currentAttempt++;
            String _imageLink = getImageLink();
            if(_imageLink == null)
            {
                return DownloadResult.BANDWIDTH_EXCEEDED;
            }
            Debug.Log(String.format(str_Downloading, _imageLink));

            String _output = String.format("%s.%s", _path, getExtension(_imageLink));

            _fos = new FileOutputStream(_output);
            URLConnection connection = new URL(_imageLink).openConnection();
            connection.setRequestProperty("User-Agent", Util.USER_AGENT);
            connection.addRequestProperty("Cookie", Util.getCookie());
            ProgressAwareInputStream is = new ProgressAwareInputStream(connection.getInputStream(), connection.getContentLength());
            is.setOnProgressListener(this);

            downloadIdleTimer = new Timer(true);
            rescheduleIdleCheck();

            ReadableByteChannel c = Channels.newChannel(is);
            long transferred = 0L;

            while(transferred < connection.getContentLength())
            {
                if(_cancelled)
                    _fos.close();
                transferred += _fos.getChannel().transferFrom( c, transferred, 1 << 8 );
            }

            _fos.close();

            cancelIdleCheck();
            Debug.Log(String.format(str_Downloaded, _pageURL));
            return DownloadResult.SUCCESS;
        }
        catch (Exception e)
        {
            Debug.Log(String.format(str_Download_Failed, _pageURL));

            return DownloadResult.FAILURE;
        }
    }
}
