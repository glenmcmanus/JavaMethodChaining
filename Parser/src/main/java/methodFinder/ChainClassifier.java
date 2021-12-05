package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

//Using the identification output, classify method chains by size into bins: S, L, XL  and by and type (Accessor, Builder, Assertion, Others)
public class ChainClassifier implements Runnable {

    public File[] repos;
    protected final StringBuilder identifier;
    protected final int UPPER_BOUND;
    protected final int LOWER_BOUND;

    public static final HashMap<String, Integer> bin_mapping = new HashMap<>() {{
        put("accessor", 0);
        put("builder", 1);
        put("assertion", 2);
        put("others", 3);
    }};

    // |Size|Accessor|Builder|Assertion|Others
    // |S   |
    // |L   |
    // |XL  |

    protected int[][] bin_counts = new int[3][4];

    public ChainClassifier(File[] repos, int LOWER_BOUND, int UPPER_BOUND)
    {
        this.repos = repos;
        this.identifier = new StringBuilder();
        this.UPPER_BOUND = UPPER_BOUND;
        this.LOWER_BOUND = LOWER_BOUND;
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
                    File classifications = new File("output/" + repos[i].getName() + "_identifiers.csv");
                    Scanner scanner = new Scanner(classifications);
                    HashMap<String, String> method_classification = new HashMap<>();
                    while(scanner.hasNext())
                    {
                        String[] row = scanner.nextLine().split(",");
                        method_classification.putIfAbsent(row[0], row[1]);
                    }
                    scanner.close();

                    for (File f : Objects.requireNonNull(repos[i].listFiles())) {
                        try {
                            processFile(new FileInputStream(f.getAbsolutePath()), repos[i].getName(), method_classification);
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

    protected void processFile(FileInputStream target, String repoName, HashMap<String, String> method_classification)
    {
        try
        {
            CompilationUnit cu = StaticJavaParser.parse(target);

            var chains = cu.stream(Node.TreeTraversal.PREORDER)
                    .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
                    .distinct()
                    .collect(Collectors.toList());

            String prev_scope = "";
            for(int i = 0; i < chains.size(); i++)
            {
                String cur_scope = chains.get(i).getScope().toString();
                cur_scope = cur_scope.substring("Optional[".length(), cur_scope.length() - 1);

                if(prev_scope.contains("(") && prev_scope.contains(cur_scope))
                    chains.remove(i);

                prev_scope = cur_scope;
            }

            prev_scope = "";
            for(int i = 0; i < chains.size(); i++)
            {
                String cur_scope = chains.get(i).getScope().toString();
                cur_scope = cur_scope.substring("Optional[".length(), cur_scope.length() - 1);

                if(prev_scope.contains(chains.get(i).getNameAsString()))
                {
                    if(method_classification.get(chains.get(i - 1).getNameAsString()) == "builder")
                        chains.remove(i);
                    else
                        chains.remove(i - 1);

                    prev_scope = "";
                }
                else
                    prev_scope = cur_scope;
            }


            chains.forEach(x -> {
                System.out.println(x.getNameAsString() + ", scope: " + x.getScope());

                int scope_size = MethodFinder.getScopeSize(x);
                int bin = 0;

                if(scope_size < 8)
                    bin = 0;
                else if(scope_size < 42)
                    bin = 1;
                else if(scope_size >= 42)
                    bin = 2;

                bin_counts[bin][bin_mapping.get(method_classification.get(x.getNameAsString()))]++;
            });

        }
        catch(Exception e) { e.printStackTrace(); }
    }

    public final int[][] getResult()
    {
        return bin_counts;
    }

}
