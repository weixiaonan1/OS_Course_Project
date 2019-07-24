package DS.Controller;

import DS.Basic.Const;
import DS.Model.FAT;
import DS.Util.GsonUtils;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DiskManager {
    private FAT fat;
    private IO io;

    public DiskManager() {
        io = new IO();
    }

    public void init(){
        io.init();
        //从Block中读取位图
        ByteBuffer fatBuffer = io.read(1);
        String fatJson = new String(fatBuffer.array(), Const.CHARSET);

        fat = GsonUtils.fromJson(fatJson,FAT.class);
    }

    public void format(){
        for (int i = 0; i < Const.BLOCK_COUNT; i++) {
            this.io.write(i,  "");
        }

        // 重新初始化位图以及根目录
        io.initRootFile();
    }

    public ByteBuffer readFile(int startIndex, int endIndex) {
        int index = startIndex;
        ByteBuffer resultBuffer = ByteBuffer.allocate(0);

        while (true){
            ByteBuffer currentBinDataBuffer = io.read(index);
            byte[] temp = resultBuffer.array();
            resultBuffer.rewind();
            resultBuffer.get(temp, 0, temp.length);

            // resize
            resultBuffer = ByteBuffer.allocate(temp.length
                    + currentBinDataBuffer.limit());

            // put back
            resultBuffer.put(temp, 0, temp.length);

            // append
            resultBuffer.put(currentBinDataBuffer.array(), 0,
                    currentBinDataBuffer.limit());

            if (currentBinDataBuffer.limit() < currentBinDataBuffer.capacity()) {
                break;
            }
            if(index == endIndex || fat.fatMap.get(index) == Const.FILE_END){
                break;
            }
            index = fat.fatMap.get(index);
        }

        resultBuffer.rewind();
        return resultBuffer;
    }

    public List<ByteBuffer> readFCBList(int startIndex, int endIndex){
        List<ByteBuffer> bufferList = new ArrayList<>();
        int index = startIndex;

        while (true){
            ByteBuffer currentBinDataBuffer = io.read(index);
            bufferList.add(currentBinDataBuffer);
            if(index == endIndex || fat.fatMap.get(index) == Const.FILE_END){
                break;
            }
            index = fat.fatMap.get(index);
        }
        return bufferList;
    }
    public int findFreeBlock() {
        return fat.findFreeBlock(0);
    }

    public int findFreeBlock(int offset){
        return fat.findFreeBlock(offset);
    }

    public void allocate(int blockId, int state) {
        modifyFAT(blockId,state);
    }

    public void modifyFAT(int blockId, int state) {
        fat.fatMap.put(blockId,state);
        io.write(1,new Gson().toJson(fat));
    }

    public void write(int blockId, String json) {
        io.write(blockId,json);
    }

    public void write(int blockId, byte[] bytes) {
        io.write(blockId,bytes);
    }



    public void deleteFile(int startIndex) {
        int curIndex = startIndex;
        int nextIndex = fat.fatMap.get(curIndex);
        while(true){
            fat.fatMap.put(curIndex,Const.FREE);
            if(nextIndex == Const.FILE_END)
                break;
            curIndex = nextIndex;
            nextIndex = fat.fatMap.get(curIndex);
        }
        io.write(1,new Gson().toJson(fat));
    }

    public List<Integer> getFileBlocks(int startIndex){
        List<Integer> lists = new ArrayList<>();
        int index = startIndex;
        while (true){
            lists.add(index);
            if(fat.fatMap.get(index) == Const.FILE_END){
                break;
            }
            index = fat.fatMap.get(index);
        }
        return lists;
    }

    public void save() {
        io.save();
    }
}
