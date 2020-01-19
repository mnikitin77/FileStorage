package com.mvnikitin.filestorage.client.utils;

import com.mvnikitin.filestorage.common.message.file.FileDirCommand;
import javafx.scene.control.TableCell;

import java.text.DateFormat;
import java.util.Date;

public class DateFormatCell extends TableCell<FileDirCommand.DirEntry, Date> {
    @Override
    protected void updateItem(Date item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item == null ? "" :
                    DateFormat.getDateTimeInstance().format(item));
        }
    }
}
