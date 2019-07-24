package DS.Util;

import DS.Basic.Const;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GsonUtils {
    public static <T> List<T> getList(List<ByteBuffer> byteBuffers, Class<T> cls) {

        List<T> list = new ArrayList<T>();
        for (int i = 0; i < byteBuffers.size(); i++) {
            list.add(fromJson(new String(byteBuffers.get(i).array(), Const.CHARSET), cls));
        }
        return list;
    }

    public static <T> T fromJson(String jsonString, Class<T> cls) {
        StringReader reader = new StringReader(jsonString);
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        return new Gson().fromJson(jsonReader, cls);
    }
}
