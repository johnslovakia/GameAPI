package cz.johnslovakia.gameapi.utils;

import java.io.InputStream;

public class InputStreamWithName {
    private final InputStream inputStream;
    private final String fileName;

    public InputStreamWithName(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getFileName() {
        return fileName;
    }
}