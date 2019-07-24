package DS.Controller;

import DS.Basic.Const;
import DS.Basic.FileType;
import DS.Basic.Index;
import DS.Model.Block;
import DS.Model.FAT;
import DS.Model.FCB;
import com.google.gson.Gson;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Vector;

public class IO {
    private Vector<Block> blocks = new Vector<Block>();

    //初始化block，从磁盘同步
    public void init(){
        System.out.println("系统开始初始化...");
        File diskDir = new File("disk");

        boolean diskDirExists = diskDir.exists();
        if (!diskDirExists) {
            System.out.println("!!!物理磁盘上模拟信息不存在");
            System.out.println("在物理磁盘上新建目录...");
            diskDir.mkdirs();
            System.out.println("...在物理磁盘上新建目录完成");
        }

        // 初始化物理块
        System.out.println("初始化物理块...");
        for (int i = 0; i < Const.BLOCK_COUNT; i++) {
            Block block = new Block(i);
            block.sync();
            this.blocks.add(block);
        }
        System.out.println("...物理块初始化完成");

        // 本地已有文件信息，无需初始化
        if (diskDirExists) {
            return;
        }

        // 初始化FAT表以及根目录
        this.initRootFile();
    }

    public void initRootFile(){
        // 初始化FAT
        System.out.println("初始化FAT表...");
        FAT fat = new FAT();
        fat.init();
        fat.fatMap.put(0,Const.FILE_END);
        fat.fatMap.put(1,Const.FILE_END);

        Gson gson = new Gson();

        // 初始化根目录
        // 根目录FCB，放在0号块，占一个块
        System.out.println("初始化根目录...");


        FCB rootDirFCB = new FCB(0,"root", FileType.FOLDER,-1,new Index(-1,-1));
        String rootDirFCBJSON = gson.toJson(rootDirFCB);

        // 保存根目录FCB以及根目录目录文件
        this.write(rootDirFCB.nodeId, rootDirFCBJSON);

        System.out.println("...根目录初始化完成");

        // 保存位图
        this.write(1, gson.toJson(fat));
        System.out.println("...fat表初始化完成");

        // 写回物理磁盘
        this.save();
        System.out.println("...系统初始化完成");
    }

    public void save() {
        System.out.println("系统数据写回物理磁盘...");
        for (int i = 0; i < Const.BLOCK_COUNT; i++) {
            this.blocks.get(i).update();
        }
        System.out.println("...系统数据保存完毕");
    }

    public void write(int blockId, String content) {
        this.blocks.get(blockId).wipe(); // 清空该块，从头写起
        byte[] writeBytes = content.getBytes(Const.CHARSET);
        this.blocks.get(blockId).getBinData()
                .put(writeBytes); // 写入数据
    }

    public void write(int blockId, byte[] bytes){
        this.blocks.get(blockId).wipe(); // 清空该块，从头写起
        this.blocks.get(blockId).getBinData()
                .put(bytes); // 写入数据
    }

    public ByteBuffer read(int blockId) {
        return blocks.get(blockId).getBinData();
    }
}
