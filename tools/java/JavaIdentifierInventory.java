import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Emits every declared Java identifier without depending on source formatting. */
public final class JavaIdentifierInventory {
    private JavaIdentifierInventory() {}

    private static String methodKey(MethodTree method) {
        StringBuilder result = new StringBuilder();
        result.append(method.getName()).append('(');
        for (int index = 0; index < method.getParameters().size(); index++) {
            if (index != 0) result.append(',');
            result.append(method.getParameters().get(index).getType());
        }
        return result.append(')').toString();
    }

    private static final class Scanner extends TreePathScanner<Void, Void> {
        private final Trees trees;
        private final CompilationUnitTree unit;
        private String method = "<class>";

        Scanner(Trees trees, CompilationUnitTree unit) {
            this.trees = trees;
            this.unit = unit;
        }

        private String owner() {
            List<String> names = new ArrayList<>();
            for (TreePath path = getCurrentPath(); path != null; path = path.getParentPath()) {
                if (path.getLeaf() instanceof ClassTree type && !type.getSimpleName().isEmpty()) {
                    names.add(type.getSimpleName().toString());
                }
            }
            Collections.reverse(names);
            return String.join(".", names);
        }

        private long line(Tree tree) {
            long position = trees.getSourcePositions().getStartPosition(unit, tree);
            return position < 0 ? -1 : unit.getLineMap().getLineNumber(position);
        }

        private String role(VariableTree variable) {
            TreePath parentPath = getCurrentPath().getParentPath();
            Tree parent = parentPath == null ? null : parentPath.getLeaf();
            if (parent instanceof ClassTree) return "field";
            if (parent instanceof MethodTree methodTree
                    && methodTree.getParameters().contains(variable)) return "parameter";
            if (parent != null && parent.getKind() == Tree.Kind.CATCH) return "catch";
            if (parent != null && parent.getKind() == Tree.Kind.ENHANCED_FOR_LOOP) return "iteration";
            if (parent != null && parent.getKind() == Tree.Kind.FOR_LOOP) return "counter";
            if (parent != null && parent.getKind() == Tree.Kind.TRY) return "resource";
            return "local";
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            String previous = method;
            method = methodKey(tree);
            super.visitMethod(tree, unused);
            method = previous;
            return null;
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            System.out.println(
                    new File(unit.getSourceFile().toUri()).getName()
                            + "\t" + line(tree)
                            + "\t" + owner()
                            + "\t" + method
                            + "\t" + role(tree)
                            + "\t" + tree.getName()
                            + "\t" + tree.getType());
            return super.visitVariable(tree, unused);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: JavaIdentifierInventory JAVA_SOURCE ...");
            System.exit(2);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        List<File> files = new ArrayList<>();
        for (String arg : args) files.add(new File(arg));
        Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(files);
        JavacTask task = (JavacTask) compiler.getTask(
                null, manager, null, List.of("-proc:none"), null, units);
        Trees trees = Trees.instance(task);
        for (CompilationUnitTree unit : task.parse()) {
            new Scanner(trees, unit).scan(unit, null);
        }
        manager.close();
    }
}
