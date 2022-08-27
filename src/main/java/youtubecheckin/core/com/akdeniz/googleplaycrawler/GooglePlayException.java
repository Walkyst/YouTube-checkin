package youtubecheckin.core.com.akdeniz.googleplaycrawler;

import youtubecheckin.core.com.akdeniz.googleplaycrawler.GooglePlay.ResponseWrapper;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

public class GooglePlayException extends IOException {
  private static final long serialVersionUID = 1L;

  private final int httpStatus;

  public GooglePlayException(int httpStatus, String message) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public static GooglePlayException create(HttpResponse httpResponse) {
    String message = httpResponse.getStatusLine().getReasonPhrase();

    // If the response contains a Protobuf response, retrieves the message from a
    // ResponseWrapper object
    InputStream content = null;

    try {
      content = httpResponse.getEntity().getContent();
      if ("application/protobuf".equals(httpResponse.getEntity().getContentType().getValue())) {
        ResponseWrapper rw = ResponseWrapper.parseFrom(content);

        if (rw.hasCommands() && rw.getCommands().hasDisplayErrorMessage()) {
          message = rw.getCommands().getDisplayErrorMessage();
        }
      }
    } catch (Exception ignored) {
    }

    try {
      content.close();
    } catch (IOException ignored) {
    }

    return new GooglePlayException(httpResponse.getStatusLine().getStatusCode(), message);
  }
}
