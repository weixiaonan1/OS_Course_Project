package MemoryManagement;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;


public class Controller {
    //动态分配
    @FXML
    TableView dynamicTable;
    @FXML
    TableColumn dynamicMemory;
    @FXML
    TableColumn dynamicUse;
    @FXML
    Label dynamicCurMission;
    @FXML
    Label dynamicCurFit;
    @FXML
    JFXButton firstFitButton;
    @FXML
    JFXButton bestFitButton;
    DynamicController dynamicController;
    //调页存储
    @FXML
    TableView pagingInstruction;
    @FXML
    TableColumn pagingId;
    @FXML
    TableColumn pagingAddress;
    @FXML
    TableColumn pagingPage;
    @FXML
    TableView pagingMem;
    @FXML
    TableColumn pagingMemId;
    @FXML
    TableColumn pagingMemPage;
    @FXML
    TableColumn pagingMemRange;
    @FXML
    Label pagingCurSelect;
    @FXML
    Label pagingMissingNum;
    @FXML
    Label pagingMissingPercent;
    @FXML
    JFXButton FIFOButton;
    @FXML
    JFXButton LRUButton;
    @FXML
    JFXButton OPTButton;
    PagingController pagingController;


    public void init(){
        initDynamic();
        initPaging();
    }

    private void initDynamic(){
        dynamicController = new DynamicController(dynamicTable,dynamicMemory,dynamicUse,dynamicCurMission,dynamicCurFit);
        Thread dynamicThread = new Thread(dynamicController);
        dynamicThread.start();
        dynamicCurFit.setText("请选择算法");
        firstFitButton.setOnAction(event -> {
            dynamicController.start(DynamicController.FIRST_FIT);
            dynamicCurFit.setText("首次适应算法");
        });
        bestFitButton.setOnAction(event -> {
            dynamicController.start(DynamicController.BEST_FIT);
            dynamicCurFit.setText("最佳适应算法");
        });

    }

    private void initPaging() {
        pagingController = new PagingController( pagingInstruction, pagingId, pagingAddress, pagingPage, pagingMem, pagingMemId,pagingMemPage,pagingMemRange, pagingMissingNum, pagingMissingPercent);
        Thread pagingThread = new Thread(pagingController);
        pagingThread.start();
        pagingCurSelect.setText("请选择算法");
        FIFOButton.setOnAction(event -> {
            pagingController.start(PagingController.FIFO);
            pagingCurSelect.setText("FIFO算法");
        });
        LRUButton.setOnAction(event -> {
            pagingController.start(PagingController.LRU);
            pagingCurSelect.setText("最近最少使用算法");
        });
        OPTButton.setOnAction(event -> {
            pagingController.start(PagingController.OPT);
            pagingCurSelect.setText("最优置换算法");
        });
    }

}
