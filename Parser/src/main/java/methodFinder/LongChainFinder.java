package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

//Find the files with the longest chains per repo
public class LongChainFinder implements Runnable {

    public List<List<String>> data;
    public File[] repos;
    protected final StringBuilder identifier;
    protected final int UPPER_BOUND;
    protected final int LOWER_BOUND;

    protected HashMap<String, LongChain[]> longest_chains;

    protected class LongChain {
        public final int length;
        public final String repo;
        public final String filename;

        public LongChain(int length, String repo, String filename)
        {
            this.length = length;
            this.repo = repo;
            this.filename = filename;
        }
    }

    public LongChainFinder(File[] repos, int LOWER_BOUND, int UPPER_BOUND)
    {
        this.repos = repos;
        this.identifier = new StringBuilder();
        this.UPPER_BOUND = UPPER_BOUND;
        this.LOWER_BOUND = LOWER_BOUND;

        longest_chains = new HashMap<>();
        for(var f : repos)
            longest_chains.put(f.getName(), new LongChain[LongChainSearchRunner.chains_per_repo]);
    }

    @Override
    public void run() {
        for(int i = LOWER_BOUND; i < UPPER_BOUND; i++)
        {
            if(repos[i].isFile())
                throw new IllegalStateException("Cannot classify files without a repo's method classifications");
            else {
                try
                {
                    for (File f : Objects.requireNonNull(repos[i].listFiles())) {
                        try {
                            processFile(new FileInputStream(f.getAbsolutePath()), repos[i].getName(), f.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    protected void processFile(FileInputStream target, String repoName, String filename)
    {
        //System.out.println("Process " + repoName + " :: " + filename);
        try
        {
            CompilationUnit cu = StaticJavaParser.parse(target);

            var chains = cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .collect(Collectors.toList());

            LongChain[] longChains = longest_chains.get(repoName);

            //System.out.println("Chains.length " + chains.size());

            for(int i = 0; i < chains.size(); i++)
            {
                int scope_size = MethodFinder.getScopeSize(chains.get(i));

                //System.out.println("Chain " + i + " size: " + scope_size);

                for(int k = 0; k < longChains.length; k++)
                {
                    if(longChains[k] == null)
                    {
                        longChains[k] = new LongChain(scope_size, repoName, filename);
                        break;
                    }
                    else if(longChains[k].length < scope_size)
                    {
                        for(int n = k + 1; n < longChains.length; n++)
                            longChains[n] = longChains[n - 1];

                        longChains[k] = new LongChain(scope_size, repoName, filename);
                        break;
                    }
                }
            }

            longest_chains.put(repoName, longChains);
        }
        catch(Exception e) { System.out.println(repoName + " :: " + filename + " contains lines that can't be parsed"); } //e.printStackTrace(); }
    }

    public final ArrayList<String> getResult()
    {
        if(longest_chains == null)
            return null;

        ArrayList<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for(var repo : repos)
        {
            if(longest_chains.containsKey(repo.getName()) == false)
                continue;;

            for(var long_chain : longest_chains.get(repo.getName()))
            {
                if(long_chain == null)
                    continue;

                sb.append(long_chain.length).append(",").append(long_chain.repo).append(",").append(long_chain.filename).append("\n");

                result.add(sb.toString());

                System.out.println(result.get(result.size() - 1));

                sb.setLength(0);
            }
        }

        return result;
    }

}
