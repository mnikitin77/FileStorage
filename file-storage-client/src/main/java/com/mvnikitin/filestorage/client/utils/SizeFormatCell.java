package com.mvnikitin.filestorage.client.utils;

import com.mvnikitin.filestorage.common.message.file.FileDirCommand;
import javafx.scene.control.TableCell;

public class SizeFormatCell extends TableCell<FileDirCommand.DirEntry, Long> {
    @Override
    protected void updateItem(Long item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item == 0 ? "" : item.toString());
        }
    }
}
