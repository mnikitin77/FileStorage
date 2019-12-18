package com.mvnikitin.filestorage.common.message.file;

import java.io.Serializable;
import java.util.List;

public class FileDirCommand extends FileAbstractCommand {
    private List<DirEntry> results;

    public static class DirEntry implements Serializable {
        private String entryName;
        private boolean isDirectory;

        public DirEntry(String entryName, boolean isDirectory) {
            this.entryName = entryName;
            this.isDirectory = isDirectory;
        }

        public String getEntryName() {
            return entryName;
        }

        public boolean isDirectory() {
            return isDirectory;
        }
    }

    public FileDirCommand() {
        super("");
    }

    public List<DirEntry> getResults() {
        return results;
    }

    public void setResults(List<DirEntry> results) {
        this.results = results;
    }

    @Override
    public void dummy() {
    }
}
