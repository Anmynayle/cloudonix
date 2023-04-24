package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private List<String> words = new ArrayList<>();
  private File wordsFile = new File("words.txt");

  @Override
  public void start() throws Exception{
    loadWords();

    HttpServer server = vertx.createHttpServer();
    server.requestHandler(this::handleRequest);

    server.listen(8080, result -> {
        if (result.succeeded()){
          System.out.println("AnalyzerVerticle started Succesfully");
        } 
        else {
          System.out.println("AnalyzerVerticle failed to start");
        }
    });
    
  }
 
  private void handleRequest(HttpServerRequest request){
    if ("/main".equals(request.path()) && "POST".equals(request.method().name())){
      request.bodyHandler(buffer -> {
        JsonObject requestBody = buffer.toJsonObject();
        String text = requestBody.getString("text");

        if (words.isEmpty()){
          request.response().setStatusCode(200).putHeader("content-type","application/json").end(new JsonObject().encode());
           return;
        } 

        JsonObject response= new JsonObject();
        response.put("value", findClosestByValue(text));
        response.put("lexical", findClosestByLexical(text));

        request.response().setStatusCode(200).putHeader("content-type", "application/json").end(response.encode());
      });
      request.response().setStatusCode(404).end();
    }
}
private String findClosestByValue(String text) {
  int targetValue = calculateValue(text);
  String closest = null;
  int closestDiff = Integer.MAX_VALUE;

  for (String word : words) {
      int value = calculateValue(word);
      int diff = Math.abs(targetValue - value);
      if (diff < closestDiff) {
          closest = word;
          closestDiff = diff;
      }
  }

  return closest;
}

private int calculateValue(String text) {
  return text.toLowerCase().chars().map(ch -> ch - 'a' + 1).sum();
}

private String findClosestByLexical(String text) {
  String target = text.toLowerCase();

  return words.stream()
          .min((a, b) -> Integer.compare(levenshteinDistance(a.toLowerCase(), target), levenshteinDistance(b.toLowerCase(), target)))
          .orElse(null);
}

private int levenshteinDistance(String a, String b) {
  int[][] dp = new int[a.length() + 1][b.length() + 1];

  for (int i = 0; i <= a.length(); i++) {
      for (int j = 0; j <= b.length(); j++) {
          if (i == 0 || j == 0) {
              dp[i][j] = i + j;
          } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
              dp[i][j] = dp[i - 1][j - 1];
          } else {
              dp[i][j] = 1 + Math.min(dp[i][j - 1], Math.min(dp[i - 1][j], dp[i - 1][j - 1]));
          }
      }
  }

  return dp[a.length()][b.length()];
}

private void loadWords() {
  if (wordsFile.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(wordsFile))) {
          words = reader.lines().collect(Collectors.toList());
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
}

private void saveWords() {
  try (PrintWriter writer = new PrintWriter(new FileWriter(wordsFile))) {
      for (String word : words) {
          writer.println(word);
      }
  } catch (IOException e) {
      e.printStackTrace();
  }
}

@Override
public void stop() throws Exception {
  saveWords();
}

public static void main(String[] args) {
  Vertx vertx = Vertx.vertx();
  vertx.deployVerticle(new MainVerticle());
}
}

