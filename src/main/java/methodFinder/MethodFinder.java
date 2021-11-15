package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class MethodFinder {

    private static final int THREAD_COUNT = 4;

    public static void main(String[] args) throws FileNotFoundException {

        if(args.length == 0)
        {
            CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/testRepo/ReversePolishNotation.java"));

            cu.getTypes().stream()
                .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                .forEach(x -> System.out.println(x.toString() + "\nMethod Chains = " + getScopeSize(x)));
        }
        else
        {
            //String pwd = System.getProperty("user.dir");
            //System.out.println("Working Directory = " + pwd);

            List<List<String>> data = new ArrayList<>();

            for(int i = 0; i < args.length; i++)
            {
                File target = new File(args[i]);

                if(target.exists() == false)
                {
                    System.out.println("Couldn't find target at path: " + args[i]);
                    System.out.println("Expected usage: args contain full path to files or folders of files to parse.");
                    continue;
                }

                if(target.isFile())
                    data.add(processFile(new FileInputStream(target.getAbsolutePath()), "Undefined," + target.getName() + ","));
                else if(target.isDirectory())
                {
                    File[] repos = target.listFiles();
                    if(repos == null)
                    {
                        System.out.println(args[0] + " has no content to parse.");
                        return;
                    }

                    Thread[] threads = new Thread[4];

                    for(int n = 0; n < THREAD_COUNT; i++)
                    {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                int bin_size = repos.length / THREAD_COUNT;
                                for(int k = bin_size * n; k < bin_size * (n+1); k++)
                                {
                                    for(File f : repos[k].listFiles())
                                    {
                                        try {
                                            String identifier = repos[k].getName() + "," + f.getName() + ",";
                                            data.add(processFile(new FileInputStream(f.getAbsolutePath()), identifier));
                                        }
                                        catch(Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        };

                        threads[n] = new Thread(r);
                        threads[n].run();
                    }

                    try {
                        for(int n = 0; n < threads.length; n++)
                            threads[n].join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            writeCSV(data);
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
        catch(Exception e)
        {
            e.printStackTrace();
        }

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
        catch(Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    protected static void writeCSV(List<List<String>> data)
    {
        try
        {
            FileWriter csvWriter = new FileWriter("method_chaining_results.csv");

            csvWriter.append("Repo,File,ChainLength\n");

            for (List<String> repos : data) {
                for(String observation : repos )
                    csvWriter.append(observation);
            }

            csvWriter.flush();
            csvWriter.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

}
