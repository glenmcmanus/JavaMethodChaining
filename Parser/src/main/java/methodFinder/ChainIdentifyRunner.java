package methodFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChainIdentifyRunner {

    public static void handleIdentify(String[] args)
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
            identifyRepos(root, target_repos);
        else
        {
            System.out.println("Processing file " + root.getName());
            getUniqueMethodNames(new File[] {root});
        }
    }

    protected static void identifyRepos(final File root, final ArrayList<String> target_repos)
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

        getUniqueMethodNames(repos);
    }

    protected  static void getUniqueMethodNames(File[] repos)
    {
        final int bin_size = repos.length / Constants.THREAD_COUNT;
        Thread[] threads = new Thread[Constants.THREAD_COUNT];
        ChainIdentifier[] identifiers = new ChainIdentifier[Constants.THREAD_COUNT];

        for(int i = 0; i < Constants.THREAD_COUNT - 1; i++)
        {
            identifiers[i] = new ChainIdentifier(repos, bin_size * i, bin_size * (i+1) );
            threads[i] = new Thread(identifiers[i]);
            threads[i].start();
        }

        identifiers[Constants.THREAD_COUNT - 1] = new ChainIdentifier(repos, bin_size * (Constants.THREAD_COUNT - 1), repos.length);
        threads[Constants.THREAD_COUNT - 1] = new Thread(identifiers[Constants.THREAD_COUNT - 1]);
        threads[Constants.THREAD_COUNT - 1].start();

        try {
            for(int i = 0; i < threads.length; i++)
                threads[i].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        write_unique_method_name_CSV(identifiers);
    }

    protected  static void write_unique_method_name_CSV(ChainIdentifier[] identifiers)
    {
        File output_dir = new File("output/");
        if(!output_dir.exists())
            output_dir.mkdir();

        System.out.println("Write unique method names");
        final StringBuilder sb = new StringBuilder("output/");
        for(var identifier : identifiers)
        {
            for(String repo : identifier.getRepoNames())
            {
                try
                {
                    sb.append(repo).append("_identifiers.csv");
                    FileWriter csvWriter = new FileWriter(sb.toString());

                    csvWriter.append("Method_Name,Method_Type\n");

                    for(String row : identifier.getRepoMethodNames(repo))
                    {
                        csvWriter.append(row);
                    }

                    csvWriter.flush();
                    csvWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                sb.setLength("/output/".length());
            }
        }
    }
}
