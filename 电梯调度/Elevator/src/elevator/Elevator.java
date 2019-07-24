package elevator;

import com.jfoenix.controls.JFXButton;
import elevator.Const.Command;
import elevator.Const.FXColor;
import elevator.Const.Icon;
import javafx.application.Platform;
import javafx.scene.control.Slider;

import java.util.*;

public class Elevator implements Runnable{
    private int id;
    private int status;//见Const.Command, 0代表静止，1代表上升，-1代表下降
    private int nextStatus;//当将要执行电梯外部任务时，表明该外部任务的状态
    private int currentFloor;
    private boolean arrivedNextFloor = false;
    private boolean doorIsOpen;
    private Slider slider;//当前电梯的示意Slider
    private JFXButton outerDisplayButton;
    private JFXButton innerFloorDisplayButton;
    private JFXButton innerUpDisplayButton;
    private JFXButton innerDownDisplayButton;
    private Controller controller;
    private TreeSet<Integer> upTaskSet = new TreeSet<>(); //存放电梯任务,上升任务升序排列，下降任务降序排列
    private TreeSet<Integer> downTaskSet = new TreeSet<>(new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o1 > o2)
                return -1;
            else if (o1 < o2) {
                return 1;
            } else {
                return 0;
            }
        }
    });
    private Map<Integer,String> innerButtonState = new HashMap<>();  //因为电梯内部按钮共享，map用来存当前电梯内部按钮的状态
    private List<FloorButton> outerFloorTask = new ArrayList<>();//存放外部任务，便于到达后熄灭外按钮

    public Elevator(int id, Slider slider, JFXButton outerDisplayButton, JFXButton innerFloorDisplayButton, JFXButton innerUpDisplayButton, JFXButton innerDownDisplayButton, Controller controller) {
        this.id = id;
        this.slider = slider;
        this.outerDisplayButton = outerDisplayButton;
        this.innerFloorDisplayButton = innerFloorDisplayButton;
        this.innerUpDisplayButton = innerUpDisplayButton;
        this.innerDownDisplayButton = innerDownDisplayButton;
        this.controller = controller;
        this.status = Command.REST;
        this.nextStatus = Command.REST;
        this.currentFloor = 1;
    }

    @Override
    public void run() {
        System.out.println("启动电梯" + id + "线程！");
        while (true) {
            if(status == Command.REST){
                if(!upTaskSet.isEmpty()){
                    move(upTaskSet.first(),Command.UP);
                }else if(!downTaskSet.isEmpty()){
                    move(downTaskSet.first(),Command.DOWN);
                }
            }else if(status == Command.UP){
                if(!upTaskSet.isEmpty()){
                    move(upTaskSet.first(),Command.UP);
                }else {
                    status = Command.REST;
                }
            }else{
                if(!downTaskSet.isEmpty()){
                    move(downTaskSet.first(),Command.DOWN);
                }else{
                    status = Command.REST;
                }
            }
            sleep(1);
        }
    }

    private void move(int next_floor, int state) {
        System.out.println(upTaskSet + " " + downTaskSet);
        //还未到达目的地时，运动
        if(currentFloor != next_floor) {
            if (state == Command.UP) {
                status = Command.UP;
                currentFloor += 1;
            } else if (state == Command.DOWN) {
                status = Command.DOWN;
                currentFloor -= 1;
            }
        }
        //到达目的地后，删除该任务并更新状态。1.外任务需要更新外部按钮 2.内任务需要更新内部按钮
        if(currentFloor == next_floor){
            arrivedNextFloor = true;
            if (!outerFloorTask.isEmpty()) {
                Iterator<FloorButton> iterator = outerFloorTask.iterator();
                while(iterator.hasNext()){
                    FloorButton button = iterator.next();
                    nextStatus = button.direction;
                    if(button.floor == currentFloor){
                        controller.removeOuterButton(button.floor, button.direction);
                        iterator.remove();
                        nextStatus = Command.REST;
                    }
                }
            }
            //因为是用内任务模拟开门，存在静止状态仍要更新任务集合的情况，所以三个状态都要判断
            if (status == Command.DOWN) {
                downTaskSet.remove(next_floor);
                if(downTaskSet.isEmpty()){
                    status = Command.REST;
                }
            } else if(status == Command.UP){
                upTaskSet.remove(next_floor);
                if(upTaskSet.isEmpty()){
                    status = Command.REST;
                }
            }else{
                if(downTaskSet.contains(currentFloor)) downTaskSet.remove(next_floor);
                if(upTaskSet.contains(currentFloor)) upTaskSet.remove(next_floor);
            }

            controller.setInnerButtonStyle(id,currentFloor,FXColor.GREY);
            innerButtonState.remove(currentFloor);
        }else{
            arrivedNextFloor = false;
        }

        //在runLater线程中执行UI更新操作
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                slider.setValue(currentFloor);
                outerDisplayButton.setText(currentFloor + (status == Command.UP? Icon.UP_ICON : (status == Command.DOWN? Icon.DOWN_ICON : Icon.REST_ICON)));
                if (controller.currentElevatorId == id) {
                    innerFloorDisplayButton.setText(String.valueOf(currentFloor));
                    innerUpDisplayButton.setStyle(status == Command.UP ? FXColor.YELLOW : FXColor.GREY);
                    innerDownDisplayButton.setStyle(status == Command.DOWN ? FXColor.YELLOW : FXColor.GREY);
                }
            }
        });
        //开关门的UI更新
        if(arrivedNextFloor){
            slider.setStyle("-jfx-default-thumb: #FF6600");
            doorIsOpen = true;
            sleep(2);
            if(doorIsOpen){
               closeDoor();
            }
        }
    }
    
    //处理电梯内部按钮的请求
    public void addInnerTask(int floor){
        if(!upTaskSet.contains(floor)&& !downTaskSet.contains(floor)){
            if(floor <= currentFloor){
                downTaskSet.add(floor);
            }else{
                upTaskSet.add(floor);
            }
            innerButtonState.put(floor,FXColor.YELLOW);
        }
    }

    //处理电梯外部任务分配的请求
    public void addOuterTask(int floor, int direction){
        if(!outerFloorTask.isEmpty()){
            for (FloorButton button:outerFloorTask){
                if(button.floor == floor && button.direction == direction)
                    return;
            }
        }
        outerFloorTask.add(new FloorButton(floor,direction));
        nextStatus = direction;
        if(floor > currentFloor){
            upTaskSet.add(floor);
        }else{
            downTaskSet.add(floor);
        }
    }

    //通过添加同层的上升任务来模拟开门
    public void openDoor(){
        if ((arrivedNextFloor || status == Command.REST) && !doorIsOpen) {
            upTaskSet.add(currentFloor);
        }
    }

    public void closeDoor(){
        slider.setStyle("-jfx-default-thumb: #0f9d58");
        doorIsOpen = false;
    }

    
    public int getStatus() {
        return status;
    }

    public int getNextStatus() {
        return nextStatus;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Map<Integer, String> getInnerButtonState() {
        return innerButtonState;
    }

    public void sleep(int sec){
        try {
            Thread.sleep( sec * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}
