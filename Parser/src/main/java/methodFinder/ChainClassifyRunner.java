package methodFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;

public class ChainClassifyRunner {
    public static void handleClassify(String[] args)
    {
        File root = null;
        ArrayList<String> target_repos = null;
        if(args[1].contains(".txt"))
        {
            try {
                File input = new File(args[1]);
                Scanner scanner = new Scanner(input);

                root = new File(scanner.nextLine());
                target_repos = new ArrayList<>();
                while (scanner.hasNextLine())
                    target_repos.add(scanner.nextLine());
                scanner.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
        else
            root = new File(args[1]);

        if (!root.exists()) {
            System.out.println("Couldn't find target at path: " + root);
            System.out.println("Expected usage: args contain full path to files or folders of files to parse.");
            return;
        }

        if (root.isDirectory())
            locateRepos(root, target_repos);
        else
        {
            System.out.println("Processing file " + root.getName());
            classifyChains(new File[] {root});
        }
    }

    protected static void locateRepos(final File root, final ArrayList<String> target_repos)
    {
        File[] repos;

        if(target_repos != null) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return target_repos.contains(name);
                }
            };
            repos = root.listFiles(filter);
        }
        else
            repos = root.listFiles();

        if (repos == null) {
            System.out.println(root.getName() + " has no content to parse.");
            return;
        }

        System.out.println("Processing directory " + root.getName() + " with " + repos.length + " sub-files/folders");

        classifyChains(repos);
    }

    protected  static void classifyChains(File[] repos)
    {
        final int bin_size = repos.length / Constants.THREAD_COUNT;
        Thread[] threads = new Thread[Constants.THREAD_COUNT];
        ChainClassifier[] classifiers = new ChainClassifier[Constants.THREAD_COUNT];

        for(int i = 0; i < Constants.THREAD_COUNT - 1; i++)
        {
            classifiers[i] = new ChainClassifier(repos, bin_size * i, bin_size * (i+1));
            threads[i] = new Thread(classifiers[i]);
            threads[i].start();
        }

        classifiers[Constants.THREAD_COUNT - 1] = new ChainClassifier(repos, bin_size * (Constants.THREAD_COUNT - 1), repos.length);
        threads[Constants.THREAD_COUNT - 1] = new Thread(classifiers[Constants.THREAD_COUNT - 1]);
        threads[Constants.THREAD_COUNT - 1].start();

        try {
            for(int i = 0; i < threads.length; i++)
                threads[i].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        write_size_bin_classification_CSV(classifiers);
    }

    protected  static void write_size_bin_classification_CSV(ChainClassifier[] classifiers)
    {
        File output_dir = new File("output/");
        if(!output_dir.exists())
            output_dir.mkdir();

        final StringBuilder sb = new StringBuilder();

        int[][] total_results = new int[3][4];
        for(var identifier : classifiers) {
            int[][] results = identifier.getResult();
            for (int x = 0; x < results.length; x++) {
                for (int y = 0; y < results[x].length; y++) {
                    total_results[x][y] += results[x][y];
                }
            }
        }

        try {

            // |Size|Accessor|Builder|Assertion|Others
            // |S   |
            // |L   |
            // |XL  |

            FileWriter csvWriter = new FileWriter("output/binned_size_chain_classification_counts.csv");
            csvWriter.append("Size,Accessor,Builder,Assertion,Others\n");
            final String[] sizes = new String[] {"S,","L,","XL,"};

            for (int x = 0; x < total_results.length; x++) {
                sb.append(sizes[x]);
                for (int y = 0; y < total_results[x].length; y++) {
                    sb.append(total_results[x][y]).append(",");
                }
                sb.append("\n");
                csvWriter.append(sb.toString());

                sb.setLength(0);
            }

            csvWriter.flush();
            csvWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
