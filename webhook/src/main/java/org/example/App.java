package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.HashMap;
import java.util.List;

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
public class App {

  //  for demonstration purposes, use the freemium "webhook.site" app as our webhook target
  static final String WEBHOOKURL = "https://webhook.site/#!/view/91ed1091-524e-4b48-a9af-0ed281009cd7";

  //  this sample uses Google's Gson to convert Java types
  //  to and from JSON, but there are alternatives, such as
  //  Jackson's ObjectMapper and others
  static final Gson GSON = new GsonBuilder()
      //  try to parse 'number' as Long first; else Double
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .create();

  /**
   * @param args optional; base URL and apikey
   */
  @SuppressWarnings("java:S106")  // use of System.println()
  public static void main(String[] args) {

    //  to create a webhook, your api-key must be associated
    // with a user assigned to the Administrators security group
    String apikey = "your-api-key-here";

    String instance = "sandbox";  // subscriber instance name
    String baseURL = "https://" + instance + ".briostack.io/rest/v1";

    if ( args.length == 2 ) {
      //  take the URL and apikey from the command line
      baseURL = args[0];
      apikey = args[1];
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
        httpClient.execute(request, response -> {

          //  convert the response JSON to a HashMap, for convenience
          var created = GSON.fromJson(EntityUtils.toString(response.getEntity()), HashMap.class);

          switch ( response.getCode() ) {
            case SC_OK, SC_CREATED:
              System.out.println("successfully created webhook id = " + created.get("webhookId"));
              break;
            case SC_UNAUTHORIZED:
              System.out.println("invalid or missing api key");
              break;
            case SC_FORBIDDEN:
              System.out.println("the api key does not have permission to create a webhoook");
              break;
            case SC_BAD_REQUEST:
              System.out.println("there was a problem with the request: " + created.get("message"));
              break;
            default:
              System.out.println("unexpected error; status code " + response.getCode() );

          }
          return response;
        });

      }

    } catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

  }

}