package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.GameState;
import nz.ac.auckland.se206.SceneManager.AppUi;
import nz.ac.auckland.se206.gpt.ChatMessage;
import nz.ac.auckland.se206.gpt.GptPromptEngineering;
import nz.ac.auckland.se206.gpt.openai.ApiProxyException;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionRequest;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionResult;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionResult.Choice;
import nz.ac.auckland.se206.speech.TextToSpeech;

/** Controller class for the chat view. */
public class ChatController {

  private static Timeline timeline;
  private static final Integer START_TIME_MIN = 2;
  private static final Integer START_TIME_SEC = 00;

  public static void playTimer() {
    timeline.play();
  }

  public static void stopTimer() {
    timeline.stop();
  }

  @FXML private TextArea chatTextArea;
  @FXML private TextField inputText;
  @FXML private Label timerMinLabel;
  @FXML private Label timerSecLabel;
  @FXML private Button sendButton;
  @FXML private Button goBackButton;

  private ChatCompletionRequest chatCompletionRequest;
  private Integer timeMinutes = START_TIME_MIN;
  private Integer timeSeconds = START_TIME_SEC;

  /**
   * Initializes the chat view, loading the riddle.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    startTimer();

    Task<Void> initializeRiddleTask =
        new Task<Void>() {

          @Override
          protected Void call() throws Exception {

            // Edit AI behaviour here (temperature, topP, maxTokens)
            chatCompletionRequest =
                new ChatCompletionRequest()
                    .setN(1)
                    .setTemperature(1.2)
                    .setTopP(0.8)
                    .setMaxTokens(200);
            runGpt(new ChatMessage("user", GptPromptEngineering.getRiddleWithGivenWord("vinyl")));
            return null;
          }
        };

    Thread initializeRiddleThread = new Thread(initializeRiddleTask, "initializeRiddleThread");
    initializeRiddleThread.start();
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    if (msg.getRole().equals("user")) {
      chatTextArea.appendText("You: " + msg.getContent() + "\n\n");
    } else if (msg.getRole().equals("assistant")) {
      chatTextArea.appendText("Book: " + msg.getContent() + "\n\n");
    }
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws InterruptedException
   */
  public ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    chatCompletionRequest.addMessage(msg);

    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      chatCompletionRequest.addMessage(result.getChatMessage());
      appendChatMessage(result.getChatMessage());
      return result.getChatMessage();
    } catch (ApiProxyException e) {
      // TODO handle exception appropriately
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = inputText.getText();
    sendButton.setDisable(true); // disable send button while processing message

    if (message.trim().isEmpty()) { // if user sends empty message, do nothing
      sendButton.setDisable(false);
      goBackButton.setDisable(false);
      return;
    }

    Task<Void> onSendMessageTask =
        new Task<Void>() {

          @Override
          protected Void call() throws Exception {

            inputText.clear();
            ChatMessage msg = new ChatMessage("user", message);
            appendChatMessage(msg);
            ChatMessage lastMsg = runGpt(msg);
            // if the last message is from the assistant and starts with "Correct"
            if (lastMsg.getRole().equals("assistant")
                && lastMsg.getContent().startsWith("Correct")) {
              GameState.isRiddleResolved = true;
            }
            if (lastMsg.getRole().equals("assistant")) {
              Task<Void> textToSpeechTask =
                  new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                      TextToSpeech.main(new String[] {lastMsg.getContent()});
                      return null;
                    }
                  };
              Thread textToSpeechThread = new Thread(textToSpeechTask, "textToSpeechThread");
              textToSpeechThread.start();
            }
            return null;
          }
        };

    onSendMessageTask.setOnSucceeded(
        e -> {
          sendButton.setDisable(false);
        });

    onSendMessageTask.setOnFailed(
        e -> {
          sendButton.setDisable(false);
        });

    Thread onSendMessageThread = new Thread(onSendMessageTask, "onSendMessageThread");
    onSendMessageThread.start();
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   * @throws URISyntaxException
   */
  @FXML
  private void onGoBack(ActionEvent event)
      throws ApiProxyException, IOException, URISyntaxException {
    MediaPlayer bookClosingPlayer =
        new MediaPlayer(
            new Media(getClass().getResource("/sounds/bookClosing.mp3").toURI().toString()));
    bookClosingPlayer.play();
    App.setUi(AppUi.DARK_ROOM);
  }

  public String getChatText() {
    return chatTextArea.getText();
  }

  public void startTimer() {
    timerMinLabel.setText(timeMinutes.toString());
    timerSecLabel.setText(": 00");
    timeline = new Timeline(); // create a timeline for the timer
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline
        .getKeyFrames()
        .add(
            new KeyFrame(
                Duration.seconds(1), // handler is called every second
                new EventHandler<ActionEvent>() {
                  @Override
                  public void handle(ActionEvent event) {
                    timeSeconds--;
                    if (timeSeconds < 0
                        && timeMinutes > 0) { // decrement minutes if seconds reach 0
                      timeMinutes--;
                      timeSeconds = 59;
                    }
                    timerMinLabel.setText(timeMinutes.toString());
                    if (timeSeconds < 10) {
                      timerSecLabel.setText(": 0" + timeSeconds.toString()); // aesthetic purposes
                    } else {
                      timerSecLabel.setText(": " + timeSeconds.toString());
                    }
                    if (timeMinutes <= 0 && timeSeconds <= 0) {
                      timeline.stop();
                      try {
                        App.setRoot("endPage"); // go to end page if time runs out
                      } catch (IOException e) {
                        e.printStackTrace();
                      }
                    }
                  }
                }));
  }
}
