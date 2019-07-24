package MemoryManagement;

public class Instruction {
    private int id;
    private int address;
    private int page;

    public Instruction(int id, int address) {
        this.id = id;
        this.address = address;
        this.page = address / 10;
    }

    public int getId() {
        return id;
    }

    public int getAddress() {
        return address;
    }

    public int getPage() {
        return page;
    }

    @Override
    public String toString() {
        return "序号:" + id +" 地址:" + address + "页数:" + page;
    }
}
