package elevator;

//FloorButton只是记录任务的信息，类似于结构体
public class FloorButton {
    int floor;
    int direction;

    public FloorButton(int floor, int direction) {
        this.floor = floor;
        this.direction = direction;
    }
}
