package methodFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;

public class LongChainSearchRunner {

    protected static int chains_per_repo = 2;

    public static void handleFindLongestN(String[] args)
    {
        File root = null;
        ArrayList<String> target_repos = null;
        if(args[1].contains(".txt"))
        {
            try {
                File input = new File(args[1]);
                Scanner scanner = new Scanner(input);

                root = new File(scanner.nextLine());

                if(scanner.hasNextInt())
                    chains_per_repo = scanner.nextInt();
                else
                    chains_per_repo = 2;
                
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
            System.out.println("Read the file: " + root.getName());
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

        findLongest_N_ChainsFromRepos(repos);
    }

    protected  static void findLongest_N_ChainsFromRepos(File[] repos)
    {
        final int bin_size = repos.length / Constants.THREAD_COUNT;
        Thread[] threads = new Thread[Constants.THREAD_COUNT];
        LongChainFinder[] finders = new LongChainFinder[Constants.THREAD_COUNT];

        for(int i = 0; i < Constants.THREAD_COUNT - 1; i++)
        {
            finders[i] = new LongChainFinder(repos, bin_size * i, bin_size * (i+1) );
            threads[i] = new Thread(finders[i]);
            threads[i].start();
        }

        finders[Constants.THREAD_COUNT - 1] = new LongChainFinder(repos, bin_size * (Constants.THREAD_COUNT - 1), repos.length);
        threads[Constants.THREAD_COUNT - 1] = new Thread(finders[Constants.THREAD_COUNT - 1]);
        threads[Constants.THREAD_COUNT - 1].start();

        try {
            for(int i = 0; i < threads.length; i++)
                threads[i].join();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        write_longest_chain_path_CSV(finders);
    }

    protected  static void write_longest_chain_path_CSV(LongChainFinder[] finders)
    {
        File output_dir = new File("output/");
        if(!output_dir.exists())
            output_dir.mkdir();

        try
        {
            FileWriter csvWriter = new FileWriter("output/paths_to_n_longest_chains.csv");
            FileWriter error_log = new FileWriter("output/error_log_paths_to_n_longest.txt");

            csvWriter.append("Chain_Length,Repo,Filename\n");

            for(var finder : finders)
            {
                for(String err : finder.getFails())
                    error_log.append(err);

                if(finder.longest_chains == null)
                    continue;

                for(String result : finder.getResult())
                    csvWriter.append(result);
            }

            csvWriter.flush();
            csvWriter.close();

            error_log.flush();
            error_log.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
