package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.chat.ChatService;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

@Slf4j
public class ChatManager {
    private ChatService chatService;

    private final ListView<String> chatListView;
    private final TextField chatInput;
    private final VBox chatBoxContainer;

    public ChatManager(ListView<String> chatListView, TextField chatInput, VBox chatBoxContainer) {
        this.chatListView = chatListView;
        this.chatInput = chatInput;
        this.chatBoxContainer = chatBoxContainer;
    }

    public void setupChatSystem(PlayerType playerType) {
        boolean isOnline = (playerType != PlayerType.LOCAL);

        if (chatBoxContainer != null) {
            chatBoxContainer.setVisible(isOnline);
            chatBoxContainer.setManaged(isOnline);
        }

        if (isOnline) {
            connectToChatService();
        }
    }

    private void connectToChatService() {
        try {
            String hostname = ConfigurationReader.getStringValue(ConfigurationKey.HOSTNAME);
            int rmiPort = ConfigurationReader.getIntegerValue(ConfigurationKey.RMI_PORT);

            Registry registry = LocateRegistry.getRegistry(hostname, rmiPort);
            chatService = (ChatService) registry.lookup(ChatService.REMOTE_OBJECT_NAME);

            log.info("Connected to chat service");
            startChatPolling();

        } catch (Exception e) {
            log.error("Failed to connect to chat", e);
        }
    }

    private void startChatPolling() {
        Timeline chatPoll = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            try {
                if (chatService != null) {
                    List<String> messages = chatService.returnChatHistory();
                    Platform.runLater(() -> {
                        if (chatListView != null) {
                            chatListView.getItems().clear();
                            chatListView.getItems().addAll(messages);
                        }
                    });
                }
            } catch (RemoteException e) {
                log.error("Chat polling error", e);
            }
        }));
        chatPoll.setCycleCount(Animation.INDEFINITE);
        chatPoll.play();
    }

    public void sendMessage(GameManager gameManager) {
        if (chatService == null || chatInput == null) return;

        try {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                String playerName = gameManager.getCurrentPlayer().getName();
                chatService.sendChatMessage(playerName + ": " + message);
                chatInput.clear();
            }
        } catch (RemoteException e) {
            log.error("Failed to send chat message", e);
        }
    }
}
