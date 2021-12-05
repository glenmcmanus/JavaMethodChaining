package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MethodFinder {

    private static int max_chain_length = 0;
    private static final String ARG_PATH_MISSING_MSG = "A full path to a target root folder of repos, " +
                                                        "or an input file with the first line targeting the root of a repo collection " +
                                                        "and the names of repos to parse must be provided";

    public static void main(String[] args) throws FileNotFoundException {

        for(var s : args)
            System.out.println("'"+s+"'");

        if(args.length == 0)
        {
            CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/testRepo/ReversePolishNotation.java"));

            cu.getTypes().stream()
                .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                .forEach(x -> System.out.println(x.toString() + "\nMethod Chains = " + getScopeSize(x)));
        }
        else if(args[0].strip().equals("-identify"))
        {
            if(args.length == 1)
            {
                System.out.println(ARG_PATH_MISSING_MSG);
                return;
            }

            ChainIdentifyRunner.handleIdentify(args);
        }
        else if(args[0].strip().equals("-classify"))
        {
            if(args.length == 1)
            {
                System.out.println(ARG_PATH_MISSING_MSG);
                return;
            }

            ChainClassifyRunner.handleClassify(args);
        }
        else if(args[0].strip().equals("-longest"))
        {
            if(args.length == 1)
            {
                System.out.println(ARG_PATH_MISSING_MSG);
                return;
            }

            LongChainSearchRunner.handleFindLongestN(args);
        }
        else
        {
            List<List<String>> data = new ArrayList<>();

            for (String arg : args) {
                File target = new File(arg);

                if (!target.exists()) {
                    System.out.println("Couldn't find target at path: " + arg);
                    System.out.println("Expected usage: args contain full path to files or folders of files to parse.");
                    continue;
                }

                if (target.isFile()) {
                    System.out.println("Processing file " + target.getName());

                    data.add(processFile(new FileInputStream(target.getAbsolutePath()), "Undefined," + target.getName() + ","));
                } else if (target.isDirectory()) {
                    File[] repos = target.listFiles();
                    if (repos == null) {
                        System.out.println(args[0] + " has no content to parse.");
                        return;
                    }

                    System.out.println("Processing directory " + target.getName() + " with " + repos.length + " sub-files/folders");

                    processRepos(repos, data);
                }
            }

            write_MC_Count_CSV(data);
        }
    }

    protected static void processRepos(File[] repos, List<List<String>> data)
    {
        final int bin_size = repos.length / Constants.THREAD_COUNT;
        Thread[] threads = new Thread[Constants.THREAD_COUNT];
        RepoProcessor[] processors = new RepoProcessor[Constants.THREAD_COUNT];

        for(int i = 0; i < Constants.THREAD_COUNT - 1; i++)
        {
            processors[i] = new RepoProcessor(repos, bin_size * i, bin_size * (i+1) );
            threads[i] = new Thread(processors[i]);
            threads[i].start();
        }

        processors[Constants.THREAD_COUNT - 1] = new RepoProcessor(repos, bin_size * (Constants.THREAD_COUNT - 1), repos.length);
        threads[Constants.THREAD_COUNT - 1] = new Thread(processors[Constants.THREAD_COUNT - 1]);
        threads[Constants.THREAD_COUNT - 1].start();

        try {
            for(int i = 0; i < threads.length; i++)
            {
                threads[i].join();
                data.add(processors[i].getRepoStats());

                if(max_chain_length < processors[i].longestChain())
                    max_chain_length = processors[i].longestChain();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected static List<String> processFile(FileInputStream target, String identifier)
    {
        try
        {
            List<String> result = new ArrayList<>();

            CompilationUnit cu = StaticJavaParser.parse(target);

            cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .forEach(x -> result.add(identifier + getScopeSize(x) + "\n"));

            return result;
        }
        catch(Exception e) { e.printStackTrace(); }

        return null;
    }

    protected static int getScopeSize(MethodCallExpr expr) {
        try {
            return expr.getScope()
                    .filter(Expression::isMethodCallExpr)
                    .map(Expression::asMethodCallExpr)
                    .map(
                            val -> getScopeSize(val) + 1
                    )
                    .orElse(1);
        }
        catch(Exception e) { e.printStackTrace(); return 0; }
    }

    protected static void write_MC_Count_CSV(List<List<String>> data)
    {
        File output_dir = new File("output/");
        if(!output_dir.exists())
            output_dir.mkdir();

        try
        {
            FileWriter csvWriter = new FileWriter("output/method_chaining_results.csv");

            csvWriter.append("Repo,LongestChain");

            for(int i = 0; i <= max_chain_length; i++)
                csvWriter.append(",").append("Length ").append(String.valueOf(i));

            csvWriter.append("\n");

            for (List<String> repos : data) {
                for(String observation : repos )
                    csvWriter.append(padOutputWithZeros(observation));
            }

            csvWriter.flush();
            csvWriter.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    protected static String padOutputWithZeros(String observation)
    {
        observation = observation.substring(0, observation.length() - 1);

        int count = 0;
        for(int i = 0; i < observation.length(); i++)
        {
            if(observation.charAt(i) == ',')
                count++;
        }

        count = max_chain_length + 2 - count;

        StringBuilder sb = new StringBuilder(observation);
        while(count > 0)
        {
            sb.append(",0");
            count--;
        }

        sb.append("\n");

        return sb.toString();
    }

}
