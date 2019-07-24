package elevator;

import com.jfoenix.controls.JFXButton;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import elevator.Const.Command;
import elevator.Const.FXColor;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    @FXML
    private StackPane root;
    @FXML
    private ToggleGroup toggleGroup;

    private List<JFXButton> innerButton = new ArrayList<>();
    private List<Slider> sliderList = new ArrayList<>();
    private List<JFXButton> outerDisplayButton = new ArrayList<>();
    private List<JFXButton> outerFloorUpButton = new ArrayList<>();
    private List<JFXButton> outerFloorDownButton = new ArrayList<>();
    private JFXButton innerFloorDisplayButton;
    private JFXButton innerUpDisplayButton;
    private JFXButton innerDownDisplayButton;
    private JFXButton openDoorButton;
    private JFXButton closeDoorButton;
    private JFXButton alarmButton;
    private List<Elevator> elevatorList = new ArrayList<>();
    private Queue<FloorButton> outerTaskQueue = new LinkedList<FloorButton>();

    public int currentElevatorId;
    private ExecutorService service = Executors.newCachedThreadPool();

    public void init(){
        initChooseElevator();
        initInnerElevator();
        initOuterElevator();
        initDisplayElevator();
        //创建五部电梯并开始运行
        for(int i = 1;i<=5;i++){
            Elevator elevator = new Elevator(i, sliderList.get(i-1), outerDisplayButton.get(i-1),innerFloorDisplayButton, innerUpDisplayButton, innerDownDisplayButton,this);
            elevatorList.add(elevator);
            service.execute(elevator);
        }
        //创建外部调度算法的线程并开始运行
        Schedule schedule = new Schedule(outerTaskQueue,this);
        service.execute(schedule);

        currentElevatorId = 1;
    }

    private void initChooseElevator() {
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            @Override
            public void changed(ObservableValue<? extends Toggle> changed, Toggle oldVal, Toggle newVal)
            {
                RadioButton temp_rb=(RadioButton)newVal;
                currentElevatorId = Integer.valueOf(temp_rb.getText().split(" ")[1]);
                System.out.println("当前运行的电梯id是：" + currentElevatorId);
                resetInnerButton();
            }
        });
    }

    public List<Elevator> getElevatorList() {
        return elevatorList;
    }

    private void initInnerElevator() {
        //初始化功能键
        openDoorButton = (JFXButton) root.lookup("#open-door");
        openDoorButton.setOnAction(action->{
            elevatorList.get(currentElevatorId - 1).openDoor();
        });

        closeDoorButton = (JFXButton) root.lookup("#close-door");
        closeDoorButton.setOnAction(action->{
            elevatorList.get(currentElevatorId - 1).closeDoor();
        });

        alarmButton = (JFXButton) root.lookup("#alarm");
        alarmButton.setOnAction(action->{
            AudioClip audioClip = new AudioClip(Paths.get("src/resource/alarm.wav").toUri().toString());
            audioClip.play();
        });

        //初始化电梯状态展示的三个按钮
        innerFloorDisplayButton = (JFXButton) root.lookup("#floor_display");
        innerFloorDisplayButton.setText("1");
        innerUpDisplayButton = (JFXButton) root.lookup("#up-display");
        innerDownDisplayButton = (JFXButton) root.lookup("#down-display");

        //初始化电梯内部20个按键
        for(int i =1;i <= 20;i++){
            JFXButton button = (JFXButton) root.lookup("#e"+ i);
            button.setOnAction(action->{
                button.setDisable(true);
                int floor = Integer.valueOf(button.getText());
                button.setStyle(FXColor.YELLOW);
                elevatorList.get(currentElevatorId -1).addInnerTask(floor);
            });
            innerButton.add(button);
        }

    }
    private void initOuterElevator() {
        //上行按钮(从1楼到19楼)
        for(int i = 1;i<20;i++){
            JFXButton button = (JFXButton) root.lookup(String.format("#L%d-up",i));
            button.setOnAction(action->{
                //外部上行按钮的监听事件,改变颜色并设置为不可用,避免多次添加同一任务,将该外部任务添加到调度队列中
                button.setStyle(FXColor.YELLOW);
                button.setDisable(true);
                int floor = getFloorFromId(button.getId());
                outerTaskQueue.offer(new FloorButton(floor, Command.UP));
            });
            outerFloorUpButton.add(button);
        }

        //下行按钮(从2楼到20楼)
        for(int i = 2;i<=20;i++){
            JFXButton button = (JFXButton) root.lookup(String.format("#L%d-down",i));
            button.setOnAction(action->{
                //外部下行按钮的监听事件,改变颜色并设置为不可用,避免多次添加同一任务,将该外部任务添加到调度队列中
                button.setStyle(FXColor.YELLOW);
                button.setDisable(true);
                int floor = getFloorFromId(button.getId());
                outerTaskQueue.offer(new FloorButton(floor, Command.DOWN));
            });
            outerFloorDownButton.add(button);
        }
    }

    private void initDisplayElevator() {
        for (int i = 1; i <=5; i++){
            Slider slider = (Slider) root.lookup(String.format("#e%d-slider",i));
            slider.setValue(1);

            sliderList.add(slider);
            JFXButton button = (JFXButton) root.lookup(String.format("#e%d-display",i));
            button.setText("1-");
            outerDisplayButton.add(button);
        }
    }

    private int getFloorFromId(String id){
        return Integer.valueOf(id.split("L")[1].split("-")[0]);
    }

    //当切换电梯时重置电梯内部的按钮状态
    private void resetInnerButton() {
        Elevator elevator = elevatorList.get(currentElevatorId - 1);
        //按钮恢复默认状态
        for (Button button : innerButton) {
            button.setStyle(FXColor.GREY);
            button.setDisable(false);
        }
        innerUpDisplayButton.setStyle(FXColor.GREY);
        innerDownDisplayButton.setStyle(FXColor.GREY);

        //访问当前电梯的按钮状态map及状态，设置按钮
        Iterator iter = elevator.getInnerButtonState().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            int key = (int) entry.getKey();
            innerButton.get(key-1).setDisable(true);
            innerButton.get(key-1).setStyle(FXColor.YELLOW);
        }
        innerFloorDisplayButton.setText(String.valueOf(elevator.getCurrentFloor()));
        int status = elevator.getStatus();
        if(status == 1){
            innerUpDisplayButton.setStyle(FXColor.YELLOW);
        }else if(status == 2){
            innerDownDisplayButton.setStyle(FXColor.YELLOW);
        }

    }

    public void setInnerButtonStyle(int id, int floor, String style){
        if(currentElevatorId == id){
            JFXButton button = innerButton.get(floor-1);
            button.setStyle(style);
            button.setDisable(false);
        }
    }


    public void removeOuterButton(int current_floor, int status) {
        JFXButton button;
        if(status == Command.UP){
            button = outerFloorUpButton.get(current_floor - 1);
        }else{
            button = outerFloorDownButton.get(current_floor - 2);
        }
        button.setStyle(FXColor.GREY);
        button.setDisable(false);
    }

}
