package Lemmatizer;


import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Lem  {

    static final List<String> signs = Arrays.asList("ПРЕДЛ", "МЕЖД", "СОЮЗ", "ЧАСТ");
    static LuceneMorphology luceneMorphology = null;


    public static void createMorph() throws IOException {
        luceneMorphology = new RussianLuceneMorphology();
    }


    public static ConcurrentHashMap<String,Integer> searchForLem(String text) throws IOException {
        ConcurrentHashMap<String,Integer> lems = new ConcurrentHashMap();
        String[] words = text.replaceAll("\\p{Punct}", " ")
                .replaceAll("[^а-яА-Я\\s]+"," ")
                .toLowerCase().split("\\s");

        for (String word : words) {
            if (word.equals("")) {
                continue;
            }
            int count = 1;
            if (signs.stream().anyMatch(luceneMorphology.getMorphInfo(word).get(0)::contains)) {
                continue;
            }
            String lem = luceneMorphology.getNormalForms(word).get(0);
            if (lems.containsKey(lem)) {
                lems.put(lem, lems.get(lem) + 1);
            } else
                lems.put(lem, count);
        }

        return lems;
    }

    public static HashMap<String, String> replaceForLemms(String text) throws IOException {
        createMorph();
        HashMap<String, String> results = new HashMap<>();
        String[] words = text.replaceAll("\\p{Punct}", "")
                .replaceAll("[^а-яА-Я\\s]+", "")
                .toLowerCase().split("\\s");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("") || signs.stream().anyMatch(luceneMorphology.getMorphInfo(words[i]).get(0)::contains)) {
                continue;
            }

                  results.put(luceneMorphology.getNormalForms(words[i]).get(0), words[i]);



        }
        return results;

    }

}
