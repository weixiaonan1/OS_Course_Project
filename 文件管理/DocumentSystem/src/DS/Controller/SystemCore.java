package DS.Controller;

import DS.Basic.Const;
import DS.Basic.FileType;
import DS.Basic.Index;
import DS.Basic.Response;
import DS.Controller.DiskManager;
import DS.Model.FCB;
import DS.Util.GsonUtils;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SystemCore {
    private DiskManager diskManager;
    public FCB currentDirFCB;
    public List<FCB> childrenFCB = new ArrayList<>();

    public SystemCore(){
        diskManager = new DiskManager();
        init();
    }
    private void init(){

        diskManager.init();
        //读取根目录FCB
        ByteBuffer currentDirFCBBuffer = diskManager.readFile(0,0);
        String currentDirFCBJSON = new String(currentDirFCBBuffer.array(), Const.CHARSET);
        currentDirFCB = GsonUtils.fromJson(currentDirFCBJSON,FCB.class);

        //读取根目录文件的FCB
        Index index = currentDirFCB.index;
        if(index.startIndex!= -1){
            childrenFCB = GsonUtils.getList(diskManager.readFCBList(index.startIndex,index.endIndex),FCB.class);
        }else {
            childrenFCB.clear();
        }

    }

    public String readFile(int nodeId){
        int index = findFCBByNodeId(nodeId);
        FCB fcb = childrenFCB.get(index);
        if(fcb.index.startIndex == -1){
            return null;
        }
        return new String(diskManager.readFile(fcb.index.startIndex,fcb.index.endIndex).array(),Const.CHARSET);

    }

    public Response writeFile(int nodeId, String content){
        FCB fcb = childrenFCB.get(findFCBByNodeId(nodeId));
        List<Integer> blockLists = new ArrayList<>();
        if(fcb.index.startIndex != -1){
            blockLists = diskManager.getFileBlocks(fcb.index.startIndex);
        }
        byte[] bytes = content.getBytes(Const.CHARSET);
        int count = (int)Math.ceil((double)bytes.length/Const.BLOCK_SIZE);
        if(blockLists.size() < count){
            int offset = 0;
            int addCount = count - blockLists.size();
            for(int i = 0; i < addCount;i++){
                int freeId = diskManager.findFreeBlock(offset);
                if(freeId == -1){
                    return new Response(false,"磁盘空间不足");
                }
                blockLists.add(freeId);
                offset = freeId;
            }
        }else if(blockLists.size()>count){
            for(int i = count;i < blockLists.size();i++){
                diskManager.modifyFAT(blockLists.get(i), Const.FREE);
            }
            blockLists = blockLists.subList(0,count);
        }
        int next;
        byte[] temp;
        fcb.index.startIndex = blockLists.get(0);
        fcb.index.endIndex = blockLists.get(blockLists.size() - 1);

        for(int i = 0;i<blockLists.size();i++){
            if(i == blockLists.size()-1){
                next = Const.FILE_END;
                temp = Arrays.copyOfRange(bytes,i*Const.BLOCK_SIZE,bytes.length);
            }else{
                next = blockLists.get(i+1);
                temp = Arrays.copyOfRange(bytes,i*Const.BLOCK_SIZE,(i+1) * Const.BLOCK_SIZE);
            }
            diskManager.allocate(blockLists.get(i),next);
            diskManager.write(blockLists.get(i),temp);
        }
        Date date = new Date();
        int newActualSize = bytes.length;
        int newOccupiedSize = blockLists.size()*Const.BLOCK_SIZE;
        modifyFolder(fcb.actualSize, fcb.occupiedSize,newActualSize,newOccupiedSize,date);
        fcb.modifyDate = date;
        fcb.actualSize = newActualSize;
        fcb.occupiedSize = newOccupiedSize;

        diskManager.write(nodeId,new Gson().toJson(fcb));

        return  new Response(true,"写文件成功");
    }

    private void modifyFolder( int actualSize, int occupiedSize, int newActualSize, int newOccupiedSize, Date date) {
        FCB parent = currentDirFCB;
            while (true){
                parent.modifyDate = date;
                parent.actualSize = parent.actualSize - actualSize + newActualSize;
                parent.occupiedSize = parent.occupiedSize - occupiedSize + newOccupiedSize;
                diskManager.write(parent.nodeId,new Gson().toJson(parent));
                int parentFCBNodeId = parent.parentFCBNodeId;
                if(parentFCBNodeId == -1){
                    break;
                }
                parent = GsonUtils.getList(diskManager.readFCBList(parentFCBNodeId,parentFCBNodeId),FCB.class).get(0);
            }

    }


    public Response create(String name, FileType type){
        if(!checkFileName(name,type)){
            String s = type == FileType.FILE? "文件" : "文件夹";
            return new Response(false,"该"+ s + "已存在!");
        }
        //只创建FCB而不分配数据内存，之后写入数据时才分配
        int FCBStart = diskManager.findFreeBlock();
        if(FCBStart != -1){
            FCB fcb = new FCB(FCBStart,name,type,currentDirFCB.nodeId,new Index(-1,-1));
            //分配空间并将该fcb写入物理内存
            diskManager.allocate(FCBStart,Const.FILE_END);
            writeFCB(fcb);
            //将FAT表中当前FCB的最后一块的状态改为新分配的块
            //如果原来文件夹为空
            if(currentDirFCB.index.startIndex == -1){
                currentDirFCB.index.startIndex = fcb.nodeId;
            }
            else{
                diskManager.modifyFAT(childrenFCB.get(childrenFCB.size() - 1).nodeId,fcb.nodeId);
            }
            childrenFCB.add(fcb);
            currentDirFCB.index.endIndex = fcb.nodeId;
            modifyFolder(0,0,0,0,new Date());

            return new Response(true,"创建成功");
        }else{
            return new Response(false,"磁盘空间不足");
        }
    }

    public void deleteFile(int nodeId){
        int index = findFCBByNodeId(nodeId);
        FCB fcb = childrenFCB.get(index);
        //清空文件的数据块
        if(fcb.index.startIndex!= -1){
            diskManager.deleteFile(fcb.index.startIndex);
        }
        //更新父FCB的信息
        updateFCBIndex(index);
        childrenFCB.remove(fcb);
        modifyFolder(fcb.actualSize,fcb.occupiedSize,0,0,new Date());
        //删除该文件的FCB
        diskManager.modifyFAT(fcb.nodeId,Const.FREE);
    }

    private void updateFCBIndex(int index){
        int size = childrenFCB.size();
        //如果删除第一个文件
        if(index == 0){
            //文件夹为空
            if(size == 1){
                currentDirFCB.index.startIndex = -1;
                currentDirFCB.index.endIndex = -1;
            }else{
                currentDirFCB.index.startIndex = childrenFCB.get(1).nodeId;
            }
        }
        //如果删除了最后一个文件
        else if(index == size -1){
            currentDirFCB.index.endIndex = childrenFCB.get(size - 2).nodeId;
            diskManager.modifyFAT(childrenFCB.get(size - 2).nodeId,Const.FILE_END);
        }else{
            diskManager.modifyFAT(childrenFCB.get(index - 1).nodeId, childrenFCB.get(index + 1).nodeId);
        }
    }

    public void deleteFolder(int nodeId){
        int index = findFCBByNodeId(nodeId);
        FCB fcb = childrenFCB.get(index);
        //删除该PCB
        updateFCBIndex(index);
        childrenFCB.remove(fcb);
        modifyFolder(fcb.actualSize,fcb.occupiedSize,0,0,new Date());

        recursiveDeleteFolder(fcb);
    }

    private void recursiveDeleteFolder(FCB fcb){
        //删除该文件夹的FCB
        diskManager.modifyFAT(fcb.nodeId,Const.FREE);
        //获取子文件
        if(fcb.index.startIndex == -1){
            return;
        }
        List<FCB> children =  GsonUtils.getList(diskManager.readFCBList(fcb.index.startIndex,fcb.index.endIndex),FCB.class);
        for(int i = 0;i<children.size();i++){
            if(children.get(i).type == FileType.FILE){
                //删除文件FCB
                diskManager.modifyFAT(children.get(i).nodeId,Const.FREE);
                //删除文件数据块
                if(children.get(i).index.startIndex!= -1)
                     diskManager.deleteFile(children.get(i).index.startIndex);
            }else {
                recursiveDeleteFolder(children.get(i));
            }
        }
    }

    public void entryDir(int nodeId){
        FCB folder = childrenFCB.get(findFCBByNodeId(nodeId));
        currentDirFCB = folder;
        if(currentDirFCB.index.startIndex != -1) {
            childrenFCB =  GsonUtils.getList(diskManager.readFCBList(currentDirFCB.index.startIndex,currentDirFCB.index.endIndex),FCB.class);
        }
        else{
            childrenFCB.clear();
        }
    }

    public Response leaveDir(){
        if(currentDirFCB.parentFCBNodeId == -1){
            return new Response(false,"已经到达根目录!");
        }
        currentDirFCB = GsonUtils.getList(diskManager.readFCBList(currentDirFCB.parentFCBNodeId,currentDirFCB.parentFCBNodeId),FCB.class).get(0);
        if(currentDirFCB.index.startIndex == -1){
            childrenFCB.clear();
        }else{
            childrenFCB =  GsonUtils.getList(diskManager.readFCBList(currentDirFCB.index.startIndex,currentDirFCB.index.endIndex),FCB.class);
        }
        return  new Response(true,"成功返回!");
    }

    public void format(){
        diskManager.format();
        init();
    }

    public void exit(){
        diskManager.save();
    }

    private int findFCBByNodeId(int nodeId){
        for(int i = 0;i < childrenFCB.size();i++){
            if(childrenFCB.get(i).nodeId == nodeId){
                return i;
            }
        }
        return -1;
    }

    //将FCB的信息写入物理内存
    private void writeFCB(FCB fcb) {
        diskManager.write(fcb.nodeId, new Gson().toJson(fcb));
    }

    //检查文件名称是否重复
    private boolean checkFileName(String fileName, FileType type) {
        for(int i = 0;i<childrenFCB.size();i++){
            if(childrenFCB.get(i).type == type && childrenFCB.get(i).name.equals(fileName)){
                return false;
            }
        }
        return true;
    }

    public void print(){
        System.out.println("current:" + currentDirFCB.toString());
        for(int i = 0;i<childrenFCB.size();i++){
            System.out.println("children"+i+":" + childrenFCB.get(i).toString());
        }
    }
}
