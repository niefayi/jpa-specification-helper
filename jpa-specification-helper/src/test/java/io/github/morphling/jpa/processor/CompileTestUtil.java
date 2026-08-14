package io.github.morphling.jpa.processor;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Minimal {@code javax.tools.JavaCompiler} harness that compiles in-memory
 * sources with the {@link EntityConditionProcessor} explicitly enabled, so the
 * processor's compile-time validation and constants generation can be tested
 * without external dependencies.
 *
 * @author anyifei
 */
final class CompileTestUtil {

    private static final String PROCESSOR_CLASS =
            "io.github.morphling.jpa.processor.EntityConditionProcessor";

    private CompileTestUtil() {
    }

    /** A named source unit; the body must not include a package declaration. */
    static final class Source {
        final String className;
        final String body;

        Source(String className, String body) {
            this.className = className;
            this.body = body;
        }
    }

    static final class Result {
        final boolean success;
        final List<String> diagnostics;
        final Map<String, String> generatedSources;

        Result(boolean success, List<String> diagnostics, Map<String, String> generatedSources) {
            this.success = success;
            this.diagnostics = diagnostics;
            this.generatedSources = generatedSources;
        }

        String allDiagnostics() {
            StringBuilder sb = new StringBuilder();
            for (String d : diagnostics) {
                sb.append(d).append('\n');
            }
            return sb.toString();
        }
    }

    static Result compile(List<Source> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        List<JavaFileObject> units = new ArrayList<>();
        for (Source src : sources) {
            units.add(new InMemorySource(src.className, withPackage(src.className, src.body)));
        }

        Path outDir = tempDir("jpa-test-classes");
        Path srcOutDir = tempDir("jpa-test-src");

        List<String> options = new ArrayList<>(Arrays.asList(
                "-classpath", System.getProperty("java.class.path"),
                "-processor", PROCESSOR_CLASS,
                "-d", outDir.toString(),
                "-s", srcOutDir.toString()
        ));

        JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, diagnostics, options, null, units);
        Boolean ok = task.call();
        return new Result(ok != null && ok, collect(diagnostics), generatedSources(srcOutDir));
    }

    private static String withPackage(String className, String body) {
        int idx = className.lastIndexOf('.');
        if (idx < 0) {
            return body;
        }
        return "package " + className.substring(0, idx) + ";\n\n" + body;
    }

    private static Path tempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> collect(DiagnosticCollector<JavaFileObject> diagnostics) {
        List<String> result = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String source = d.getSource() != null ? " @ " + d.getSource().getName() : "";
            result.add(d.getKind() + ": " + d.getMessage(null) + source);
        }
        return result;
    }

    private static Map<String, String> generatedSources(Path srcOutDir) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.isDirectory(srcOutDir)) {
            return result;
        }
        try (Stream<Path> walk = Files.walk(srcOutDir)) {
            walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String rel = srcOutDir.relativize(p).toString().replace(File.separatorChar, '.');
                        String name = rel.substring(0, rel.length() - ".java".length());
                        try {
                            result.put(name, new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;

        InMemorySource(String className, String content) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
