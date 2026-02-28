package com.familywishes.config;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import java.io.IOException;

class GmailConfigTest {

  private final GmailConfig gmailConfig = new GmailConfig();

  private HttpResponseException buildHttpResponseException(String responseContent)
      throws IOException {
    HttpTransport transport = new MockHttpTransport();
    var requestFactory = transport.createRequestFactory();
    var request =
        requestFactory.buildGetRequest(
            new com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"));

    return new HttpResponseException.Builder(400, "Bad Request", request.getHeaders())
        .setContent(responseContent)
        .build();
  }
}
