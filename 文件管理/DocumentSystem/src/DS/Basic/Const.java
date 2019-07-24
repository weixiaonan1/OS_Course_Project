package DS.Basic;

import java.nio.charset.Charset;

public class Const {
    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final int BLOCK_SIZE = 1024;
    public static final int BLOCK_COUNT = 512;
    public static final int FREE = -2;
    public static final int FILE_END = -1;

}
