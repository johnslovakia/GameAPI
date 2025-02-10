package cz.johnslovakia.gameapi.utils;

import lombok.Getter;

import java.io.InputStream;

@Getter
public class InputStreamWithName {
    private final InputStream inputStream;
    private final String fileName;

    public InputStreamWithName(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

}