package methodFinder;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

//Identifies methods in repos and does a naive classification of the method types into the following categories: Accessor, Builder, Assertion, Others
public class ChainIdentifier implements Runnable {

    public File[] repos;
    protected final StringBuilder identifier;
    protected final int UPPER_BOUND;
    protected final int LOWER_BOUND;

    protected ArrayList<String> failed_to_parse;

    protected HashMap<String, ArrayList<String>> repo_methods;

    public ChainIdentifier(File[] repos, int LOWER_BOUND, int UPPER_BOUND)
    {
        this.repos = repos;
        this.identifier = new StringBuilder();
        this.UPPER_BOUND = UPPER_BOUND;
        this.LOWER_BOUND = LOWER_BOUND;
        repo_methods = new HashMap<>();

        failed_to_parse = new ArrayList<>();
    }

    @Override
    public void run() {
        for(int i = LOWER_BOUND; i < UPPER_BOUND; i++)
        {
            if(repos[i].isFile())
            {
                try {
                    processFile(repos[i], repos[i].getName());
                }
                catch (Exception e) { e.printStackTrace(); }
            }
            else {
                for (File f : Objects.requireNonNull(repos[i].listFiles()))
                        processFile(f, repos[i].getName());
            }
        }
    }

    protected void processFile(File file, String repoName)
    {
        FileInputStream target = null;
        try {
            target = new FileInputStream(file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

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

            var chains = cu.stream(Node.TreeTraversal.PREORDER)
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .collect(Collectors.toList());

            ArrayList<Optional<Range>> visited_ranges = new ArrayList<>();

            String prev_scope = "";
            for(int i = 0; i < chains.size(); i++)
            {
                Optional<Range> r = chains.get(i).getRange();
                if(visited_ranges.contains(r))
                {
                    chains.remove(i);
                    continue;
                }

                visited_ranges.add(r);

                String cur_scope = chains.get(i).getScope().toString();
                cur_scope = cur_scope.substring("Optional[".length(), cur_scope.length() - 1);

                //System.out.println(chains.get(i).getNameAsString() + ", scope: " + cur_scope);

                if(prev_scope.contains("(") && prev_scope.contains(cur_scope))
                    chains.remove(i);

                prev_scope = cur_scope;
            }

            chains.forEach(x -> {
                if(result.contains(x.getNameAsString()) == false) {
                    result.add(x.getNameAsString());
                }
            });

        }
        catch(Exception e) {
            failed_to_parse.add("Failed to parse " + repoName + " :: " + file.getName() + "\n");
        }
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

    //todo: replace hardcode with config
    public List<String> getRepoMethodNames(String repo)
    {
        List<String> result = new ArrayList<>();

        final StringBuilder sb = new StringBuilder();

        for(String method : repo_methods.get(repo))
        {
            sb.append(method);

            String lower = method.toLowerCase();
            if(lower.contains("get") || lower.contains("for") || lower.contains("set") || lower.contains("with") || lower.equals("pop") || lower.equals("push") || lower.equals("peek") || lower.equals("valueof"))
                sb.append(",").append("accessor");
            else if(lower.contains("build") || lower.contains("make") || lower.contains("create") || (lower.length() >= 2 && lower.substring(0,2).equals("to")) || lower.equals("split"))
                sb.append(",").append("builder");
            else if(lower.contains("assert") || lower.contains("is"))
                sb.append(",").append("assertion");
            else
                sb.append(",").append("others");

            sb.append("\n");

            result.add(sb.toString());

            sb.setLength(0);
        }

        return result;
    }

    public final ArrayList<String> getFails()
    {
        return failed_to_parse;
    }
}
