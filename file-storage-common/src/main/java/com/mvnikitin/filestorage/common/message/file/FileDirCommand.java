package com.mvnikitin.filestorage.common.message.file;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class FileDirCommand extends FileAbstractCommand {
    private List<DirEntry> results;
    private String relativePath;

    public static class DirEntry implements Serializable {
        private String entryName;
        private boolean directory;
        private long size;
        private Date creationTime;
        private Date modified;

        public DirEntry(String entryName,
                        boolean directory,
                        long size,
                        Date creationTime,
                        Date modified) {
            this.entryName = entryName;
            this.directory = directory;
            this.size = size;
            this.creationTime = creationTime;
            this.modified = modified;
        }

        public String getEntryName() {
            return entryName;
        }

        public void setEntryName(String entryName) {
            this.entryName = entryName;
        }

        public boolean getDirectory() {
            return directory;
        }

        public void setDirectory(boolean directory) {
            this.directory = directory;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public Date getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(Date creationTime) {
            this.creationTime = creationTime;
        }

        public Date getModified() {
            return modified;
        }

        public void setModified(Date modified) {
            this.modified = modified;
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

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    @Override
    public String info() {
        return "get the list of files and folders";
    }
}
