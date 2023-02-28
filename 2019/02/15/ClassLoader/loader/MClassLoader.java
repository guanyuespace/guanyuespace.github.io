package loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Java 1.8 or earlier version
 */
public class MClassLoader extends ClassLoader {
    private String mLibPath;

    public MClassLoader(String mLibPath) {
        this.mLibPath = mLibPath;
        System.out.println(getParent()+"   mine: "+mLibPath);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] b = loadClassData(name);
            if (b != null) {
                return defineClass(name, b, 0, b.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.findClass(name);
    }

    private byte[] loadClassData(String name) throws IOException {
        String fileName = getFileName(name);
        File resFile = new File(mLibPath, fileName);
        if (resFile != null) {
            System.err.println("loading ... "+resFile.getAbsolutePath());
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
