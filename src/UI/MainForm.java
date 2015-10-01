package UI;

import ex.*;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.event.ActionEvent;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainForm implements Initializable
{
    @FXML private Button btn_Add;
    @FXML private Button btn_Remove;
    @FXML private Button btn_Start;
    @FXML private Button btn_Settings;
    @FXML private TableView <GalleryDownloader> tree_Tasks;

    @FXML private SVGPath gfx_Stop;
    @FXML private SVGPath gfx_Start;
    @FXML private Hyperlink lbl_ExInfo;

    private static final String rgx_ID = "^http://exhentai.org/g/(\\w+)/(\\w+)/?$";

    private final String str_UI_Control_Clicked = "Control clicked: %s";
    private final String str_UI_TT_AddTask = "Add Task";
    private final String str_UI_TT_RemoveTask = "Remove Task";
    private final String str_UI_TT_StartTasks = "Start Tasks";
    private final String str_UI_TT_Settings = "Settings";

    private final String str_UI_TC_Name = "Task";
    private final String str_UI_TC_State = "State";
    private final String str_UI_TC_Progress = "/";
    private final String str_UI_TC_Done = "?";

    private TaskHandler _th = new TaskHandler();

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources)
    {
        btn_Add.setTooltip(new Tooltip(str_UI_TT_AddTask));
        btn_Remove.setTooltip(new Tooltip(str_UI_TT_RemoveTask));
        btn_Start.setTooltip(new Tooltip(str_UI_TT_StartTasks));
        btn_Settings.setTooltip(new Tooltip(str_UI_TT_Settings));

        TableColumn<GalleryDownloader, String> nameColumn = new TableColumn<>( str_UI_TC_Name );
        TableColumn<GalleryDownloader, String> stateColumn = new TableColumn<>( str_UI_TC_State );
        TableColumn<GalleryDownloader, String> progressColumn = new TableColumn<>( str_UI_TC_Progress );
        TableColumn<GalleryDownloader, Boolean> doneColumn = new TableColumn<>( str_UI_TC_Done );

        nameColumn.setSortable(false);
        stateColumn.setSortable(false);
        progressColumn.setSortable(false);
        doneColumn.setSortable(false);

        nameColumn.setPrefWidth(300);

        stateColumn.setMinWidth(150);
        stateColumn.setMaxWidth(150);

        progressColumn.setMinWidth(100);
        progressColumn.setMaxWidth(100);

        doneColumn.setMinWidth(30);
        doneColumn.setMaxWidth(30);

        doneColumn.setCellFactory(tc -> {
            CheckBoxTableCell _tc = new CheckBoxTableCell();
            _tc.setAlignment(Pos.CENTER);
            return _tc;
        });

        nameColumn.setCellValueFactory(param -> param.getValue().getGalleryName());
        stateColumn.setCellValueFactory(param -> param.getValue().getGalleryState());
        progressColumn.setCellValueFactory(param -> param.getValue().getTaskProgress());
        doneColumn.setCellValueFactory(f -> f.getValue().getIsComplete());


        tree_Tasks.getColumns().setAll(nameColumn, stateColumn, progressColumn, doneColumn);
        tree_Tasks.setPlaceholder(new Label("No tasks."));

        ObservableList<GalleryDownloader> _list = tree_Tasks.getItems();

        _th.getTasks().addListener((Observable observable) -> {
            _list.clear();
            ArrayList<String> _galleryList = new ArrayList<>();
            _th.getTasks().stream().forEach((gd) -> {
                _galleryList.add(gd.getURL());
                if (!gd.getIsComplete().getValue())
                    _list.add(gd);
            });
            Util.writeLines(Paths.get(Util.fileOutput, Util.fileList).toString(), _galleryList);
        });

        gfx_Start.visibleProperty().bind(_th.isBusy.not());
        gfx_Stop.visibleProperty().bind(_th.isBusy);
        lbl_ExInfo.textProperty().bind(_th.exInfo._infoString);

        Debug.Log("Initialised.");
        Debug.InsertBlank();

        parseList();
    }

    public void controlClicked(ActionEvent event)
    {
        Control source = (Control) event.getSource();
        String id = source.getId();
        Debug.Log(String.format(str_UI_Control_Clicked, id));

        if(source == btn_Start)
        {
            if(!_th.isBusy.get())
            {
                _th.Start();
            }
            else
            {
                _th.Stop();
            }
        }
        else if(source == btn_Add)
        {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("");
            dialog.setHeaderText("Enter gallery URL");
            dialog.setContentText("URL:");
            dialog.setResizable(false);
            dialog.getDialogPane().setPrefHeight(200);
            dialog.getDialogPane().setPrefWidth(350);

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(_url -> {
                if(!addTask(_url))
                {
                    Alert(Alert.AlertType.ERROR, "Error", "Error with gallery URL", String.format("URL: %s%nDoes not match form %s", _url,rgx_ID));
                }
            });
        }
        else if(source == btn_Remove)
        {
            if(_th.isBusy.get()) return;
            _th.removeTask(tree_Tasks.getSelectionModel().getSelectedItem());
        }
        else if(source == btn_Settings)
        {
            Util.launchFile(Paths.get(Util.fileOutput, Util.fileConf).toString());
        }
        else if(source == lbl_ExInfo)
        {
            _th.exInfo.populateInfo();
        }
    }
    public void Terminate()
    {
        Debug.Log("Stopping TaskHandler.");
        if(_th.isBusy.get()) _th.Stop();
    }
    private void parseList()
    {
        List<String> _urls = Util.getLines(Paths.get(Util.fileOutput, Util.fileList).toString());
        for(String _url : _urls)
            addTask(_url);
    }
    private boolean addTask(String url)
    {
        boolean isMatch = Util.isMatch(url, rgx_ID);
        if(isMatch)
            _th.addTask(new GalleryDownloader(url));
        return isMatch;
    }
    public static void Alert(Alert.AlertType alertType, String title, String header, String content)
    {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}