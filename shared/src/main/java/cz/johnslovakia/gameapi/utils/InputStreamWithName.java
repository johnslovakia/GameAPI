package cz.johnslovakia.gameapi.utils;

import java.io.InputStream;

public record InputStreamWithName(InputStream inputStream, String fileName) {
}