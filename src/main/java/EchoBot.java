import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class EchoBot extends TelegramLongPollingBot {

  public static void main(String[] args) {
    try {
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      botsApi.registerBot(new EchoBot());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onUpdateReceived(Update update) {
    // Ist das Update eine Message? (oder eine bearbeite Message, oder einen Poll)
    if (update.hasMessage()) {
      Message message = update.getMessage();

      // Ist die Message ein Text? (oder Bild, oder Audio)
      if (message.hasText()) {
        String text = message.getText();

        // Eine Antwort bauen:
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(text);
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setReplyToMessageId(message.getMessageId());

        // Antwort senden:
        try {
          execute(sendMessage);
        } catch (TelegramApiException e) {
          e.printStackTrace();
        }

      }
    }
  }

  @Override
  public String getBotUsername() {
    return "Jenny's EchoBot";
  }

  @Override
  public String getBotToken() {
    // TODO: Level 1: Hier noch API-Key einfuegen!
    // TODO: Level 2: API-Key in einer Datei speichen und nicht im Java Code
    return "";
  }
}
