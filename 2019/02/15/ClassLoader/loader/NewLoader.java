package loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Java 1.8 or earlier version
 * 在当前目录下寻找class
 */
public class NewLoader extends ClassLoader {
    private String mLibPath;

    public NewLoader(String mLibPath) {
        this.mLibPath = mLibPath;
        System.out.println(getParent() + "   mine: " + mLibPath);
    }


  /*@Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // If still not found, then invoke findClass in order
                // to find the class.
                c = findClass(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }*/

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        try {
            byte[] b = loadClassData(name);
            if (b != null) {
                return defineClass(name, b, 0, b.length);//.....
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.findClass(name);
    }

    protected Class<?> getClass(String name) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        ClassLoader cl = getParent();

        Class<?> c = findLoadedClass(name);
        if (c != null) {
            System.out.println("have loaded ...");
            return c;
        }
        Method findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        findLoadedClassMethod.setAccessible(true);
        Class<?> c1 = (Class<?>) findLoadedClassMethod.invoke(cl, name);
        if (c1 != null) {
            System.out.println("parent have loaded ...");
            return c1;
        }

        //haven't load class ....
        byte[] b = loadClassData(name);
        Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClassMethod.setAccessible(true);
        return (Class<?>) defineClassMethod.invoke(cl, name, b, 0, b.length);
    }

    private byte[] loadClassData(String name) throws IOException {
        String fileName = getFileName(name);
        File resFile = new File(mLibPath, fileName);
        if (resFile != null) {
            System.err.println("loading ... " + resFile.getAbsolutePath());
            return Files.readAllBytes(Paths.get(resFile.getAbsolutePath()));
        }
        return null;
    }

    private String getFileName(String name) {
        int pos = name.lastIndexOf('.');
        if (pos == -1)
            return name + ".class";
        else
            return name.substring(pos + 1) + ".class";
    }
}
