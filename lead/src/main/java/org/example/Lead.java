package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import java.lang.reflect.Type;
import java.util.*;

import static java.util.Map.entry;
import static org.apache.hc.core5.http.HttpStatus.*;


/**
 * Sample application to create a "lead" -- a potential customer
 */
@SuppressWarnings("unchecked")
public class Lead {

  //  this sample uses Google's Gson to convert Java types
  //  to and from JSON, but there are alternatives, such as
  //  Jackson's ObjectMapper and others
  static final Gson GSON = new GsonBuilder()
      //  try to parse 'number' as Long first; else Double
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .create();
  static Random random = new Random();

  static String apikey = "your-api-key-here";
  static String baseURL;

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
      //
      //  POST on collection /leads to create one
      var request = new HttpPost(baseURL + "/leads");
      request.setHeader("Accept", "application/json");
      //  set the mandatory x-api-key HTTP header
      request.setHeader("x-api-key", apikey);

      try (var httpClient = HttpClients.createDefault()) {

        var officeId = getRandomOfficeId();

        //  minimum properties for creation -- normally they would be
        //  obtained through user input, but we'll pick random values
        var lead = Map.ofEntries(
            entry("firstName", "Example " + random.nextLong() ),
            entry("lastName", "Lead"),
            entry("branchId", getRandomBranchId() ),
            entry("officeId", officeId ),
            entry("primarySalesRepId", getOfficeRandomSalesRepId( officeId ) ),
            entry("statusId", "SS_NOT_HOME")
        );

        //  convert the HashMap to JSON as required
        request.setEntity(new StringEntity(GSON.toJson(lead), ContentType.APPLICATION_JSON));

        //   send the POST request
        var created = httpClient.execute(request, response -> {
          checkResponse( request, response );

          //  convert the response JSON to a HashMap, for convenience
          return GSON.fromJson(EntityUtils.toString(response.getEntity()), HashMap.class);
        });

        //  the created lead is returned in the response, including its "leadId"
        System.out.println( "lead succesfully created: \n" + created.toString() );

      }

    } catch (Exception e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

  }


  /**
   * Select a random branch from the available branches and return its id.
   * @return a branch id
   * @throws Exception on error
   */
  static String getRandomBranchId() throws Exception {

    var uri = new URIBuilder( baseURL + "/branches" )
        .addParameter( "filter", "active=true" );

    var request = new HttpGet( uri.build() );
    request.setHeader("Accept", "application/json");
    request.setHeader("x-api-key", apikey);

    try (var httpClient = HttpClients.createDefault()) {

      //   send the GET request
      var branches = httpClient.execute(request, response -> {
        checkResponse( request, response );

        //  convert the response JSON to a List<Map>; could also convert to POJO
        Type targetType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        return (List<Map<String, Object>>) GSON.fromJson(EntityUtils.toString(response.getEntity()), targetType);

      });

      if ( branches.isEmpty() ) {
        //  should not occur -- always at least one branch exists
        throw new Exception("no branches found!");
      }

      int index = random.nextInt(branches.size());
      Map<String,Object> branch = branches.get( index );
      return (String) branch.get("branchId");

    }

  }


  /**
   * Select a random sales office from the available offices and return its id.
   * @return an office id
   * @throws Exception on error
   */
  static String getRandomOfficeId() throws Exception {

    var request = new HttpGet(baseURL + "/offices");
    request.setHeader("Accept", "application/json");
    request.setHeader("x-api-key", apikey);

    try (var httpClient = HttpClients.createDefault()) {

      //   send the GET request
      var offices = httpClient.execute(request, response -> {
        checkResponse( request, response );

        //  convert the response JSON to a List<Map>; could also convert to POJO
        Type targetType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        return (List<Map<String, Object>>) GSON.fromJson(EntityUtils.toString(response.getEntity()), targetType);

      });

      if ( offices.isEmpty() ) {
        throw new Exception("no offices found!");  //  should not occur -- always at least one should exist
      }

      int index = random.nextInt(offices.size());
      Map<String,Object> office = offices.get( index );
      return (String) office.get("officeId");

    }

  }


  /**
   * Select a random salesrep from the given sales office and return their employeeId.
   * @param officeId a sales office id
   * @return an office id
   * @throws Exception on error
   */
  static String getOfficeRandomSalesRepId(String officeId) throws Exception {

    //  get active sales reps in the desired office
    var uri = new URIBuilder( baseURL + "/offices/" + officeId + "/sales-reps" )
        .addParameter( "filter", "active=true" );

    var request = new HttpGet( uri.build() );
    request.setHeader("Accept", "application/json");
    request.setHeader("x-api-key", apikey);

    try (var httpClient = HttpClients.createDefault()) {

      //   send the GET request
      var salesreps = httpClient.execute(request, response -> {
        checkResponse( request, response );

        //  convert the response JSON to a List<Map>; could also use POJO
        Type targetType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        return (List<Map<String,Object>>) GSON.fromJson(EntityUtils.toString(response.getEntity()), targetType);

      });

      if ( salesreps.isEmpty() ) {
        throw new Exception("no salesreps found in office " + officeId );
      }

      int index = random.nextInt(salesreps.size());
      Map<String,Object> salesrep = salesreps.get( index );
      return (String) salesrep.get("employeeId");

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
          Map<String, Object> content = null;
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