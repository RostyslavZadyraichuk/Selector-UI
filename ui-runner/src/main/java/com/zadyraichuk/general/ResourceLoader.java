package com.zadyraichuk.general;

import java.io.*;

public class ResourceLoader {

    private ResourceLoader() {
    }

    public static File loadResource(String resourcePath, String localPath, String prefix, String suffix) {
        File localFile = new File(localPath + prefix + '.' + suffix);
        InputStream is = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            if (!localFile.exists()) {
                if (!localFile.getParentFile().exists()) {
                    localFile.getParentFile().mkdirs();
                }
                localFile.createNewFile();
            }

            is = ResourceLoader.class.getResourceAsStream(resourcePath);
            bis = new BufferedInputStream(is);
            fos = new FileOutputStream(localFile);
            bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[1024];
            while (bis.available() > 0) {
                int readBytes = bis.read(buffer);
                bos.write(buffer, 0, readBytes);
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Resource %s cannot be loaded", resourcePath), e);
        } finally {
            try {
                is.close();
                bis.close();
                fos.close();
                bos.close();
            } catch (IOException | NullPointerException ignored) {
            }
        }

        return localFile;
    }

}