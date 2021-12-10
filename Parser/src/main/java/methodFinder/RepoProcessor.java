package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RepoProcessor implements Runnable {

    public List<List<String>> data;
    public File[] repos;
    protected final StringBuilder identifier;
    protected final int UPPER_BOUND;
    protected final int LOWER_BOUND;

    protected HashMap<String, RepoStats> repoStats;

    protected int m_longestChain;

    public RepoProcessor(File[] repos, int LOWER_BOUND, int UPPER_BOUND)
    {
        this.repos = repos;
        this.identifier = new StringBuilder();
        this.UPPER_BOUND = UPPER_BOUND;
        this.LOWER_BOUND = LOWER_BOUND;
        data = new ArrayList<>();
        repoStats = new HashMap<>();
    }

    @Override
    public void run() {
        for(int i = LOWER_BOUND; i < UPPER_BOUND; i++)
        {
            if(repos[i].isFile())
            {
                try {
                    processFile(new FileInputStream(repos[i].getAbsolutePath()), repos[i].getName(), repos[i].getName());
                }
                catch (Exception e) { e.printStackTrace(); }
            }
            else {
                walkPath(repos[i], repos[i].getName());
            }
        }
    }

    protected void walkPath(File directory, String repo_name)
    {
        for(File f : Objects.requireNonNull(directory.listFiles()))
        {
            if(f.isDirectory())
            {
                if(f.getName().equals("test"))
                {
                    for(File file : Objects.requireNonNull(f.listFiles()))
                    {
                        if(file.isDirectory())
                        {
                            walkPath(file, repo_name);
                            continue;
                        }

                        try {
                            processTestFile(new FileInputStream(file.getAbsolutePath()), repo_name, file.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else
                {
                    for(File file : Objects.requireNonNull(f.listFiles()))
                    {
                        if(file.isDirectory())
                        {
                            walkPath(file, repo_name);
                            continue;
                        }

                        try {
                            processFile(new FileInputStream(file.getAbsolutePath()), repo_name, file.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            else
            {
                for(File file : Objects.requireNonNull(f.listFiles()))
                {
                    if(file.isDirectory())
                    {
                        walkPath(file, repo_name);
                        continue;
                    }

                    try {
                        processFile(new FileInputStream(file.getAbsolutePath()), repo_name, file.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void processFile(FileInputStream target, String repoName, String fileName)
    {
        try
        {
            if(!repoStats.containsKey(repoName))
                repoStats.put(repoName, new RepoStats(repoName));

            CompilationUnit cu = StaticJavaParser.parse(target);

            cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .forEach(x -> repoStats.get(repoName).addObservation(MethodFinder.getScopeSize(x)));
        }
        catch(Exception e) { System.out.println("Failed to parse " + repoName + "::" + fileName); } //e.printStackTrace(); }
    }

    protected void processTestFile(FileInputStream target, String repoName, String fileName)
    {
        try
        {
            if(!repoStats.containsKey(repoName))
                repoStats.put(repoName, new RepoStats(repoName));

            CompilationUnit cu = StaticJavaParser.parse(target);

            cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .forEach(x -> repoStats.get(repoName).addTestObservation(MethodFinder.getScopeSize(x)));
        }
        catch(Exception e) { System.out.println("Failed to parse " + repoName + "::" + fileName); } // e.printStackTrace(); }
    }

    public List<String> getRepoStats()
    {
        List<String> result = new ArrayList<>();
        for(RepoStats stats : repoStats.values()) {
            result.add(stats.getObservations());

            if(stats.longestChain() > m_longestChain)
                m_longestChain = stats.longestChain();
        }

        return result;
    }

    public int longestChain()
    {
        return m_longestChain;
    }
}
