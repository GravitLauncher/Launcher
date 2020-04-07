package pro.gravit.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.UniversalJsonAdapter;

public class SerializeTest {
    public static GsonBuilder builder;
    public static Gson gson;
    public static ProviderMap<TestInterface> map;

    @BeforeAll
    public static void prepare() {
        builder = new GsonBuilder();
        map = new ProviderMap<>();
        map.register("test", MyTestClass.class);
        map.register("test2", MyTestClass2.class);
        builder.registerTypeAdapter(TestInterface.class, new UniversalJsonAdapter<>(map));
        gson = builder.create();
    }

    @Test
    public void main() {
        Assertions.assertNotNull(gson);
        String json = gson.toJson(new MyTestClass("AAAA"), TestInterface.class);
        String json2 = gson.toJson(new MyTestClass2("BBBB"), TestInterface.class);
        TestInterface test1 = gson.fromJson(json, TestInterface.class);
        TestInterface test2 = gson.fromJson(json2, TestInterface.class);
        Assertions.assertEquals(test1.get(), "AAAA");
        Assertions.assertEquals(test2.get(), "BBBB");
    }

    public interface TestInterface {
        String get();
    }

    public static class MyTestClass implements TestInterface {
        public final String a;

        public MyTestClass(String a) {
            this.a = a;
        }

        @Override
        public String get() {
            return a;
        }
    }

    public static class MyTestClass2 implements TestInterface {
        public final String b;

        public MyTestClass2(String a) {
            this.b = a;
        }

        @Override
        public String get() {
            return b;
        }
    }
}
