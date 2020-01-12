package com.mvnikitin.filestorage.client;

import com.mvnikitin.filestorage.client.utils.*;
import com.mvnikitin.filestorage.common.message.AbstractNetworkMessage;
import com.mvnikitin.filestorage.common.message.file.*;
import com.mvnikitin.filestorage.common.message.service.GetConfigInfoCommand;
import com.mvnikitin.filestorage.common.message.service.LogoffCommand;
import com.mvnikitin.filestorage.common.utils.FileCommandProcessUtils;
import com.mvnikitin.filestorage.common.utils.FileProcessData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    MenuBar menuBar;
    @FXML
    Label fileLbl, commandStatusLbl;
    @FXML
    TableView filesTableView;
    @FXML
    TableColumn<FileDirCommand.DirEntry, Boolean> isFolderCol;
    @FXML
    TableColumn<FileDirCommand.DirEntry, String> nameCol;
    @FXML
    TableColumn<FileDirCommand.DirEntry, Long> sizeCol;
    @FXML
    TableColumn<FileDirCommand.DirEntry, Date> createdCol;
    @FXML
    TableColumn<FileDirCommand.DirEntry, Date> modifiedCol;
    @FXML
    Button upBtn, openBtn, newFolderBtn, deleteBtn, renameBtn,
            uploadBtn, downloadBtn, refreshBtn;
    @FXML
    ProgressBar commandProg;

    private Stage stage;

    private ServerCommandSender serverCommandSender;

    private String relativePath;
    private boolean isRootFolder;
    private String lastSelectedFileName;

    private FileTransferParameters fileTransferParameters;

    private static Callback
            <TableColumn<FileDirCommand.DirEntry,String>,
            TableCell<FileDirCommand.DirEntry,String>>
            nameCellFactory;

    private static EventHandler
            <TableColumn.CellEditEvent<FileDirCommand.DirEntry, String>>
            nameCellDefaultOnEditCancel;

    public void setStage(Stage mainStage) {
        this.stage = mainStage;
        serverCommandSender.setOwner(mainStage);
    }

    // A class to pass the array and buffer size to the FileTransferCommand
    public static class FileTransferParameters implements FileProcessData {
        private String currentDirectory;
        private byte[] localArray;
        private int blockSize;

        public FileTransferParameters(int blockSize) {
            this.blockSize = blockSize;
            localArray = new byte[blockSize];
        }

        @Override
        public int getBlockSize() {
            return blockSize;
        }

        @Override
        public String getCurrentDirectory() {
            return currentDirectory;
        }

        @Override
        public byte[] getWorkArray() {
            return localArray;
        }

        @Override
        public void setCurrentDirectory(String path) {
            currentDirectory = path;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        serverCommandSender = new ServerCommandSender();

        isRootFolder = true;

        upBtn.setGraphic(new ImageView(new Image("/img/folder_16.png")));
        openBtn.setGraphic(new ImageView(new Image("/img/forward_16.png")));
        newFolderBtn.setGraphic(new ImageView(new Image("/img/new_folder_16.png")));
        deleteBtn.setGraphic(new ImageView(new Image("/img/delete_16.png")));
        renameBtn.setGraphic(new ImageView(new Image("/img/rename_16.png")));
        uploadBtn.setGraphic(new ImageView(new Image("/img/up_16.png")));
        downloadBtn.setGraphic(new ImageView(new Image("/img/down_16.png")));
        refreshBtn.setGraphic(new ImageView(new Image("/img/refresh_16.png")));
        refreshBtn.setAlignment(Pos.CENTER);

        isFolderCol.setCellValueFactory(new PropertyValueFactory<>("directory"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("entryName"));
        nameCol.setCellFactory(param -> new NameFormatCell());
        nameCellFactory = nameCol.getCellFactory();
        nameCellDefaultOnEditCancel = nameCol.getOnEditCancel();

        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeCol.setCellFactory(param -> {
            SizeFormatCell cell = new SizeFormatCell();
            cell.setAlignment(Pos.TOP_RIGHT);
            return cell;
        });

        createdCol.setCellValueFactory(new PropertyValueFactory<>("creationTime"));
        createdCol.setCellFactory(param -> {
            DateFormatCell cell = new DateFormatCell();
            cell.setAlignment(Pos.TOP_CENTER);
            return cell;
        });

        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modified"));
        modifiedCol.setCellFactory(param -> {
            DateFormatCell cell = new DateFormatCell();
            cell.setAlignment(Pos.TOP_CENTER);
            return cell;
        });

        filesTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        FileDirCommand.DirEntry entry =
                                (FileDirCommand.DirEntry) newValue;
                        lastSelectedFileName = entry.getEntryName();

                        commandStatusLbl.setText("");
                        commandProg.setProgress(0);

                        updateUIControlsState(entry);
                    }
                });

        isFolderCol.setSortType(TableColumn.SortType.DESCENDING);
        nameCol.setSortType(TableColumn.SortType.ASCENDING);

        filesTableView.getSortOrder().addAll(isFolderCol, nameCol);
        filesTableView.setEditable(true);
        filesTableView.getSelectionModel().setCellSelectionEnabled(true);

    // Filling the table view with the content.
        fillFilesTableViewWithServerData();
        updateUIControlsState(null);

    // Init file transfer parameters - buffer size and buffer array.
    // An instance of FileTransferParameters is created here.
        initFileTransferParameters();
    }

    @FXML
    public void handleKeyPressedOnButton(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            ((Button)keyEvent.getSource()).fire();
        }
    }

    @FXML
    public void handleKeyPressedOnTableView(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                FileDirCommand.DirEntry entry =
                        (FileDirCommand.DirEntry) (
                                (TableView)keyEvent
                                        .getSource())
                                .getSelectionModel()
                                .getSelectedItem();
                if(entry.getDirectory()) {
                    openBtn.fire();
                }
                break;
            case DELETE:
                deleteBtn.fire();
                break;
            case F5:
                refreshBtn.fire();
                break;
            default:
        }
    }

    @FXML
    public void handleMouseClickedOnTableView(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY &&
                mouseEvent.getClickCount() == 2) {
            FileDirCommand.DirEntry entry =
                    (FileDirCommand.DirEntry) (
                            (TableView)mouseEvent
                                    .getSource())
                            .getSelectionModel()
                            .getSelectedItem();
            if(entry.getDirectory()) {
                openBtn.fire();
            }
        }
    }

    @FXML
    public void exitApplication(ActionEvent actionEvent) {
        doLogoff();

        NetworkManager.stop();
        Platform.exit();
    }

    @FXML
    public void showAbout(ActionEvent actionEvent) {
        UserNotifier.showInfoMessage("About",
                "Net Memory application.",
                "Maxim Nikitin 2019",
                stage);
    }

    @FXML
    public void logoff(ActionEvent actionEvent) {
        if (doLogoff()) {
        // Switch to the application logon screen
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/logon.fxml"));
                Scene logonScene = new Scene(root);
                Stage stage = (Stage) menuBar.getScene().getWindow();
                stage.hide();
                stage.setScene(logonScene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleUp(ActionEvent actionEvent) {
        changeFolder(null);
    }

    @FXML
    public void handleOpen(ActionEvent actionEvent) {
        changeFolder(lastSelectedFileName);
    }

    @FXML
    public void handleNew(ActionEvent actionEvent) {
        ObservableList<FileDirCommand.DirEntry> fileList =
                filesTableView.getItems();
        Date current = new Date();
        fileList.add(0, new FileDirCommand.DirEntry(
                "New Folder", true, 0, current, current));

        filesTableView.getSelectionModel().clearAndSelect(0, nameCol);
        TablePosition position = (TablePosition) filesTableView
                .getSelectionModel()
                .getSelectedCells()
                .get(0);

        disableButtons();

        newOrRename(position, true);

        nameCol.setOnEditCancel(event -> {
            nameCol.setCellFactory(nameCellFactory);
            fileList.remove(0);
        // Set initial onEditCancel in order to not to go on removing the filelist.
            nameCol.setOnEditCancel(nameCellDefaultOnEditCancel);
            updateUIControlsState(null);
        });
    }

    @FXML
    public void handleRename(ActionEvent actionEvent) {
        FileDirCommand.DirEntry item =
                (FileDirCommand.DirEntry) filesTableView
                        .getSelectionModel()
                        .getSelectedItem();
        if (item != null) {
            TablePosition position = (TablePosition) filesTableView
                    .getSelectionModel()
                    .getSelectedCells()
                    .get(0);

            disableButtons();

            filesTableView.getSelectionModel().clearAndSelect(
                    position.getRow(), nameCol);
            filesTableView.scrollTo(
                    filesTableView.getSelectionModel().getSelectedIndex());

            newOrRename(position, false);

            nameCol.setOnEditCancel(event -> {
                nameCol.setCellFactory(nameCellFactory);
                nameCol.setOnEditCancel(nameCellDefaultOnEditCancel);
                updateUIControlsState(null);
            });
        }
    }

    @FXML
    public void handleDelete(ActionEvent actionEvent) {
        AbstractNetworkMessage msg = new
                FileDeleteCommand(lastSelectedFileName);
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
            FileDeleteCommand delCmd = (FileDeleteCommand) msg;
            if (delCmd.isDeleted()) {
                // Repopulate the table content
                fillFilesTableViewWithServerData();
                updateUIControlsState(null);
            } else {
                UserNotifier.showInfoMessage("Information",
                        "Unable to delete.",
                        lastSelectedFileName,
                        stage);
            }
        }
    }

    @FXML
    public void handleUpload(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pick a file to trafnser");
        final File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            Task<Void> task = new Task() {
                @Override
                protected String call() throws Exception {
                long fileSize = file.length();
                String shortFileName = file.getName();

                fileTransferParameters.setCurrentDirectory(file.getParent());

            // Send a file by blocks of blockSize parts.
                AbstractNetworkMessage msg = new FIleTransferCommand(
                        shortFileName,
                        true,
                        fileTransferParameters.getBlockSize());

                long interationsCount =
                        (fileSize % fileTransferParameters.getBlockSize() == 0) ?
                                (fileSize / fileTransferParameters.getBlockSize()) :
                                (fileSize / fileTransferParameters.getBlockSize()) + 1;
                long workDone = 0;

                for (long i = 0; i < interationsCount; i++) {
                    FIleTransferCommand command = (FIleTransferCommand) msg;
                    command.setIsOnClient(true);
                    FileCommandProcessUtils.execute(command,
                            fileTransferParameters);

                    if (command.getData() != null) {

                        NetworkManager.sendMsg(msg);

                        workDone += command.getData().length;
                        updateProgress(workDone, fileSize);
                        updateMessage("[" + shortFileName + "]: " +
                                workDone + " bytes of " +
                                fileSize + " transferred.");

                        if (command.getCurrentPartNumber() ==
                                command.getPartsCount()) {
                            updateMessage("[" + shortFileName +
                                    "] is uploadad successfully.");
                            NetworkManager.readObject();
                            break;
                        }

                        msg = NetworkManager.readObject();
                    } else {
                        // Upload is complete.
                        break;
                    }
                }

                return null;
                }
            };

            task.setOnSucceeded(event -> {
                commandProg.progressProperty().unbind();
                commandStatusLbl.textProperty().unbind();
                // Update the table to show the recently uploaded file.
                fillFilesTableViewWithServerData();
            });

            task.setOnFailed(event -> {
                commandProg.progressProperty().unbind();
                commandStatusLbl.textProperty().unbind();
                UserNotifier.showErrorMessage("File Transfer Error",
                        "Error when uploading a file " + file.getName() + ".",
                        task.getException().getMessage(),
                        stage);
            });

            // Bind the sources of the task information with the UI controls.
            commandStatusLbl.textProperty().bind(task.messageProperty());
            commandProg.progressProperty().bind(task.progressProperty());

            new Thread(task).start();
        }
    }

    @FXML
    public void handleDownload(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Pick a folder to save the file");

        String saveFolder = ClientProperties.getInstance().getSaveFolder();
        if (saveFolder == null || saveFolder.equals("")) {
            directoryChooser.setInitialDirectory(
                    FileSystemView.getFileSystemView().getDefaultDirectory());
        } else {
            directoryChooser.setInitialDirectory(
                    new File(ClientProperties.getInstance().getSaveFolder()));
        }
        File folder = directoryChooser.showDialog(stage);

        if (folder != null) {

            FileDirCommand.DirEntry item =
                    (FileDirCommand.DirEntry) filesTableView
                            .getSelectionModel()
                            .getSelectedItem();

            long fileSize = item.getSize();
            String fileName = item.getEntryName();

            Task<Void> task = new Task() {
                @Override
                protected String call() throws Exception {

                fileTransferParameters.setCurrentDirectory(folder.toString());

                // Download a file by blocks of blockSize parts.
                AbstractNetworkMessage msg = new FIleTransferCommand(
                        fileName,
                        false,
                        fileTransferParameters.getBlockSize());

                long interationsCount =
                        (fileSize % fileTransferParameters.getBlockSize() == 0) ?
                                (fileSize / fileTransferParameters.getBlockSize()) :
                                (fileSize / fileTransferParameters.getBlockSize()) + 1;


                FIleTransferCommand command = (FIleTransferCommand) msg;
                long workDone = 0;

                for (long i = 0; i < interationsCount; i++) {
                    command.setIsOnClient(true);
                    command = (FIleTransferCommand) serverCommandSender
                            .sendMessage(command);

                    if (command == null || command.getData() == null) {
                        break;
                    } else {
                        workDone += command.getData().length;
                        updateProgress(workDone, fileSize);
                        updateMessage("[" + fileName + "]: " +
                                workDone + " bytes of " +
                                fileSize + " transferred.");

                        command.setIsOnClient(true);
                        FileCommandProcessUtils.execute(command,
                                fileTransferParameters);

                        if (command.getCurrentPartNumber() ==
                                command.getPartsCount()) {
                            updateMessage("File " + fileName +
                                    " is uploadad successfully.");
                            break;
                        }
                    }
                }

                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                commandProg.progressProperty().unbind();
                commandStatusLbl.textProperty().unbind();

                // Save the last folder to store a downloading file
                ClientProperties.getInstance().setSaveFolder(folder.toString());
                try {
                    ClientProperties.getInstance().saveProperties();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            task.setOnFailed(event -> {
                commandProg.progressProperty().unbind();
                commandStatusLbl.textProperty().unbind();
                UserNotifier.showErrorMessage("File Transfer Error",
                        "Error when downloading a file " + fileName + ".",
                        task.getException().getMessage(),
                        stage);
            });

            // Bind the sources of the task information with the UI controls.
            commandStatusLbl.textProperty().bind(task.messageProperty());
            commandProg.progressProperty().bind(task.progressProperty());

            new Thread(task).start();
        }
    }

    @FXML
    public void handleRefresh(ActionEvent actionEvent) {
        fillFilesTableViewWithServerData();
    }

    private void initFileTransferParameters() {
        AbstractNetworkMessage msg = new GetConfigInfoCommand();
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
            fileTransferParameters = new FileTransferParameters(
                    ((GetConfigInfoCommand)msg).getBlockSize());
        }
    }

    private boolean doLogoff() {
        AbstractNetworkMessage msg = new LogoffCommand();
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
            return true;
        }
        return false;
    }

    private void fillFilesTableViewWithServerData() {
        ObservableList<FileDirCommand.DirEntry> fileList =
                FXCollections.observableArrayList();

        AbstractNetworkMessage msg = new FileDirCommand();
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
            FileDirCommand cmd = (FileDirCommand)msg;

            fileList.addAll(cmd.getResults());
            filesTableView.setItems(fileList);
            filesTableView.getSortOrder().addAll(isFolderCol, nameCol);

        // Select and focus the upper left visible cell.
            setFocusOnTreeView();
            filesTableView.getSelectionModel().clearAndSelect(0, nameCol);
            filesTableView.getFocusModel().focus(0, nameCol);

            relativePath = cmd.getRelativePath();
            if (relativePath != null) {
                isRootFolder = relativePath.equals("");
            }
        }
    }

    private void changeFolder(String name) {
        AbstractNetworkMessage msg = new FileChangeDirCommand(name);
        msg = serverCommandSender.sendMessage(msg);
        if (msg != null) {
        // Repopulate the table content
            fillFilesTableViewWithServerData();
            updateUIControlsState(null);
        }
    }

    private void newOrRename(final TablePosition position, boolean isNew) {

        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());

        filesTableView.requestFocus();
        filesTableView.edit(position.getRow(), nameCol);

        nameCol.setOnEditCommit(event -> {
            AbstractNetworkMessage msg = null;
            if (isNew) {
                msg = new FileMakeDirCommand(event.getNewValue());
            } else {
                msg = new FileRenameCommand(
                        event.getOldValue(),
                        event.getNewValue());
            }

            msg = serverCommandSender.sendMessage(msg);
            if (msg != null) {
            // Repopulate the table content
                fillFilesTableViewWithServerData();
                filesTableView.getSelectionModel().clearAndSelect(
                        event.getTablePosition().getRow(),
                        nameCol);
                filesTableView.scrollTo(
                        filesTableView.getSelectionModel().getSelectedIndex());
                updateUIControlsState(null);
            }

            nameCol.setCellFactory(nameCellFactory);
        });
    }

    private void updateUIControlsState(FileDirCommand.DirEntry entry) {
        FileDirCommand.DirEntry item = entry;
        if (item == null) {
            item = (FileDirCommand.DirEntry) filesTableView
                        .getSelectionModel()
                        .getSelectedItem();
        }

        if (item != null) {
        // An item is selected
            fileLbl.setText(relativePath + "\\" +
                    item.getEntryName());

            deleteBtn.setDisable(false);
            renameBtn.setDisable(false);

            if (item.getDirectory()) {
                openBtn.setDisable(false);
                downloadBtn.setDisable(true);
            } else {
                openBtn.setDisable(true);
                downloadBtn.setDisable(false);
            }
        } else {
        // No item is selected or the table is empty
            fileLbl.setText(relativePath + "\\");
            deleteBtn.setDisable(true);
            renameBtn.setDisable(true);
            openBtn.setDisable(true);
            downloadBtn.setDisable(true);
        }

        // Always
        newFolderBtn.setDisable(false);
        uploadBtn.setDisable(false);
        refreshBtn.setDisable(false);
        if (isRootFolder) {
            upBtn.setDisable(true);
        } else {
            upBtn.setDisable(false);
        }
    }

    private void disableButtons() {
        upBtn.setDisable(true);
        openBtn.setDisable(true);
        newFolderBtn.setDisable(true);
        deleteBtn.setDisable(true);
        renameBtn.setDisable(true);
        uploadBtn.setDisable(true);
        downloadBtn.setDisable(true);
        refreshBtn.setDisable(true);
    }

    private void setFocusOnTreeView() {
        Platform.runLater(() -> filesTableView.requestFocus());
    }
}