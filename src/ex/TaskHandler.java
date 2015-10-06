package ex;

import UI.MainForm;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.util.concurrent.atomic.AtomicBoolean;

public class TaskHandler
{
    private final String str_ExInfoLimit = "ExInfo: Current = %d, Limit = %d, Recover/Min = %d";
    private final String str_ExInfoOverLimit = "ExInfo: Over limit, terminating.";

    private ObservableList<GalleryDownloader> _tasks = FXCollections.observableArrayList();
    private Thread _taskThread;
    private GalleryDownloader _currentTask;
    private AtomicBoolean isStopping = new AtomicBoolean(false);
    public BooleanProperty isBusy = new SimpleBooleanProperty(false);
    public ExInfo exInfo = new ExInfo();

    public TaskHandler()
    {
    }
    public void addTask(GalleryDownloader gd)
    {
        _tasks.add(gd);
    }
    public void removeTask(GalleryDownloader gd)
    {
        _tasks.remove(gd);
    }
    public ObservableList<GalleryDownloader> getTasks()
    {
        return _tasks;
    }


    public void Start()
    {
        Debug.Log("TaskHandler: Start invoked.");
        if(isBusy.getValue()) return;
        if(_tasks.size() == 0) return;
        isBusy.set(true);
        isStopping.set(false);

        exInfo.populateInfo();
        String _strExInfo = String.format(str_ExInfoLimit, exInfo.limitCurrent, exInfo.limitMax, exInfo.limitRecoverRate);
        Debug.Log(_strExInfo);

        if(exInfo.limitCurrent < exInfo.limitMax)
        {
            _taskThread = new Thread(this::Download);
            _taskThread.start();
        }
        else
        {
            Platform.runLater(() -> MainForm.Alert(Alert.AlertType.ERROR, "Limit exceeded!", str_ExInfoOverLimit, _strExInfo));
            Debug.Log(str_ExInfoOverLimit);
            isBusy.set(false);
        }

    }
    public void Stop()
    {
        Debug.Log("TaskHandler: Stop invoked.");
        if(isStopping.get()) return;

        try {
            if(_currentTask != null)
                _currentTask.isAlive = false;
            isBusy.set(false);
            isStopping.set(true);
        } catch (Exception e)
        {
            Debug.Log("ERROR STOPPING TASKHANDLER!");
        }
    }
    private void Download()
    {
        while(!isStopping.get())
            DownloadNext();
    }
    private void DownloadNext()
    {
        Debug.Log("TaskHandler: DownloadNext invoked.");
        if(isStopping.get()) return;
        try
        {
            _currentTask = null;
            for(GalleryDownloader _gd : _tasks)
                if(!_gd.getIsComplete().get())
                {
                    _currentTask = _gd;
                    break;
                }
            if(_currentTask == null)
            {
                isBusy.set(false);
            }
            else
            {
                GalleryResult _gr = _currentTask.call();
                if(_gr == GalleryResult.BANDWIDTH_EXCEEDED)
                {
                    Stop();
                    _currentTask = null;
                }
            }
        }catch(Exception ignored){}
        if(_currentTask == null)
        {
            Debug.InsertBlank();
            Debug.Log("Tasks complete!");
            Stop();
        }
    }
}
