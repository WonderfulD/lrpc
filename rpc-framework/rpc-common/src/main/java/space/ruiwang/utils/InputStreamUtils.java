package space.ruiwang.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public class InputStreamUtils {
    /**
     * 从输入流中读取所有字节
     *
     * @param inputStream 要读取的输入流
     * @return 包含所有读取字节的字节数组
     * @throws IOException 如果读取过程中发生I/O错误
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
