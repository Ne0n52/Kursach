package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
    public enum FileType{
        FILE("F"), DIRECTORY("D");
        private String name;

        public String getName() {
            return name;
        }
        FileType(String name) {
            this.name = name;
        }
    }
    private String filename;
    private FileType type;
    private String dateOfCreation;
    private long size;

    public String getFilename() {
        return filename;
    }

    public FileType getType() {
        return type;
    }

    public String getDateOfCreation() {
        return dateOfCreation;
    }

    public long getSize() {
        return size;
    }

    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.dateOfCreation = Files.getAttribute(path, "creationTime").toString();
        } catch (IOException e) {
            throw new RuntimeException("Невозможно получить информацию о файле!");
        }
    }

}
