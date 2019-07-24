package elevator;

import elevator.Const.Command;
import java.util.Queue;

//外部调度算法
public class Schedule implements Runnable {

    private Controller controller;
    private Queue<FloorButton> queue;

    public Schedule(Queue<FloorButton> queue, Controller controller) {
        this.queue = queue;
        this.controller = controller;
    }

    private void sleep(int mills){
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        while(true){
            //不断的处理外部任务队列中的任务，每两个任务处理间隔为2s
            while(!queue.isEmpty()){

                FloorButton button = queue.poll();
                int floor = button.floor;
                int button_type = button.direction;
                int target = -1;//记录将该任务分配给的电梯id
                int[] eFloor = new int[5];
                int[] eStatus = new int[5];
                int[] eNextStatus = new int[5];
                int[] temp = new int[5];//存储电梯当前位置和目标位置的差

                while (target == -1){
                    int differ = Integer.MAX_VALUE;
                    for(int i = 0;i < 5; i++){
                        Elevator elevator = controller.getElevatorList().get(i);
                        eStatus[i] = elevator.getStatus();
                        eNextStatus[i] = elevator.getNextStatus();
                        eFloor[i] = elevator.getCurrentFloor();
                        temp[i] = floor - eFloor[i];
                    }
                    //从同方向的电梯中找距离该层最近的:电梯运动方向一致，如果该电梯被分配了外任务，外任务的方向也一致
                    for(int i = 0;i<5;i++){
                        if(eStatus[i] * button_type > 0 && eNextStatus[i] * button_type >= 0){
                            //若电梯上升，该层要在电梯的上方;下降则在电梯的下方
                            if((button_type == Command.UP && temp[i] < 0) ||(button_type == Command.DOWN && temp[i] > 0)) {
                                continue;
                            }
                            int abs_temp = Math.abs(temp[i]);
                            if(abs_temp < differ){
                                differ = abs_temp;
                                target = i;
                            }
                        }
                    }
                    //上述条件没有找到，则从静止的电梯中找距离该层最近的
                    if(target == -1){
                        for(int i = 0;i < 5;i++){
                            if(eStatus[i] == Command.REST){
                                int abs_temp = Math.abs(temp[i]);
                                if(abs_temp < differ){
                                    differ = abs_temp;
                                    target = i;
                                }
                            }
                        }
                    }
                    //如果依然没有找到，则等待一会，再重新寻找
                    if(target == -1){
                        sleep(500);
                    }
                }
                System.out.println("外部分配：电梯" + target + " floor:" + floor + " direction:" + button_type);
                controller.getElevatorList().get(target).addOuterTask(floor,button_type);
                sleep(2000);
            }
            sleep(0);
        }

    }
}
