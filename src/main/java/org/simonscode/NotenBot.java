package org.simonscode;

import static org.simonscode.NotenAPI.getUsernameFilename;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class NotenBot extends TelegramLongPollingBot {

  public static void main(String[] args) {
    System.setProperty("jsse.enableSNIExtension", "false");
    try {
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      botsApi.registerBot(new NotenBot());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  public NotenBot() {
    Timer timer = new Timer("check-grades", true);
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            try {
              System.out.println("Checking grades...");
              Files.list(Path.of(System.getProperty("user.dir")))
                  .filter(p -> p.toString().endsWith(".username"))
                  .forEach(path -> Stream.of(path)
                      .map(p -> {
                        try {
                          return Files.readString(p);
                        } catch (IOException e) {
                          e.printStackTrace();
                          return null;
                        }
                      })
                      .filter(Objects::nonNull)
                      .map(u -> {
                        try {
                          System.out.println("Checking grades for " + u + "...");
                          return new NotenAPI().get_new_entries(u);
                        } catch (IOException e) {
                          e.printStackTrace();
                          return null;
                        }
                      })
                      .filter(Objects::nonNull)
                      .filter(p -> !p.isEmpty())
                      .forEach(changes -> {
                        String id = path.toFile().getName().split("\\.")[0];
                        System.out.println(id + " : " + changes);

                        StringBuilder sb = new StringBuilder();
                        sb.append("Neue Note:\n");
                        for (Entry<String, Double> entry : changes.entrySet()) {
                          sb.append(entry.getKey());
                          sb.append(" : ");
                          sb.append(entry.getValue());
                          sb.append("\n");
                        }

                        exec(new SendMessage(id, sb.toString()));
                      }));
              System.out.println("Checking grades: Done!");
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        },
        Duration.ofSeconds(5).toMillis(),
        Duration.ofHours(1).toMillis()
    );
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      Message message = update.getMessage();
      if (message.hasText()) {
        if (message.getText().startsWith("/start")) {
          SendMessage sendMessage = new SendMessage(
              String.valueOf(message.getChatId()),
              "Willkommen!\n" +
                  "Du kannst den Befehl:\n" +
                  "  /login [benutzername] [passwort]\n" +
                  "(ohne Klammern) verwenden,\n" +
                  "um Benachrichtigungen von neuen Noten zu aktivieren.\n" +
                  "\n" +
                  "Sobald eine neue Note verfügbar ist,\n" +
                  "erhälst du eine Nachricht mit der Note.\n" +
                  "\n" +
                  "Zum Anhalten des Bots kannst du /stop verwenden,\n"
                  + "dann wird der Bot alle deine Daten löschen.\n"
          );
          exec(sendMessage);
        }
        if (message.getText().startsWith("/login")) {
          String[] parts = message.getText().split(" ");

          System.out.println("Got login request from " + message.getFrom().getFirstName());
          String reply;
          if (new NotenAPI().init(parts[1], parts[2])) {
            reply = "Login erfolgreich!\n"
                + "Du wirst ab nun von neuen Noten benachrichtigt!";
            System.out.println("Login success for " + message.getFrom().getFirstName());

            try {
              Files.writeString(Path.of(getUsernameFilename(message.getFrom().getId().toString())), parts[1]);
            } catch (IOException e) {
              e.printStackTrace();
            }
            exec(new DeleteMessage(String.valueOf(message.getChatId()), message.getMessageId()));
          } else {
            reply = "Login fehlgeschlagen!\n"
                + "Bitte melde dich bei Simon.";
            System.out.println("Login fail for " + message.getFrom().getFirstName());
          }

          exec(new SendMessage(String.valueOf(message.getChatId()), reply));
        } else if (message.getText().equals("/stop")) {
          System.out.println("Got delete request from " + message.getFrom().getFirstName());
          boolean deleted = false;
          try {
            String username = Files.readString(Path.of(getUsernameFilename(message.getFrom().getId().toString())));
            deleted = new File(NotenAPI.getGradesFilename(username)).delete();
            deleted &= Path.of(getUsernameFilename(message.getFrom().getId().toString())).toFile().delete();
          } catch (IOException e) {
            e.printStackTrace();
          }
          exec(new SendMessage(message.getChatId().toString(),
              "Deine Daten wurden " + (deleted ? "erfolgreich " : "") + "geloescht!"));
        }
      }
    }
  }

  private <T extends Serializable> void exec(BotApiMethod<T> method) {
    try {
      execute(method);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getBotUsername() {
    return "NotenBot";
  }

  @Override
  public String getBotToken() {
    return System.getenv("BOTTOKEN");
  }
}
