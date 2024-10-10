

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Scanner;

public class YouTubeLivestreamScheduler {
  private static final String CLIENT_SECRETS = "client_secrets.json";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String APPLICATION_NAME = "YouTube Livestream Scheduler";
  private static final Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) {
    try {
      final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      Credential credential = authorize(httpTransport);
      YouTube youtube = new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
              .setApplicationName(APPLICATION_NAME)
              .build();

      // Get user input
      System.out.print("Enter the title of the livestream: ");
      String title = scanner.nextLine();

      System.out.print("Enter the start time (YYYY-MM-DD HH:MM:SS): ");
      String startTimeStr = scanner.nextLine();
      DateTime startTime = parseDateTime(startTimeStr);

      System.out.print("Enter the path to the thumbnail image: ");
      String thumbnailPath = scanner.nextLine();

      // Create livestream
      LiveBroadcast broadcast = createLiveBroadcast(youtube, title, startTime);
      System.out.println("Livestream created with ID: " + broadcast.getId());

      // Upload thumbnail
      uploadThumbnail(youtube, broadcast.getId(), thumbnailPath);

    } catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
    }
  }

  private static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
    InputStream in = new FileInputStream(CLIENT_SECRETS);
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, Collections.singletonList("https://www.googleapis.com/auth/youtube.force-ssl"))
            .build();

    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  private static LiveBroadcast createLiveBroadcast(YouTube youtube, String title, DateTime startTime) throws IOException {
    LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet()
            .setTitle(title)
            .setScheduledStartTime(startTime);

    LiveBroadcastStatus status = new LiveBroadcastStatus()
            .setPrivacyStatus("private");

    LiveBroadcast broadcast = new LiveBroadcast()
            .setSnippet(broadcastSnippet)
            .setStatus(status);

    return youtube.liveBroadcasts()
            .insert("snippet,status", broadcast)
            .execute();
  }

  private static void uploadThumbnail(YouTube youtube, String videoId, String thumbnailPath) throws IOException {
    InputStreamContent mediaContent = new InputStreamContent(
            "image/jpeg", new FileInputStream(thumbnailPath));

    youtube.thumbnails()
            .set(videoId, mediaContent)
            .execute();
    System.out.println("Thumbnail uploaded successfully.");
  }

  private static DateTime parseDateTime(String dateTimeStr) {
    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    return new DateTime(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
  }
}