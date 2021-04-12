package org.simonscode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

public class NotenAPI {

  private Document doc;
  private Response resp;
  private final Map<String, String> cookies = new HashMap<>();

  static String getUsernameFilename(String id) {
    return id + ".username";
  }

  static String getGradesFilename(String username) {
    return username + ".grades";
  }

  public boolean init(String username, String password) {
    try {
      HashMap<String, Double> extracted = extract(username, password);
      write(extracted, password, new File(username + ".grades"));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public HashMap<String, Double> get_new_entries(String username)
      throws IOException {

    File file = new File(username + ".grades");
    HashMap<String, Double> readExtract = new HashMap<>();
    String password = read(readExtract, file);
    HashMap<String, Double> extracted = extract(username, password);
    write(extracted, password, file);
    for (String key : readExtract.keySet()) {
      extracted.remove(key);
    }

    return extracted;
  }

  private void write(HashMap<String, Double> extracted, String password, File file) throws FileNotFoundException {
    PrintWriter pw = new PrintWriter(file);
    pw.println(password);
    extracted.keySet().stream().sorted().forEach(s -> pw.printf("%s:%f\n", s, extracted.get(s)));
    pw.close();
  }

  private String read(HashMap<String, Double> extracted, File file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(file));
    String password = null;
    while (br.ready()) {
      String line = br.readLine();
      if (!line.isBlank()) {
        if (password == null) {
          password = line;
        }
        String[] parts = line.split(":");
        if (parts.length == 2) {
          extracted.put(parts[0], Double.parseDouble(parts[1]));
        }
      }
    }
    br.close();
    return password;
  }

  private HashMap<String, Double> extract(String username, String password) throws IOException {
    System.setProperty("jsse.enableSNIExtension", "false");

    login(username, password);

    clickOnLink(".auflistung");

    clickOnLink(".liste > li:nth-child(4) > a:nth-child(1)");

    clickOnLink("a.regular:nth-child(2)");

    clickOnLink("ul.treelist:nth-child(4) > li:nth-child(1) > a:nth-child(3)");

    Element table = doc.getElementsByTag("table").last();

    Elements rows = table.getElementsByTag("tbody").first().children();

    HashMap<String, Double> result = new HashMap<>();
    for (Element row : rows) {
      if (row.child(3).text().isBlank()) {
        continue;
      }
      try {
        result.put(row.child(1).text(), Double.parseDouble(row.child(3).text().replace(',', '.')));
      } catch (NumberFormatException e) {
        // do nothing
      }
    }
    return result;
  }

  private void login(String username, String password) throws IOException {
    resp = Jsoup.connect(
        "https://sta-hisweb.hs-emden-leer.de/qisserverel/rds?state=user&type=0&topitem=")
        .followRedirects(true)
        .execute();

    cookies.putAll(resp.cookies());

    doc = resp.parse();
    FormElement form = doc.getElementsByTag("form").forms().get(0);

    form.elements().get(0).attr("value", username);
    form.elements().get(1).attr("value", password);

    resp = form.submit()
        .cookies(cookies)
        .referrer(resp.url().toString())
        .followRedirects(true)
        .method(Method.POST)
        .execute();

    cookies.putAll(resp.cookies());

    doc = resp.parse();
  }

  private void clickOnLink(String cssQuery) throws IOException {
    String href = doc.select(cssQuery).first().attr("href");

    resp = Jsoup.connect(
        href)
        .followRedirects(true)
        .cookies(cookies)
        .referrer(resp.url().toString())
        .method(Method.GET)
        .execute();

    cookies.putAll(resp.cookies());

    doc = resp.parse();
  }

}
