package DS.Controller;


import DS.Basic.FileType;
import DS.Basic.Response;
import DS.Model.FCB;
import com.jfoenix.controls.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class Controller {
    private static SystemCore systemCore;
    private static final int NOT_SELECTED = -1;

    @FXML
    JFXDialog infoDialog;
    @FXML
    JFXButton fName;
    @FXML
    JFXButton fType;
    @FXML
    JFXButton fCreateDate;
    @FXML
    JFXButton fModifyDate;
    @FXML
    JFXButton fActualSize;
    @FXML
    JFXButton fOccupiedSize;
    @FXML
    JFXButton infoAcceptButton;

    //创建文件、创建文件夹复用
    @FXML
    JFXDialog makeDialog;
    @FXML
    Label makeDialogHead;
    @FXML
    JFXTextField makeDialogInput;
    @FXML
    JFXButton makeDialogCancelButton;
    @FXML
    JFXButton makeDialogOkButton;

    //创建文件、创建文件夹复用
    @FXML
    JFXDialog fileDialog;
    @FXML
    Label fileDialogName;
    @FXML
    JFXTextArea fileDialogContent;
    @FXML
    JFXButton fileDialogOkButton;

    @FXML
    StackPane root;
    @FXML
    JFXButton reset;
    @FXML
    JFXButton mkFile;
    @FXML
    JFXButton mkDir;
    @FXML
    JFXButton delete;
    @FXML
    JFXButton leave;
    @FXML
    JFXButton info;
    @FXML
    JFXButton exit;
    @FXML
    Label path;
    @FXML
    TableView table;
    @FXML
    TableColumn name;
    @FXML
    TableColumn date;
    @FXML
    TableColumn size;
    @FXML
    TableColumn type;

    private int selectedIndex = NOT_SELECTED;
    private ObservableList<FCB> children = FXCollections.observableArrayList();
    private String curPath = "root:";
    private Stage stage;

    public void init(){
        systemCore = new SystemCore();
        systemCore.print();
        path.setText(curPath);
        reset.setOnAction(action->{
            systemCore.format();
            systemCore.print();
            curPath = "root:";
            path.setText(curPath);
            resetTable();
        });
        mkFile.setOnAction(action->{
            makeDialog.show(root);
            make(FileType.FILE);
        });
        mkDir.setOnAction(action->{
            makeDialog.show(root);
            make(FileType.FOLDER);
        });

        delete.setOnAction(action->{
            if(children.get(selectedIndex).type == FileType.FILE){
                systemCore.deleteFile(children.get(selectedIndex).nodeId);
            }else{
                systemCore.deleteFolder(children.get(selectedIndex).nodeId);
            }
            systemCore.print();
            resetTable();
        });

        leave.setOnAction(action->{
            Response response = systemCore.leaveDir();
            systemCore.print();
            if(response.successful){
                resetTable();
                selectedIndex = NOT_SELECTED;
                filtering();
                curPath = curPath.substring(0,curPath.lastIndexOf("\\"));
                path.setText(curPath);
            }else{
                noticeDialog("失败",response.message);
            }

        });
        exit.setOnAction(action->{
            systemCore.exit();
            stage.close();
            systemCore.print();
        });
        info.setOnAction(action->{
            infoDialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
            infoDialog.show(root);
            showInfo();
        });

        stage = (Stage) root.getScene().getWindow();
        stage.setOnCloseRequest(event -> {

            systemCore.exit();
            systemCore.print();
        });
        resetTable();
        initTable();
        filtering();

        infoAcceptButton.setOnAction(action -> {
            infoDialog.close();
        });

        makeDialogCancelButton.setOnAction(action -> {
            makeDialog.close();
        });

        //避免show的时候多次添加
        root.getChildren().remove(infoDialog);
        root.getChildren().remove(makeDialog);
        root.getChildren().remove(fileDialog);
    }

    private void showInfo() {
        FCB temp = children.get(selectedIndex);
        fName.setText(temp.name);
        fType.setText(temp.type.name());
        fCreateDate.setText(temp.createDate.toString());
        fModifyDate.setText(temp.modifyDate.toString());
        fActualSize.setText(temp.actualSize + " Bytes");
        fOccupiedSize.setText(temp.occupiedSize + " Bytes");
    }

    private void make(FileType type){
        String ss = type == FileType.FILE? "文件" : "文件夹";
        makeDialogHead.setText("创建" + ss);
        makeDialogInput.clear();
        makeDialogInput.setPromptText("请输入" + ss +"名");
        makeDialogOkButton.setOnAction(action->{
            makeDialog.close();
            String name = makeDialogInput.getText().isEmpty()?"新建"+ss:makeDialogInput.getText();
            Response response = systemCore.create(name,type);
            if(response.successful){
                systemCore.print();
                resetTable();
            }else {
                noticeDialog("失败",response.message);
            }

        });
    }
    void resetTable(){
        children.clear();
        children.addAll(systemCore.childrenFCB);
    }

    void initTable(){
        name.setCellValueFactory(new PropertyValueFactory("name"));
        date.setCellValueFactory(new PropertyValueFactory("modifyDate"));
        size.setCellValueFactory(new PropertyValueFactory("actualSize"));
        type.setCellValueFactory(new PropertyValueFactory("type"));
        table.setItems(children);

        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                int index = table.getSelectionModel().getSelectedIndex();
                if (index < 0){
                    return;
                }
                System.out.println("selectId" + index + " fileName:"+children.get(index).name);
                selectedIndex = index;
                filtering();
            }
        });

        table.setRowFactory(tv -> {
            TableRow<FCB> row = new TableRow<FCB>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    FCB fcb = row.getItem();
                    if(fcb.type == FileType.FILE){
                        fileDialog.show(root);
                        openFile();
                    }else{
                        systemCore.entryDir(fcb.nodeId);
                        curPath= curPath + "\\" + fcb.name;
                        path.setText(curPath);
                        resetTable();
                        selectedIndex = NOT_SELECTED;
                        filtering();
                    }

                }
            });
            return row;
        });

    }

    private void openFile() {
        fileDialogName.setText( children.get(selectedIndex).name);
        String originContent = systemCore.readFile(children.get(selectedIndex).nodeId);
        if(originContent != null){
            fileDialogContent.setText(originContent);
        }else{
            fileDialogContent.setText("");
        }

        fileDialogOkButton.setOnAction(action->{
            String newContent = fileDialogContent.getText();
            if((originContent == null && newContent.isEmpty()) || newContent.equals(originContent)){
                fileDialog.close();
            }else {
                Response response =  systemCore.writeFile(children.get(selectedIndex).nodeId,newContent);
                fileDialog.close();
               if(response.successful){
                   resetTable();
               }else {
                   noticeDialog("保存失败",response.message);
               }
            }
        });

    }

    private void filtering() {
        System.out.println(selectedIndex);
        if (selectedIndex != NOT_SELECTED){
            delete.setDisable(false);
            info.setDisable(false);
            return;
        }
        delete.setDisable(true);
        info.setDisable(true);
    }


    private void noticeDialog(String head,String content){
        JFXAlert alert = new JFXAlert((Stage) root.getScene().getWindow());
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOverlayClose(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        JFXButton h = new JFXButton(head);
        layout.setHeading(h);
        layout.setBody(new Label(content));
        JFXButton closeButton = new JFXButton("确定");
        closeButton.setOnAction(event -> alert.hideWithAnimation());
        layout.setActions(closeButton);
        alert.setContent(layout);
        alert.show();
    }


}
