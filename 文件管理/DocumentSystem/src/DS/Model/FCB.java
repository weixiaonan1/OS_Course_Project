package DS.Model;

import DS.Basic.FileType;
import DS.Basic.Index;

import java.util.Date;

public class FCB {
    public int nodeId;//操作系统管理文件的唯一标识,用FCB所在块id实现
    public String name;
    public FileType type;
    public int actualSize;
    public int occupiedSize;
    public Date createDate;
    public Date modifyDate;
    public int parentFCBNodeId;//父FCB所在块
    public Index index;//文件则为数据所在块，文件夹则为子文件FCB所在块

    public FCB(int nodeId, String name, FileType type, int parent, Index index) {
        this.nodeId = nodeId;
        this.name = name;
        this.type = type;
        this.parentFCBNodeId = parent;
        this.index = index;
        actualSize = 0;
        occupiedSize = 0;
        createDate = modifyDate = new Date();
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type == FileType.FILE ? "文本文件" : "文件夹";
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public int getActualSize() {
        return actualSize;
    }

    public void setActualSize(int actualSize) {
        this.actualSize = actualSize;
    }

    public int getOccupiedSize() {
        return occupiedSize;
    }

    public void setOccupiedSize(int occupiedSize) {
        this.occupiedSize = occupiedSize;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public int getParentFCBNodeId() {
        return parentFCBNodeId;
    }

    public void setParentFCBNodeId(int parentFCBNodeId) {
        this.parentFCBNodeId = parentFCBNodeId;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "nodeId:" + nodeId + " name:" + name +" type:" + type   + " start:" + index.startIndex+" end:" + index.endIndex + " createDate:" + createDate + " actualSize:" + actualSize + " occupiedSize:" + occupiedSize;
    }
}
