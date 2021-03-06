package io.anserini.embeddings.search;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

public class SearchW2V {
  public static class SearchW2V_Args {
    @Option(name = "-model", metaVar = "[file]", required = true, usage = "path to model file")
    public String model;

    //optional arguments
    @Option(name = "-nearest", metaVar = "[Number]", usage = "number of nearest words")
    public int nearest = 10;

    @Option(name = "-term", metaVar = "[String]", usage = "input word")
    public String term = "";

    @Option(name = "-input", metaVar = "[file]", usage = "input file with one word per line")
    public String input_file = "";

    @Option(name = "-gmodel", usage = "specify if the model was trained in Google format")
    public Boolean gModel = false;

    @Option(name = "-output", metaVar = "[file]", usage = "path of the output file")
    public String output_file = "";
  }

  public static void main(String[] args) {
    final SearchW2V_Args searchW2VArgs = new SearchW2V_Args();
    CmdLineParser parser = new CmdLineParser(searchW2VArgs, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: " + SearchW2V.class.getSimpleName() + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    File model = new File(searchW2VArgs.model);
    WordVectors vec = null;

    try {
      if (searchW2VArgs.gModel) {
        vec = WordVectorSerializer.loadGoogleModel(model, true, true);
      } else {
        vec = WordVectorSerializer.readWord2Vec(model);
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    if (searchW2VArgs.term.isEmpty() && searchW2VArgs.input_file.isEmpty()) {
      System.err.println("You should enter either a word or a file to be searched");
    } else if(!searchW2VArgs.term.isEmpty() && !searchW2VArgs.input_file.isEmpty()){
      System.err.println("You should search for either a term or a file, not both");
    } else {

      if(!searchW2VArgs.term.isEmpty()){
        Collection<String> near_words = vec.wordsNearest(searchW2VArgs.term, searchW2VArgs.nearest);
        for (String word : near_words){
          System.out.println(word + " " + vec.similarity(word, searchW2VArgs.term));
        }
      } else {
        try (BufferedReader br = new BufferedReader(new FileReader(searchW2VArgs.input_file))) {
          String line;
          PrintWriter bf = new PrintWriter(new FileWriter(searchW2VArgs.output_file));

          while ((line = br.readLine()) != null) {
            String term = line.trim();
            Collection<String> near_words = vec.wordsNearest(term, searchW2VArgs.nearest);
            for(String word: near_words) {
              bf.println(term  + "\t" + word + "\t" + vec.similarity(word, term) + "\t" + Arrays.toString(vec.getWordVector(word)));
            }
          }
        } catch (IOException e){
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
