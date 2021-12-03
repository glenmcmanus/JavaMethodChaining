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
                    processFile(new FileInputStream(repos[i].getAbsolutePath()), repos[i].getName());
                }
                catch (Exception e) { e.printStackTrace(); }
            }
            else {
                for (File f : Objects.requireNonNull(repos[i].listFiles())) {
                    try {
                        processFile(new FileInputStream(f.getAbsolutePath()), repos[i].getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void processFile(FileInputStream target, String repoName)
    {
        try
        {
            if(!repoStats.containsKey(repoName))
                repoStats.put(repoName, new RepoStats(repoName));

            List<String> result = new ArrayList<>();

            CompilationUnit cu = StaticJavaParser.parse(target);

            cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .forEach(x -> repoStats.get(repoName).addObservation(MethodFinder.getScopeSize(x)));
        }
        catch(Exception e) { e.printStackTrace(); }
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
