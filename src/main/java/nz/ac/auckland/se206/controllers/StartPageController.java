package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.SceneManager;
import nz.ac.auckland.se206.SceneManager.AppUi;

public class StartPageController {

  @FXML private Button playButton;
  @FXML private Button settingsButton;

  @FXML
  private void clickPlayButton(ActionEvent event) throws IOException {
    App.setRoot("room");
  }

  @FXML
  private void clickSettingsButton(ActionEvent event) throws IOException {

    SettingsPageController controller =
        (SettingsPageController) SceneManager.getController(AppUi.SETTINGS_PAGE);
    controller.setPreviousUi(AppUi.START_PAGE);

    App.setUi(AppUi.SETTINGS_PAGE);
  }
}