package org.example;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AppController implements Initializable {
    @FXML
    TableView<FileInfo> fileTable;
    @FXML
    ComboBox<String> diskSelect;
    @FXML
    TextField pathText;
    @FXML
    Button volumeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(252);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(152);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty){
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[Директория]";
                        }
                        setText(text);
                    }
                }
            };
        });

        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата создания");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDateOfCreation().toString()));
        filenameColumn.setPrefWidth(152);

        fileTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        fileTable.getSortOrder().add(fileTypeColumn);

        diskSelect.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            diskSelect.getItems().add(p.toString());
        }
        diskSelect.getSelectionModel().select(0);
        fileTable.setOnMouseClicked(new EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                if (event.getClickCount() == 2){
                    Path path = Paths.get(pathText.getText()).resolve(fileTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateList(path);
                    }
                }
            }
        });
        updateList(Paths.get("."));
    }
    public void updateList(Path path){
        try {
            pathText.setText(path.normalize().toAbsolutePath().toString());
            fileTable.getItems().clear();
            fileTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            fileTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось получить список файлов, проверьте правильность указанного пути", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnExitAction() {
        Platform.exit();
    }

    public void btnPathUpAction() {
        Path upPath = Paths.get(pathText.getText()).getParent();
        if (upPath != null) {
            updateList(upPath);
        }

    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }


    public void btnPathAction() {
        Path newPath = Paths.get(pathText.getText());
        if (newPath != null) {
            updateList(newPath);
        }
    }

    public void btnVolumeAction() {
        AtomicLong size = new AtomicLong(0);
        volumeButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 1){
                    if (getSelectedFilename() == null) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один элемент не был выбран", ButtonType.OK);
                        alert.showAndWait();
                        return;
                    }
                    String filePath = pathText.getText() + "\\" + getSelectedFilename();
                    Path path = Paths.get(filePath);
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                size.addAndGet(attrs.size());
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch(IOException e) {
                        throw new AssertionError();
                    }
                    String text = String.format("%,d bytes", size.get());
                    text = "Директория занимает: " + text;
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, text , ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });
    }

    public String getSelectedFilename() {
        if(!fileTable.isFocused()) {
            return null;
        }
        return fileTable.getSelectionModel().getSelectedItem().getFilename();
    }
    public void btnCreatorAction() {
        if (getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один элемент не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        String filePath = pathText.getText() + "\\" + getSelectedFilename();
        Path path = Paths.get(filePath);
        try {
            FileOwnerAttributeView file = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
            UserPrincipal user = file.getOwner();
            String owner = user.getName();
            owner = "Создатель: " + owner;
            Alert alert = new Alert(Alert.AlertType.INFORMATION, owner, ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось получить информацию об авторе", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnUpdateAction() {
        Path path = Paths.get(pathText.getText());
        updateList(path);
    }
}