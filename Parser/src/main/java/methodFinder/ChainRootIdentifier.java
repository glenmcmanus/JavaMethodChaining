package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ChainRootIdentifier implements Runnable {

    public List<List<String>> data;
    public File[] repos;
    protected final StringBuilder identifier;
    protected final int UPPER_BOUND;
    protected final int LOWER_BOUND;

    protected HashMap<String, ArrayList<String>> repo_methods;

    public ChainRootIdentifier(File[] repos, int LOWER_BOUND, int UPPER_BOUND)
    {
        this.repos = repos;
        this.identifier = new StringBuilder();
        this.UPPER_BOUND = UPPER_BOUND;
        this.LOWER_BOUND = LOWER_BOUND;
        data = new ArrayList<>();
        repo_methods = new HashMap<>();
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
            ArrayList<String> result;
            if(!repo_methods.containsKey(repoName))
            {
                result = new ArrayList<>();
                repo_methods.put(repoName, result);
            }
            else
                result = repo_methods.get(repoName);

            CompilationUnit cu = StaticJavaParser.parse(target);

            cu.getTypes().stream()
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .filter(x -> MethodFinder.getScopeSize(x) == 1)
                    .forEach(x -> { if(result.contains(x.getNameAsString()) == false) { result.add(x.getNameAsString()); } });
        }
        catch(Exception e) { e.printStackTrace(); }
    }

    public Set<String> getRepoNames()
    {
        return repo_methods.keySet();
    }

    public List<String> getMethodNames()
    {
        List<String> result = new ArrayList<>();

        final StringBuilder sb = new StringBuilder();

        for(String repo : repo_methods.keySet()) {
            sb.append(repo);

            for(String method : repo_methods.get(repo))
            {
                sb.append(",").append(method).append("\n");
                result.add(sb.toString());

                sb.setLength(repo.length());
            }

            sb.setLength(0);
        }

        return result;
    }

    public List<String> getRepoMethodNames(String repo)
    {
        List<String> result = new ArrayList<>();

        final StringBuilder sb = new StringBuilder();

        for(String method : repo_methods.get(repo))
        {
            sb.append(method);

            String lower = method.toLowerCase();
            if(lower.contains("get") || lower.contains("is") || method.equals("toString"))
                sb.append(",").append("accessor");
            else if(lower.contains("set"))
                sb.append(",").append("mutator");
            else if(lower.contains("make") || lower.contains("create"))
                sb.append(",").append("builder");
            else if(lower.contains("assert"))
                sb.append(",").append("assertion");
            else
                sb.append(",").append("others");

            sb.append("\n");

            result.add(sb.toString());

            sb.setLength(0);
        }

        return result;
    }

}
