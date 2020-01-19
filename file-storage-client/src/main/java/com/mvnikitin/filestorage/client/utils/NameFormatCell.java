package com.mvnikitin.filestorage.client.utils;

import com.mvnikitin.filestorage.common.message.file.FileDirCommand;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class NameFormatCell extends TableCell<FileDirCommand.DirEntry, String> {
    private static final Image fileImage = new Image("/img/file.png");
    private static final Image folderImage = new Image("/img/folder.png");
    private ImageView fileImageView = new ImageView(fileImage);
    private ImageView folderImageView = new ImageView(folderImage);

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            int currentIndex = indexProperty()
                    .getValue() < 0 ? 0
                    : indexProperty().getValue();
            if (getTableView().getItems().get(currentIndex).getDirectory()) {
                setGraphic(folderImageView);
            } else {
                setGraphic(fileImageView);
            }
            setText(item);
        }
    }
}
