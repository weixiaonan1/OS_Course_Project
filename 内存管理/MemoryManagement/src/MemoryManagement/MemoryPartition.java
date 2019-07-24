package MemoryManagement;

public class MemoryPartition {
    private int size;
    private int user;

    public MemoryPartition(int size, int user) {
        this.size = size;
        this.user = user;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public int getSize() {
        return size;
    }

    public int getUser() {
        return user;
    }
}
