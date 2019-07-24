package MemoryManagement;

public class Block {
    private int id;
    private int page;
    private String range;

    public Block(int id, int page) {
        this.id = id;
        this.page = page;
        this.range = "[" + page * 10 + "," + (page * 10 + 10) + ")";
    }

    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public String getRange() {
        return range;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPage(int page) {
        this.page = page;
        this.range = "[" + page * 10 + "," + (page * 10 + 10) + ")";
    }

    @Override
    public String toString() {
        return "id:" + id + " page" + page;
    }
}
