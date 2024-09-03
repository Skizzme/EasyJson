import com.google.gson.JsonObject;
import me.skizzme.easyjson.EasyJson;
import me.skizzme.easyjson.annotation.JsonPropertyField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        TestObj obj = new TestObj(0, "onetwo");
        for (int i = 0; i < 20; i++) {
            obj.children.put(i, new TestObj(i, "child_" + i*1023));
        }
        JsonObject serialized = EasyJson.serialize(obj);
        System.out.println(serialized);
        TestObj deobj = EasyJson.deserialize(serialized, new TestObj());
        System.out.println(deobj.test + ", " + deobj.test2);
        for (Map.Entry<Integer, TestObj> c_obj : deobj.children.entrySet()) {
            System.out.println("C:" + c_obj.getKey() + ", " + c_obj.getValue().test + ", " + c_obj.getValue().test2);
        }
    }

    public static class TestObj {
        @JsonPropertyField(name = "test")
        public int test;
        @JsonPropertyField(name = "test_2")
        public String test2;
        @JsonPropertyField(name = "childs")
        public HashMap<Integer, TestObj> children = new HashMap<>();

        public TestObj() {

        }

        public TestObj(int test, String test2) {
            this.test = test;
            this.test2 = test2;
        }
    }

}
