package MemoryManagement;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.DecimalFormat;
import java.util.*;

public class PagingController implements Runnable{
    public static final int WAIT = 0;
    public static final int FIFO = 1;
    public static final int LRU = 2;
    public static final int OPT = 3;

    private TableView pagingInstruction;
    private TableColumn pagingId;
    private TableColumn pagingAddress;
    private TableColumn pagingPage;
    private TableView pagingMem;
    private TableColumn pagingMemId;
    private TableColumn pagingMemPage;
    private TableColumn pagingMemRange;
    private Label pagingMissingNum;
    private Label pagingMissingPercent;

    private int status;
    private int curIndex;
    private int curMissingNum = 0;
    private List<Instruction> instructions = new ArrayList<>();
    private List<Integer> LRURecords = new ArrayList<>();
    private ObservableList<Instruction> instructionObservableList = FXCollections.observableArrayList();
    private ObservableList<Block> blockObservableList = FXCollections.observableArrayList();

    public PagingController(TableView pagingInstruction, TableColumn pagingId, TableColumn pagingAddress, TableColumn pagingPage, TableView pagingMem, TableColumn pagingMemId,TableColumn pagingMemPage, TableColumn pagingMemRange, Label pagingMissingNum, Label pagingMissingPercent) {
        this.pagingInstruction = pagingInstruction;
        this.pagingId = pagingId;
        this.pagingAddress = pagingAddress;
        this.pagingPage = pagingPage;
        this.pagingMem = pagingMem;
        this.pagingMemId = pagingMemId;
        this.pagingMemPage = pagingMemPage;
        this.pagingMemRange = pagingMemRange;
        this.pagingMissingNum = pagingMissingNum;
        this.pagingMissingPercent = pagingMissingPercent;
        this.status = WAIT;
        this.curIndex = 0;
        pagingInstruction.setItems(instructionObservableList);
        pagingId.setCellValueFactory(new PropertyValueFactory("id"));
        pagingAddress.setCellValueFactory(new PropertyValueFactory("address"));
        pagingPage.setCellValueFactory(new PropertyValueFactory("page"));
        pagingMem.setItems(blockObservableList);
        pagingMemId.setCellValueFactory(new PropertyValueFactory("id"));
        pagingMemPage.setCellValueFactory(new PropertyValueFactory("page"));
        pagingMemRange.setCellValueFactory(new PropertyValueFactory("range"));
        initInstructions();
    }

    public void start(int mode){
        status = mode;
        curIndex = 0;
        curMissingNum = 0;
        pagingInstruction.getItems().clear();
        pagingMem.getItems().clear();
    }

    private void initInstructions() {
        int count = 0;
        Random random = new Random();
        while(count < 320){
            int address = random.nextInt(320);
            addInstructionTwice(count,address);
            count += 2;
            //防止nextInt报错
            address = address == 0? 1 : address;
            address = random.nextInt(address);
            addInstructionTwice(count,address);
            count += 2;
            //防止超过上界
            int bound = address + 2 > 320? 320 : address + 2;
            address = random.nextInt(320 - bound)+ bound;
            addInstructionTwice(count, address);
            count+=2;
        }

    }

    private void addInstructionTwice(int id, int address) {
        instructions.add(new Instruction(id, address));
        id += 1;
        address = (address + 1) > 320 ? address : (address + 1);
        instructions.add(new Instruction(id, address));
    }

    @Override
    public void run() {
        while(true){
            if(status != WAIT){
                allocate();
            }
            sleep(500);
        }

    }

    private void allocate() {
        //指令执行完毕
        if(curIndex == instructions.size()){
            status = WAIT;
            return;
        }
        //更新当前指令和下一条指令
        Instruction curInstruction = instructions.get(curIndex);
        System.out.println(curInstruction.toString());
        pagingInstruction.getItems().clear();
        instructionObservableList.add(curInstruction);
        if(curIndex + 1 < instructions.size()){
            instructionObservableList.add(instructions.get(curIndex + 1));
        }
        //如果指令所在页已经存在于内存，则更新LRU表
        if(pageInMem(curInstruction.getPage())){
            System.out.println("exists");
            LRURecords.remove(Integer.valueOf(curInstruction.getPage()));
            LRURecords.add(0,curInstruction.getPage());
        }
        //如果不在，则判断内存是否满
        else {
            curMissingNum++;
            if(blockObservableList.size() < 4){
                LRURecords.add(0,curInstruction.getPage());
                blockObservableList.add(new Block(blockObservableList.size(),curInstruction.getPage()));
            }else{
                //根据不同算法，找到要替换的块
                int replaceIndex = -1;
                if(status == FIFO){
                    replaceIndex = runFIFO();
                }else if(status == LRU){
                    replaceIndex = runLRU();
                }else if(status == OPT){
                    replaceIndex = runOPT();
                }
                LRURecords.remove(Integer.valueOf(blockObservableList.get(replaceIndex).getPage()));
                blockObservableList.remove(replaceIndex);
                //更新其他块的id
                for(int i = 0;i<blockObservableList.size();i++){
                    blockObservableList.get(i).setId(i);
                }
                blockObservableList.add(new Block(blockObservableList.size(),curInstruction.getPage()));
                LRURecords.add(0,curInstruction.getPage());
            }
        }
        //更新缺页数和缺页率
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                pagingMissingNum.setText(String.valueOf(curMissingNum));
                double percent = (double)curMissingNum / (curIndex + 1);
                DecimalFormat df = new DecimalFormat("#.0");
                pagingMissingPercent.setText(df.format(percent * 100) + "%");
            }
        });
        curIndex++;

    }

    private int runLRU() {
        for(int i = 0;i<blockObservableList.size();i++){
            if(blockObservableList.get(i).getPage() == LRURecords.get(3)){
                return i;
            }
        }
        return -1;
    }

    private int runFIFO() {
        return 0;
    }

    private int runOPT(){
        Map<Integer,Integer> pageToIndex = new HashMap<>();
        TreeSet<Integer> records = new TreeSet<>();
        for(int i = 0;i<blockObservableList.size();i++){
            pageToIndex.put(blockObservableList.get(i).getPage(),i);
            records.add(blockObservableList.get(i).getPage());
        }
        for(int i = curIndex + 1; i < instructions.size();i++){
            int page = instructions.get(i).getPage();
            records.remove(page);
            if(records.size() == 1){
                break;
            }
        }
        return pageToIndex.get(records.last());
    }

    private boolean pageInMem(int page){
        for(Block block: blockObservableList){
            if(block.getPage() == page){
                return true;
            }
        }
        return false;
    }
    private void sleep(int mills){
        try {
            Thread.sleep( mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
