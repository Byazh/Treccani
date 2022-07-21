import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    public static void main(String... args) throws IOException, URISyntaxException {
        // Get the list of saved words
        var inputStream = Main.class.getResourceAsStream("parole.txt");
        var words = new BufferedReader(new InputStreamReader(inputStream)).lines().toList().stream().map(String::trim).collect(Collectors.toSet());
        var preexistingJson = Files.readAllLines(Paths.get(Main.class.getClassLoader().getResource("parole.json").toURI())).stream().collect(Collectors.joining(""));
        var array = new JSONObject(preexistingJson);
        for (String word : words) {
            var object = word(word);
            if (!array.has(word)) {
                array.put(word, object);
                System.out.println(word + "  " + object.get("d") + "\n");
            }
            System.out.println(word);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try (var writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("C:\\Users\\famiglia sottero\\Downloads\\parole.json"), StandardCharsets.UTF_8))) {
            writer.write(array.toString());
        }
    }

    public static JSONObject word(String word) {
        try {
            var link = "https://www.treccani.it/vocabolario/" + word;
            // Definition and details
            var scraper1 = Jsoup.connect(link).get();
            var _clr1 = scraper1.getElementsByClass("text spiega").first();
            // Words who have more than one meaning have different web pages, each identified with a number.
            // If the default one doesn't exist, we'll get the one with n = 1
            if (_clr1 == null) {
                link += "1";
                _clr1 = Jsoup.connect(link).get().getElementsByClass("text spiega").first();
            }
            var clr1 = _clr1.select("p").get(2);
            // Definition of the word
            var definition = clr1.text();

            // Thesaurus
            var scraper2 = Jsoup.connect("https://www.treccani.it/vocabolario/" + word +  "_%28Sinonimi-e-Contrari%29/").get();
            var _clr2 = scraper2.getElementsByClass("text spiega").first();
            var synAndCont = "";
            if (_clr2 == null) {
                synAndCont = "-1";
            } else {
                synAndCont = _clr2.select("p").get(1).text();
            }

            // Grammar
            var temp1 = definition.split(" ");
            var temp2 = new ArrayList<>(Arrays.asList(temp1));
            temp2.remove(0);
            int temp3 = 0;
            for (int i = 0; i < temp2.size(); i++) {
                if (temp2.get(i).startsWith("[") || temp2.get(i).startsWith("(")) {
                    temp3 = ++i;
                    break;
                }
            }
            var grammar = String.join(" ", Arrays.copyOfRange(temp1, 1, temp3));

            // Pronunciation
            var pronunciation = "";
            if (synAndCont.equals("-1")) {
                pronunciation = pronunciation = "/" + word + "/";
            } else {
                var temp4 = new ArrayList<>(Arrays.asList(synAndCont.split(" ")));
                pronunciation = temp4.get(1);
                if (!pronunciation.contains("/")) pronunciation = "/" + word + "/";
            }

            // Definitions pt. 1
            var temp5 = (new ArrayList<>(Arrays.asList(clr1.toString().split("[\\)\\]].(.+?)–")))).get(1);
            var temp6 = (new ArrayList<>(Arrays.asList(temp5.split("◆")))).get(0);
            var pattern = Pattern.compile("<i>(.+?)</i>", Pattern.DOTALL);
            var matcher = pattern.matcher(temp6);

            /// Examples
            StringBuilder temp7 = new StringBuilder();
            while(matcher.find()) {
                temp7.append(matcher.group()).append(", ");
            }
            String examples = temp7.length() <= 2 ? "" : temp7.substring(0, temp7.length() - 2).replaceAll("<i>", "").replaceAll("</i>", "");

            // Definitions pt. 2
            var simpleDefinition = matcher.replaceAll("");
            var pattern2 = Pattern.compile("\\((.+?)\\)", Pattern.DOTALL);
            var matcher2 = pattern2.matcher(simpleDefinition);
            simpleDefinition = matcher2.replaceAll("");
            simpleDefinition = simpleDefinition.replaceAll("<(.+?)>", "");
            simpleDefinition = simpleDefinition.replaceAll("[:;,.]\\s+[:;,.]", "");
            simpleDefinition = simpleDefinition.replaceAll("[;,.]\\s*[:;,.]", "");
            simpleDefinition = simpleDefinition.replaceAll("\\s{2,}", " ").trim();
            simpleDefinition = simpleDefinition.replaceAll("\s\\.", ".").trim();
            simpleDefinition = simpleDefinition.replaceAll("\s\\,", ",").trim();
            simpleDefinition = simpleDefinition.replaceAll("\s\\;", ";").trim();
            simpleDefinition = simpleDefinition.replaceAll("\s\\:", ":").trim();

            // Synonyms
            var synonyms = "";
            if (!synAndCont.equals("-1")) {
                var _temp9 = (new ArrayList<>(Arrays.asList(synAndCont.split("[)\\]][.,](.+?)-"))));
                var temp9 = _temp9.get(1);
                var pattern3 = Pattern.compile("\\[(.+?)\\]|\\((.+?)\\)", Pattern.DOTALL);
                var matcher3 = pattern3.matcher(temp9);
                temp9 = matcher3.replaceAll("");
                var pattern4 = Pattern.compile("Espressioni(.+?)\\. [abc|1-5]\\.", Pattern.DOTALL);
                var matcher4 = pattern4.matcher(temp9);
                while (matcher4.find()) {
                    var match = matcher4.group();
                    var suffix = match.substring(match.length() - 2);
                    System.out.println(suffix);
                    temp9 = temp9.replaceAll(!suffix.matches("[a-f|1-5]\\.") ? matcher4.group() : match.substring(0, match.length() - 3), "");
                }
                synonyms = temp9.replaceAll("\\s{2,}", " ").trim();
            }
            return new JSONObject()
                    .put("w", word)
                    .put("p", pronunciation)
                    .put("g", grammar)
                    .put("d", simpleDefinition)
                    .put("e", examples)
                    .put("s", synonyms)
                    .put("l", link);
        } catch (NullPointerException | IOException e) {
            System.out.println(e);
        }
        return null;
    }
}
