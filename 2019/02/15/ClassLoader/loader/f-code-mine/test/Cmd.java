package test;

import java.io.BufferedInputStream;
import java.io.IOException;

public class Cmd{
    private void exec(String cmd) throws Exception {
        StringBuilder result = new StringBuilder();
        int len;
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(Runtime.getRuntime().exec(cmd).getInputStream());
            while ((len = bufferedInputStream.read(buffer, 0, bufferSize)) != -1)
                result.append(new String(buffer, 0, len,"gbk"));
            throw new Exception(String.valueOf(result));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}