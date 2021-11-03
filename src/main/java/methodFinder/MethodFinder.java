package methodFinder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

public class MethodFinder {
    public static void main(String[] args) throws FileNotFoundException {

        CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/testRepo/ReversePolishNotation.java"));
        new MethodNameFinder().visit(cu,null);

    }

    private static class MethodNameFinder extends VoidVisitorAdapter<Void>{

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            super.visit(md, arg);
            System.out.println("Method name Printed: " + md.getName());
            md.getScope().filter(Expression::isMethodCallExpr).map(Expression::asMethodCallExpr).ifPresent(
                    (temp) -> System.out.println("\tin scope: " + temp.getName())
            );
//            List<Node> childList = md.getChildNodes();

        }
    }


}
