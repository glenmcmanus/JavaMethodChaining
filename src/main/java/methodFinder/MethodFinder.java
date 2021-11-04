package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileNotFoundException;

public class MethodFinder {

    public static void main(String[] args) throws FileNotFoundException {

        CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/testRepo/ReversePolishNotation.java"));

        cu.getTypes().stream()
            .flatMap(x -> x.findAll(MethodCallExpr.class).stream())
            .forEach(x -> System.out.println(x.toString() + "\nMethod Chains = " + getScopeSize(x)));


    }

    protected static int getScopeSize(MethodCallExpr expr) {
        return expr.getScope()
            .filter(Expression::isMethodCallExpr)
            .map(Expression::asMethodCallExpr)
            .map(
                val -> getScopeSize(val) + 1
            )
            .orElse(1);
    }

}
