package hr.terraforming.mars.terraformingmars.chat;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ChatServiceImpl implements ChatService{

    private final List<String> chatMessageHistory = new ArrayList<>();

    @Override
    public void sendChatMessage(String ChatMessage) throws RemoteException {
        chatMessageHistory.add(ChatMessage);
    }

    @Override
    public List<String> returnChatHistory() throws RemoteException {
    return chatMessageHistory;
    }
}
