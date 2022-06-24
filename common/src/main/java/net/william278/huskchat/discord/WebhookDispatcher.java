package net.william278.huskchat.discord;

import net.william278.huskchat.config.Settings;
import net.william278.huskchat.message.ChatMessage;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WebhookDispatcher {

    private final Map<String, URL> channelWebhooks;

    private Optional<URL> getChannelWebhook(@NotNull String channelId) {
        if (channelWebhooks.containsKey(channelId)) {
            return Optional.of(channelWebhooks.get(channelId));
        }
        return Optional.empty();
    }

    public WebhookDispatcher(@NotNull Map<String, URL> channelWebhooks) {
        this.channelWebhooks = channelWebhooks;
    }

    public void dispatchWebhook(@NotNull ChatMessage message) {
        CompletableFuture.runAsync(() -> getChannelWebhook(message.targetChannelId).ifPresent(webhookUrl -> {
            try {
                final HttpURLConnection webhookConnection = (HttpURLConnection) webhookUrl.openConnection();
                webhookConnection.setRequestMethod("POST");
                webhookConnection.setDoOutput(true);

                final byte[] jsonMessage = getChatMessageJson(Settings.webhookMessageFormat, message);
                final int messageLength = jsonMessage.length;
                webhookConnection.setFixedLengthStreamingMode(messageLength);
                webhookConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                webhookConnection.connect();
                try (OutputStream messageOutputStream = webhookConnection.getOutputStream()) {
                    messageOutputStream.write(jsonMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private byte[] getChatMessageJson(@NotNull DiscordMessageFormat format, @NotNull ChatMessage message) {
        return format.postMessageFormat
                .replace("{SENDER_UUID}", message.sender.getUuid().toString())
                .replace("{SENDER_CHANNEL}", message.targetChannelId)
                .replace("{CURRENT_TIMESTAMP}", ZonedDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .replace("{SENDER_USERNAME}", message.sender.getName())
                .replace("{CHAT_MESSAGE}", message.message)
                .getBytes(StandardCharsets.UTF_8);
    }

}
