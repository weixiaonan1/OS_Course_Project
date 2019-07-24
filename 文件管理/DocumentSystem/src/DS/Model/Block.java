package DS.Model;

import DS.Basic.Const;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Block {
    private int id;
    private ByteBuffer binData;
    private String filePath;

    public Block(int id){
        this.id = id;
        this.binData = ByteBuffer.allocate(Const.BLOCK_SIZE);
        if(id == 1){
            this.binData = ByteBuffer.allocate(5*Const.BLOCK_SIZE);
        }
        this.filePath = "disk/"+ this.id + ".bin";
    }

    public void wipe(){
        binData.clear();
        binData.put(new byte[Const.BLOCK_SIZE]);
        binData.clear();
    }

    public void sync(){
        File binFile = new File(filePath);
        if(!binFile.exists()){
            return;
        }
        FileChannel inputChannel;
        try {
            inputChannel = new FileInputStream(binFile).getChannel();
            inputChannel.read(this.binData);
            this.binData.flip();
            inputChannel.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void update() {
        File binFile = new File(this.filePath);
        FileChannel outputChannel;
        try {
            outputChannel = new FileOutputStream(binFile).getChannel();
            this.binData.rewind();
            outputChannel.write(this.binData);
            this.binData.flip();
            outputChannel.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ByteBuffer getBinData() {
        return this.binData;
    }
}
