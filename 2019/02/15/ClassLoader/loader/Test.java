package loader;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
//        File file=new File("f:/code/mine");
//        System.out.println(file.isDirectory()+" "+file.toURI());
        
        // created by define Class
        byte[] bytes = Files.readAllBytes(Paths.get("f:/code/mine/test/Cmd.class"));
        Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClassMethod.setAccessible(true);
        Class cmdClass= (Class) defineClassMethod.invoke(Test.class.getClassLoader(), "test.Cmd",bytes,0,bytes.length);

        Method execMethod1 = cmdClass.getDeclaredMethod("exec", String.class);
        execMethod1.setAccessible(true);

        execMethod1.invoke(cmdClass.newInstance(),"ipconfig /all");


        // created by load Class
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{new URL("file:/f:/code/mine/")}, ClassLoader.getSystemClassLoader());
        Class cl = urlClassLoader.loadClass("test.Cmd");
        Method execMethod = cl.getDeclaredMethod("exec", String.class);
        execMethod.setAccessible(true);

        execMethod.invoke(cl.newInstance(), "ipconfig /all");


    }
}

