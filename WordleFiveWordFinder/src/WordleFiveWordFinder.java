/**
 * See https://github.com/neilcoffey/FunStuff
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WordleFiveWordFinder {
    private static final boolean PARALLEL = true;
    private static final boolean ORDER_BY_LETTER_FREQUENCY = true;

    private static final String NEWLINE = String.format("%n");
    private static final Set<String> BANNED_WORDS = Stream.of(
        "FLDXT", "HDQRS", "ZHMUD", "SEQWL", "CHIVW", "GCONV", "FCONV", "EXPWY", "PBXES"
    ).collect(Collectors.toSet());

    private static final int WORD_LENGTH = 5;
    private static final int MAX_CANDIDATES_LEVEL_THREE = 8192;
    private final List<String> words;
    private final List<int[]> solutions = new ArrayList<>(1024);

    public static void main(String[] args) {
        try {
            String wordFileName = "words_alpha.txt";
            if (args.length > 0) {
                wordFileName = args[0];
            }

            Path p = Paths.get(wordFileName);
            WordleFiveWordFinder test = new WordleFiveWordFinder(p);
            System.out.println("Words: " + test.words.size());

            long t0 = System.currentTimeMillis();
            test.solve();
            long t1 = System.currentTimeMillis();

            test.dumpSolutions();

            double secs = (double) (t1 - t0) / 1000d;
            System.out.printf("Found solutions (%d) in %.1f secs%n", test.solutions.size(), secs);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * For the given (upper case) string, returns a bitmap representing the
     * unique letters present in the word:
     *   bit 0 set = 'A' present
     *   bit 1 set = 'B' present
     *   ...
     * These bitmaps are key for testing potential solutions rapidly. If two
     * strings contain mutually exclusive characters, then ANDing their
     * respective bitmaps will yield a value of zero.
     */
    private static int letterBits(String str) {
        int n = 0;
        for (int i = str.length()-1; i >= 0; i--) {
            n |= 1 << (str.charAt(i) - 'A');
        }
        return n;
    }

    private void solve() {
        if (ORDER_BY_LETTER_FREQUENCY) {
            sortWordsByLetterEntropy();
        }

        // Pre-compute the bitmaps of all candidate words
        int[] bitmaps = new int[words.size()];
        for (int i = 0; i < words.size(); i++) {
            bitmaps[i] = letterBits(words.get(i));
        }

        // Create a re-usable array for storing subsets of candidate words for filling positions 3-5.
        ThreadLocal<int[]> thirdWordsBuffer = ThreadLocal.withInitial(() -> new int[MAX_CANDIDATES_LEVEL_THREE]);

        int noWords = words.size();
        IntStream firstIndexStream = IntStream.range(0, noWords).map(i -> noWords-1-i);
        if (PARALLEL) {
            firstIndexStream = firstIndexStream.parallel();
        }

        firstIndexStream.forEach(i -> {
            solveWithFirstWord(i, bitmaps, thirdWordsBuffer.get());
        });
    }

    private void solveWithFirstWord(int i, int[] bitmaps, int[] thirdWordsBuffer) {
        int bitmap1 = bitmaps[i];

        // For each candidate second word...
        for (int j = i-1; j >= 0; j--) {
            int bitmap2 = bitmaps[j];
            if ((bitmap1 & bitmap2) == 0) {
                int noThirdWords = 0;

                // Populate a list of all candidate words to fill the remaining three slots,
                // given first and second candidates 'i' and 'j'
                for (int k = j-1; k >= 0; k--) {
                    int bitmap3 = bitmaps[k];
                    if (((bitmap1 | bitmap2) & bitmap3) == 0) {
                        thirdWordsBuffer[noThirdWords * 2] = k;
                        thirdWordsBuffer[noThirdWords * 2 + 1] = bitmap3;
                        noThirdWords++;
                    }
                }

                // Consider combinations from the smaller list for words 3, 4 and 5
                if (noThirdWords > 0) {
                    findSolution(bitmap1 | bitmap2, thirdWordsBuffer, noThirdWords, i, j);
                }
            }
        }
    }

    private void dumpSolutions() {
        synchronized (solutions) {
            for (int i = 0; i < solutions.size(); i++) {
                System.out.println("-------- Solution " + (i + 1) + " -------");
                printSolution(solutions.get(i));
            }
        }
    }

    /**
     * Given a partial solution of 'wordNo1' and 'wordNo2' and a list candidates for the remaining three
     * slots, find all unique combinations that mean the 5x5 letters from the give words are unique.
     *
     * @param firstTwoWordsBitmap  A bitmap of letters already used (bit 0 = 'A', bit 1 = 'B' etc)
     * @param availableWords  An array containing information about the remaining candidate words.
     *                        Positions n and n+1 in the array indicate the index (into 'words') of the
     *                        nth word followed by its letter bitmap.
     * @param noAvailableWords The number of words in 'availableWords', allowing the array to be re-used
     *                         on successive calls to this method.
     * @param wordNo1  The word number (i.e. index into 'words') of the first word in the partial solution so far.
     * @param wordNo2  The word number (i.e. index into 'words') of the second word in the partial solution so far.
     */
    private void findSolution(int firstTwoWordsBitmap, int[] availableWords, int noAvailableWords, int wordNo1, int wordNo2) {
        for (int k = noAvailableWords-1; k >= 0; k--) {
            int bitmap3 = availableWords[k * 2 + 1];
            for (int l = k-1; l >= 0; l--) {
                int bitmap4 = availableWords[l * 2 + 1];
                if (((firstTwoWordsBitmap | bitmap3) & bitmap4) == 0) {
                    for (int m = l-1; m >= 0; m--) {
                        int bitmap5 = availableWords[m * 2 + 1];
                        if (((firstTwoWordsBitmap | bitmap3 | bitmap4) & bitmap5) == 0) {
                            int[] solution = new int[5];
                            solution[0] = wordNo1;
                            solution[1] = wordNo2;
                            solution[2] = availableWords[k * 2];
                            solution[3] = availableWords[l * 2];
                            solution[4] = availableWords[m * 2];
                            synchronized (solutions) {
                                solutions.add(solution);
                            }
                        }
                    }
                }
            }
        }
    }

    private void printSolution(int[] wordNos) {
        StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < wordNos.length; i++) {
            if (i > 0) {
                sb.append(NEWLINE);
            }
            sb.append(words.get(wordNos[i]));
        }
        System.out.println(sb);
    }

    private void sortWordsByLetterEntropy() {
        int[] letterCounts = new int[26];
        for (String w : words) {
            for (int n = w.length()-1; n >= 0; n--) {
                int letterNo = w.charAt(n) - 'A';
                if (letterNo < 0 || letterNo >= 26)
                    throw new RuntimeException("Unexpected letter " + letterNo + " in '" + w + "'");
                letterCounts[letterNo]++;
            }
        }

        Map<String, Long> totalFreqs = new HashMap<>(words.size());
        for (String w : words) {
            totalFreqs.put(w, totalFreq(w, letterCounts));
        }

        words.sort(Comparator.comparingLong(totalFreqs::get));
    }

    private WordleFiveWordFinder(Path wordFile) throws IOException {
        this.words = stripAnagrams(extractCandidateWords(wordFile));
    }

    private static long totalFreq(String w, int[] freqs) {
        long t = 0;
        for (int n = w.length()-1; n >= 0; n--) {
            int letterNo = w.charAt(n) - 'A';
            t += freqs[letterNo];
        }
        return t;
    }

    /**
     * From the given file, assumed to contain one word per line, returns a string
     * of all words consisting of five characters not on the banner word list. Words
     * are converted to upper case where necessary.
     *
     * @param f  The input file.
     * @return  A list of candidate words from the input file.
     * @throws IOException  If an I/O error occurs reading the contents of the file.
     */
    private static List<String> extractCandidateWords(Path f) throws IOException {
        return Files.lines(f)
                .filter(w -> w.length() == WORD_LENGTH)
                .map(String::toUpperCase)
                .filter(w -> containsUniqueLetters(w))
                .filter(w -> !BANNED_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    /**
     * Returns a subset of 'words' such that for each unique combination of letters
     * present, only one anagram is retained. Which anagram is chosen is arbitrary!
     *
     * @param words  The original list of words.
     * @return  A new list which contains the contents of 'words', stripped of anagrams.
     */
    private static List<String> stripAnagrams(List<String> words) {
        Map<Integer, List<String>> m = new LinkedHashMap<>(words.size());
        for (String w : words) {
            int bm = letterBits(w);
            m.computeIfAbsent(bm, x -> new ArrayList<>(5)).add(w);
        }
        return m.values().stream()
                .flatMap(l -> Stream.of(l.get(0)))
                .collect(Collectors.toList());
    }

    private static boolean containsUniqueLetters(String str) {
        return Integer.bitCount(letterBits(str)) == str.length();
    }

}
