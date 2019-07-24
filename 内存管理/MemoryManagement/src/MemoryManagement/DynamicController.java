package MemoryManagement;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicController implements Runnable{
    public static final int WAIT = 0;
    public static final int FIRST_FIT = 1;
    public static final int BEST_FIT = 2;
    private TableView dynamicTable;
    private TableColumn dynamicMemory;
    private TableColumn dynamicUse;
    private Label dynamicCurMission;
    private Label dynamicCurFit;

    private int status;
    private int memorySize;
    private int curMission;
    private List<String> mission = new ArrayList<>();
    private List<String> missionWord = new ArrayList<>();
    private ObservableList<MemoryPartition> memoryPartitions = FXCollections.observableArrayList();


    public DynamicController(TableView dynamicTable, TableColumn dynamicMemory, TableColumn dynamicUse, Label dynamicCurMission, Label dynamicCurFit) {
        this.dynamicTable = dynamicTable;
        this.dynamicMemory = dynamicMemory;
        this.dynamicUse = dynamicUse;
        this.dynamicCurMission = dynamicCurMission;
        this.dynamicCurFit = dynamicCurFit;
        status = WAIT;
        curMission = 0;
        memorySize = 640;
        memoryPartitions.add(new MemoryPartition(memorySize,-1));
        dynamicMemory.setCellValueFactory(new PropertyValueFactory("size"));
        dynamicUse.setCellValueFactory(new PropertyValueFactory("user"));
        dynamicTable.setItems(memoryPartitions);
        readIn();
    }

    private void readIn() {
        try {
            //读取
            InputStream is = DynamicController.class.getClassLoader().getResourceAsStream("application.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            while (line != null){
                mission.add(line);
                line = br.readLine();
            }
            is = DynamicController.class.getClassLoader().getResourceAsStream("application_word.txt");
            br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            while (line != null){
                missionWord.add(line);
                line = br.readLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(int status) {
        curMission = 0;
        this.status = status;
        dynamicTable.getItems().clear();
        memoryPartitions.add(new MemoryPartition(memorySize,-1));
    }

    @Override
    public void run() {
        while (true){
            if(status != WAIT){
                fit();
            }
            sleep(1000);
        }

    }

    //1为分配，0为释放
    private Map<String,Integer> decode(String origin){
        Map<String,Integer> map = new HashMap<>();
        String str[] = origin.split("\\|");
        map.put("user", Integer.parseInt(str[0]));
        map.put("type",Integer.parseInt(str[1]));
        map.put("size",Integer.parseInt(str[2]));
        return map;
    }

    private void fit(){
        if(curMission == mission.size()){
            status = WAIT;
            return;
        }
        String task = mission.get(curMission);
        Map map = decode(task);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                dynamicCurMission.setText(missionWord.get(curMission));
            }
        });

        if((int)map.get("type") == 1){
            int index = 0;
            if(status == FIRST_FIT){
                index = firstFit(map);
            }else if(status == BEST_FIT){
                index = bestFit(map);
            }
            MemoryPartition temp = memoryPartitions.get(index);
            if(temp.getSize() == (int)map.get("size")){
                temp.setUser((int)map.get("user"));
            }else{
                temp.setSize(temp.getSize() - (int)map.get("size"));
                memoryPartitions.add(index,new MemoryPartition((int)map.get("size"),(int)map.get("user")));
            }
        }else {
            for(int i = 0;i<memoryPartitions.size();i++){
                MemoryPartition memory = memoryPartitions.get(i);
                if(memory.getUser() == (int)map.get("user")){
                    memoryPartitions.remove(memory);
                    MemoryPartition temp = new MemoryPartition(memory.getSize(),-1);
                    memoryPartitions.add(i,temp);
                    merge(i);
                }
            }
        }
        curMission+=1;

    }
    private int firstFit(Map map) {
        int size = (int)map.get("size");
        for(int i = 0;i<memoryPartitions.size();i++){
            MemoryPartition temp = memoryPartitions.get(i);
            if(temp.getSize()>=size && temp.getUser()==-1){
                return i;
            }
        }
        return -1;
    }

    private int bestFit(Map map){
        int size = (int)map.get("size");
        int bestIndex = -1;
        int bestSize = Integer.MAX_VALUE;
        for(int i = 0;i<memoryPartitions.size();i++){
            MemoryPartition temp = memoryPartitions.get(i);
            if(temp.getUser()==-1 && temp.getSize()>=size && temp.getSize()<bestSize){
                bestIndex = i;
                bestSize = temp.getSize();
            }
        }
        return bestIndex;
    }

    private void merge(int position){
        MemoryPartition curMem = memoryPartitions.get(position);
        //与前面的块合并
        int pre = position - 1;
        if(pre >= 0){
            MemoryPartition preMem = memoryPartitions.get(pre);
            if(preMem.getUser() == -1){
                preMem.setSize(preMem.getSize() + curMem.getSize());
                memoryPartitions.remove(curMem);
                curMem = preMem;
                position -= pre;
            }
        }
        //与后面的块合并
        int post = position + 1;
        if(post < memoryPartitions.size()){
            MemoryPartition postMem = memoryPartitions.get(post);
            if(postMem.getUser()==-1){
                curMem.setSize(curMem.getSize() + postMem.getSize());
                memoryPartitions.remove(postMem);
            }
        }
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
