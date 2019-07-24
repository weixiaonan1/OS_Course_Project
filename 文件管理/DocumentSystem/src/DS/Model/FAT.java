package DS.Model;

import DS.Basic.Const;

import java.util.HashMap;
import java.util.Map;

public class FAT {

    public HashMap<Integer, Integer> fatMap = new HashMap<>();


    public int findFreeBlock(int offset){
        for(Map.Entry<Integer, Integer> entry : fatMap.entrySet()) {
            if(entry.getKey() <= offset){
                continue;
            }
            if(entry.getValue() == Const.FREE)
                return entry.getKey();
        }
        return -1;
    }

    public void init(){
        for(int i = 0;i<Const.BLOCK_COUNT;i++){
            fatMap.put(i,Const.FREE);
        }
    }

}