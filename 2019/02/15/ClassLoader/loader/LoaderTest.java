package loader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Java 1.8 or earlier version
 */
public class LoaderTest {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException, IOException {
        MClassLoader loader = new MClassLoader("F:/code/mine");
        Class cl = loader.loadClass("com.yue.test.Hello");
        Method say = cl.getDeclaredMethod("say");
        say.invoke(cl.newInstance());

        NewLoader loader1 = new NewLoader("F:/code/mine/test");
        Class test1 = loader1.getClass("test.Test");
        if (test1 != null) {
            Method testSay1 = test1.getDeclaredMethod("say");
            testSay1.invoke(test1.newInstance());
        }

        Thread th = new Thread(() -> {
            NewLoader loader2 = (NewLoader) Thread.currentThread().getContextClassLoader();
            try {
                Class test = loader2.getClass("test.Test");
                Method testSay = test.getDeclaredMethod("say");
                testSay.invoke(test.newInstance());
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
                e.printStackTrace();
            }
        });
        NewLoader loader3 = new NewLoader("F:/code/mine/test");
        th.setContextClassLoader(loader1);
        th.start();
    }
}
