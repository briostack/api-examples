package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.hc.core5.http.HttpStatus.*;


/**
 * This sample application demonstrates how to create a webhook resource. A webhoook
 * specifies a URL that is to receive a POST request whenever the configured event(s)
 * occurred. The content of the POST contains information about the resource involved
 * as well as the user who triggered the event.
 * <p>
 * Once the webhook is created, you can use the Briostack application or the API to
 * trigger the event.
 * </p>
 */
public class Webhook {

  //  for demonstration purposes, use the freemium "webhook.site" app as our webhook target
  static final String WEBHOOKURL = "https://webhook.site/cddb22e3-9846-41c1-bb13-60f949ec9c72";

  //  this sample uses Google's Gson to convert Java types
  //  to and from JSON, but there are alternatives, such as
  //  Jackson's ObjectMapper and others
  static final Gson GSON = new GsonBuilder()
      //  try to parse 'number' as Long first; else Double
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .create();

  //  to create a webhook, your api-key must be associated
  // with a user assigned to the Administrators security group
  static String apikey = "your-api-key-here";
  static String baseURL;

  /**
   * @param args optional; apikey and base URL
   */
  @SuppressWarnings("java:S106")  // use of System.println()
  public static void main(String[] args) {

    String instance = "apisandbox";  // subscriber instance name
    baseURL = "https://" + instance + ".briostack.io/rest/v1";

    if (args.length > 0) {
      apikey = args[0];
    }
    if (args.length > 1) {
      baseURL = args[1];
    }

    try {

      //  POST on collection /webhooks to create one
      var request = new HttpPost(baseURL + "/webhooks");
      request.setHeader("Accept", "application/json");
      //  set the mandatory x-api-key HTTP header
      request.setHeader("x-api-key", apikey);

      try (var httpClient = HttpClients.createDefault()) {

        //  create the request content as a HashMap
        var webhook = new HashMap<String, Object>();
        webhook.put("name", "Sample Webhook");
        //  the URL that will receive a POST when an event occurs
        webhook.put("url", WEBHOOKURL);
        //  the event(s) we want to trigger the webhook
        webhook.put("events", List.of("WHEVENT_LEAD_CREATE", "WHEVENT_LEAD_CHANGE"));
        //  set retry parameters, if desired
        webhook.put("retries", 2L);
        webhook.put("retryInterval", 3600L);  // 3600 seconds == one hour

        //  convert the HashMap to JSON as required
        request.setEntity(new StringEntity(GSON.toJson(webhook), ContentType.APPLICATION_JSON));

        //   send the POST request
        var created = httpClient.execute(request, response -> {
          checkResponse( request, response );

          //  convert the response JSON to a HashMap, for convenience
          return GSON.fromJson(EntityUtils.toString(response.getEntity()), HashMap.class);
        });

        //  the created webhook is returned in the response, including its "webhookId"
        System.out.println( "webhook succesfully created: \n" + created.toString() );
      }

    } catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

  }

  /**
   * Check for a successful HTTP response and throw an exception otherwise
   * @param request the HTTP request for the response
   * @param response the HTTP response for the request
   */
  static void checkResponse(ClassicHttpRequest request, ClassicHttpResponse response) {
    var httpstatus = response.getCode();
    var op = request.getMethod() + " " + request.getRequestUri();
    if ( httpstatus != SC_OK && httpstatus != SC_CREATED ) {
      switch (response.getCode()) {
        case SC_UNAUTHORIZED:
          throw new ApiException(op + ": invalid or missing api key");
        case SC_FORBIDDEN:
          throw new ApiException(op + ": the api key does not have permission to create a lead");
        case SC_BAD_REQUEST:
          Type targetType = new TypeToken<Map<String, Object>>() {}.getType();
          Map<String, Object> content;
          try {
            content = GSON.fromJson(EntityUtils.toString(response.getEntity()), targetType);
            throw new ApiException(op + ": there was a problem with the request: " + content.get("message"));
          } catch (Exception e) {
            throw new ApiException(e);
          }
        default:
          throw new ApiException(op + ": unexpected error; status code " + httpstatus);
      }
    }
  }

  static class ApiException extends RuntimeException {
    public ApiException(Throwable cause) {
      super(cause);
    }

    public ApiException(String message) {
      super(message);
    }
  }

}