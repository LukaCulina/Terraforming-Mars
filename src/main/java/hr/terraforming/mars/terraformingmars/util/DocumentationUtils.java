package hr.terraforming.mars.terraformingmars.util;

import javafx.scene.control.Alert.AlertType;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class DocumentationUtils {
    
    private static final String DOC_FOLDER = "documentation";
    private static final String DOC_FILE = DOC_FOLDER + "/project-documentation.html";
    private static final String UL_START = "<ul>";
    private static final String UL_END = "</ul>";
    private static final String LI_START = "<li>";
    private static final String LI_END = "</li>";
    private static final String CODE_START = "<code>";
    private static final String CODE_END = "</code>";

    private static final String HTML_START = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Project Documentation</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; margin: 2em; background-color: #1a2026; color: #EAEAEA; }
                    h1 { color: #FFA500; border-bottom: 3px solid #FFA500; padding-bottom: 10px; }
                    h2 { color: #e09500; border-bottom: 1px solid #4f4f4f; padding-bottom: 5px; margin-top: 2em; }
                    h3 { color: #d1d1d1; }
                    hr { border: 1px solid #4f4f4f; margin-top: 2em;}
                    code { background-color: #2E3A47; padding: 3px 6px; border-radius: 4px; font-family: "SF Mono", "Fira Code", "Fira Mono", "Roboto Mono", monospace; }
                    .annotation { color: #FFC66D; }
                    ul { list-style-type: none; padding-left: 0; }
                    li { background-color: #262f38; margin-bottom: 8px; padding: 10px; border-radius: 4px; border-left: 3px solid #FFA500; }
                </style>
            </head>
            <body>
                <h1>Terraforming Mars - Project Documentation</h1>""";
    private static final String HTML_END = "</body></html>";

    private DocumentationUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void generateDocumentation() {
        StringBuilder docBuilder = new StringBuilder(HTML_START);
        try {
            Path projectSrcPath = Paths.get("src/main/java");
            try (Stream<Path> stream = Files.walk(projectSrcPath)) {
                stream.filter(path -> path.toString().endsWith(".java") && !path.getFileName().toString().equals("module-info.java"))
                        .sorted()
                        .forEach(path -> {
                            try {
                                String className = getFullyQualifiedClassName(projectSrcPath, path);
                                Class<?> clazz = Class.forName(className);
                                appendClassDocumentation(docBuilder, clazz);
                            } catch (ClassNotFoundException e) {
                                log.error("Could not find class for path: {}", path, e);
                            }
                        });
            }
            docBuilder.append(HTML_END);
            writeHtmlFile(docBuilder.toString());
            DialogUtils.showDialog(AlertType.INFORMATION, "Success", "Documentation generated successfully!");
        } catch (IOException e) {
            DialogUtils.showDialog(AlertType.ERROR, "Error", "Failed to generate documentation: " + e.getMessage());
            log.error("Failed to generate documentation.", e);
        }
    }

    private static String getFullyQualifiedClassName(Path basePath, Path fullPath) {
        String relativePath = basePath.relativize(fullPath).toString();
        return relativePath.replace(".java", "").replace(java.io.File.separatorChar, '.');
    }

    private static void appendClassDocumentation(StringBuilder sb, Class<?> clazz) {
        if (clazz.isSynthetic()) return;

        sb.append("<h2>");
        appendAnnotations(sb, clazz);
        sb.append(Modifier.toString(clazz.getModifiers())).append(" ")
                .append(getProgrammaticTypeName(clazz))
                .append("</h2>");

        sb.append("<p><b>Full Name:</b> ").append(clazz.getName()).append("</p>");
        appendFields(sb, clazz);
        appendConstructors(sb, clazz);
        appendMethods(sb, clazz);
        sb.append("<hr>");
    }

    private static void appendAnnotations(StringBuilder sb, AnnotatedElement element) {
        Annotation[] annotations = element.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            sb.append("<span class='annotation'>@").append(annotation.annotationType().getSimpleName()).append("</span><br>");
        }
    }

    private static void appendFields(StringBuilder sb, Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length == 0) return;
        sb.append("<h3>Fields</h3>").append(UL_START);
        for (Field field : fields) {
            if (field.isSynthetic()) continue;
            sb.append(LI_START);
            appendAnnotations(sb, field);
            String modifiers = Modifier.toString(field.getModifiers());
            String typeName = getProgrammaticTypeName(field.getType());
            sb.append(CODE_START)
                    .append(modifiers).append(" ").append(typeName).append(" ").append(field.getName())
                    .append(CODE_END).append(LI_END);
        }
        sb.append(UL_END);
    }

    private static void appendConstructors(StringBuilder sb, Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 0) return;

        sb.append("<h3>Constructors</h3>").append(UL_START);
        for (Constructor<?> constructor : constructors) {
            if (constructor.isSynthetic()) continue;
            sb.append(LI_START);
            appendAnnotations(sb, constructor);
            String modifiers = Modifier.toString(constructor.getModifiers());
            String params = Arrays.stream(constructor.getParameters())
                    .map(p -> getProgrammaticTypeName(p.getType()) + " " + p.getName())
                    .collect(Collectors.joining(", "));
            sb.append(CODE_START)
                    .append(modifiers).append(" ").append(clazz.getSimpleName()).append("(").append(params).append(")")
                    .append(CODE_END).append(LI_END);
        }
        sb.append(UL_END);
    }

    private static void appendMethods(StringBuilder sb, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length == 0) return;

        sb.append("<h3>Methods</h3>").append(UL_START);
        for (Method method : methods) {
            boolean isSyntheticOrBridge = method.isSynthetic() || method.isBridge();
            boolean isGeneratedRecordMethod = clazz.isRecord() &&
                    (method.getName().equals("equals") || method.getName().equals("hashCode") || method.getName().equals("toString"));
            if (isSyntheticOrBridge || isGeneratedRecordMethod) {
                continue;
            }
            sb.append(LI_START);
            appendAnnotations(sb, method);
            String modifiers = Modifier.toString(method.getModifiers());
            String returnType = getProgrammaticTypeName(method.getReturnType());
            String params = Arrays.stream(method.getParameters())
                    .map(p -> getProgrammaticTypeName(p.getType()) + " " + p.getName())
                    .collect(Collectors.joining(", "));
            sb.append(CODE_START)
                    .append(modifiers).append(" ").append(returnType).append(" ").append(method.getName())
                    .append("(").append(params).append(")")
                    .append(CODE_END).append(LI_END);
        }
        sb.append(UL_END);
    }

    private static String getProgrammaticTypeName(Class<?> type) {
        if (type.isRecord()) return "record " + type.getSimpleName();
        if (type.isEnum()) return "enum " + type.getSimpleName();
        return type.getSimpleName();
    }

    private static void writeHtmlFile(String htmlContent) throws IOException {
        Files.createDirectories(Paths.get(DOC_FOLDER));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DOC_FILE))) {
            writer.write(htmlContent);
        }
    }
}