package hr.terraforming.mars.terraformingmars.chat;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ChatServiceImpl implements ChatService{

    private final List<String> chatHistory = new ArrayList<>();

    @Override
    public void sendChatMessage(String chatMessage) throws RemoteException {
        chatHistory.add(chatMessage);
    }

    @Override
    public List<String> returnChatHistory() throws RemoteException {
    return chatHistory;
    }

    @Override
    public synchronized void clearChatHistory() throws RemoteException {
        chatHistory.clear();
    }
}
