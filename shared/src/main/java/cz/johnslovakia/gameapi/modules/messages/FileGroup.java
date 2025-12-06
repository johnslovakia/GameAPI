package cz.johnslovakia.gameapi.modules.messages;

import lombok.Getter;

import java.io.InputStream;
import java.util.List;

@Getter
public class FileGroup {

    private final String name;
    private final List<InputStream> files;

    public FileGroup(String name, List<InputStream> files) {
        this.name = name;
        this.files = files;
    }

    public FileGroup(String name, InputStream... files) {
        this.name = name;
        this.files = List.of(files);
    }
}
