package com.aicodereview.agent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.aicodereview.agent.review.ReviewActivityPublisher;
import com.aicodereview.agent.review.ReviewActivityType;
import com.aicodereview.agent.review.ReviewContext;
import com.aicodereview.agent.review.ReviewContextManager;

@Component
public class RepositoryTools {

    private static final Logger log =
            LoggerFactory.getLogger(RepositoryTools.class);

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            ".idea",
            ".vscode",
            ".mvn",
            "target",
            "node_modules",
            "build",
            "dist"
    );

    private final ReviewContextManager reviewContextManager;
    private final ReviewActivityPublisher reviewActivityPublisher;

    public RepositoryTools(
            ReviewContextManager reviewContextManager,
            ReviewActivityPublisher reviewActivityPublisher) {

        this.reviewContextManager = reviewContextManager;
        this.reviewActivityPublisher = reviewActivityPublisher;
    }

    @Tool(description = """
            Lists files and directories within the repository.

            Use this tool to explore a specific directory when necessary.

            The relativePath must be relative to the repository root.
            Use "." to list the repository root.

            Never invent a reviewId. Use exactly the reviewId
            provided in the review request.
            """)
    public List<String> listFiles(

            @ToolParam(description = """
                    Active review ID.
                    Use exactly the reviewId provided in the review prompt.
                    """)
            String reviewId,

            @ToolParam(description = """
                    Directory path relative to the repository root.
                    Use "." to list the repository root.
                    Example: src/main/java
                    """)
            String relativePath) throws IOException {

        Path path =
                resolveSecurePath(reviewId, relativePath);

        log.info(
                "TOOL CALLED: listFiles | reviewId={} | relativePath={}",
                reviewId,
                relativePath);

        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.REPOSITORY_INSPECTION,
                "Inspecting directory: " + relativePath);

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    "Path is not a directory: "
                            + relativePath);
        }

        try (var paths = Files.list(path)) {

            return paths
                    .filter(child ->
                            !Files.isDirectory(child)
                                    || !IGNORED_DIRECTORIES.contains(
                                            child.getFileName().toString()))
                    .map(child -> {

                        Path relative =
                                reviewContextManager
                                        .get(reviewId)
                                        .repositoryRoot()
                                        .relativize(child);

                        return relative
                                .toString()
                                .replace("\\", "/");
                    })
                    .sorted()
                    .toList();
        }
    }

    @Tool(description = """
            Reads the contents of a text file within the repository.

            Use this tool when you need to inspect the actual source
            code or configuration of a file discovered in the repository.

            The relativePath must be relative to the repository root.

            Never invent a reviewId. Use exactly the reviewId
            provided in the review request.
            """)
    public String readFile(

            @ToolParam(description = """
                    Active review ID.
                    Use exactly the reviewId provided in the review prompt.
                    """)
            String reviewId,

            @ToolParam(description = """
                    File path relative to the repository root.
                    Example:
                    src/main/java/com/example/MyService.java
                    """)
            String relativePath) throws IOException {

        Path path =
                resolveSecurePath(reviewId, relativePath);

        log.info(
                "TOOL CALLED: readFile | reviewId={} | relativePath={}",
                reviewId,
                relativePath);

        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.FILE_READING,
                "Reading file: " + relativePath);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Path is not a file: "
                            + relativePath);
        }

        return Files.readString(path);
    }

    @Tool(description = """
            Searches searchable text files within the repository
            for a given search term.

            Use this tool when you need to locate classes, methods,
            annotations, configuration values, or other code before
            deciding which files to read.

            Returns repository-relative file paths, line numbers,
            and matching lines.

            Never invent a reviewId. Use exactly the reviewId
            provided in the review request.
            """)
    public List<String> searchCode(

            @ToolParam(description = """
                    Active review ID.
                    Use exactly the reviewId provided in the review prompt.
                    """)
            String reviewId,

            @ToolParam(description = """
                    Text to search for in repository files.
                    Examples:
                    @RestController
                    ChatClient
                    catch
                    Files.list
                    """)
            String searchTerm) throws IOException {

        ReviewContext context =
                reviewContextManager.get(reviewId);

        Path repositoryRoot =
                context.repositoryRoot();

        log.info(
                "TOOL CALLED: searchCode | reviewId={} | searchTerm={}",
                reviewId,
                searchTerm);

        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.CODE_SEARCH,
                "Searching code for: " + searchTerm);

        try (var paths = Files.walk(repositoryRoot)) {

            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSearchableFile)
                    .filter(path ->
                            !isIgnoredPath(
                                    repositoryRoot,
                                    path))
                    .flatMap(path ->
                            findMatches(
                                    repositoryRoot,
                                    path,
                                    searchTerm)
                                    .stream())
                    .limit(100)
                    .toList();
        }
    }

    @Tool(description = """
            Returns the repository file tree recursively.

            Use this tool first to understand the overall repository structure
            before deciding which files to inspect.

            Ignored directories such as .git, target, node_modules,
            build and dist are excluded.

            Never invent a reviewId. Use exactly the reviewId
            provided in the review request.
            """)
    public List<String> getRepositoryTree(

            @ToolParam(description = """
                    Active review ID.
                    Use exactly the reviewId provided in the review prompt.
                    """)
            String reviewId) throws IOException {

        ReviewContext context =
                reviewContextManager.get(reviewId);

        Path repositoryRoot =
                context.repositoryRoot();

        log.info(
                "TOOL CALLED: getRepositoryTree | reviewId={}",
                reviewId);

        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.REPOSITORY_INSPECTION,
                "Inspecting repository structure");

        try (var paths = Files.walk(repositoryRoot)) {

            return paths
                    .filter(path ->
                            !path.equals(repositoryRoot))
                    .filter(path ->
                            !isIgnoredPath(
                                    repositoryRoot,
                                    path))
                    .map(path -> {

                        String relativePath =
                                repositoryRoot
                                        .relativize(path)
                                        .toString()
                                        .replace("\\", "/");

                        if (Files.isDirectory(path)) {
                            return relativePath + "/";
                        }

                        return relativePath;
                    })
                    .sorted()
                    .limit(500)
                    .toList();
        }
    }

    private Path resolveSecurePath(
            String reviewId,
            String relativePath) {

        ReviewContext context =
                reviewContextManager.get(reviewId);

        Path repositoryRoot =
                context.repositoryRoot();

        Path resolved =
                repositoryRoot
                        .resolve(relativePath)
                        .normalize();

        if (!resolved.startsWith(repositoryRoot)) {

            throw new IllegalArgumentException(
                    "Access outside repository is not allowed: "
                            + relativePath);
        }

        return resolved;
    }

    private boolean isSearchableFile(
            Path path) {

        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase();

        return fileName.endsWith(".java")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".md")
                || fileName.endsWith(".json");
    }

    private boolean isIgnoredPath(
            Path repositoryRoot,
            Path path) {

        Path relativePath =
                repositoryRoot.relativize(path);

        for (Path part : relativePath) {

            if (IGNORED_DIRECTORIES.contains(
                    part.toString())) {

                return true;
            }
        }

        return false;
    }

    private List<String> findMatches(
            Path repositoryRoot,
            Path file,
            String searchTerm) {

        List<String> matches =
                new ArrayList<>();

        try {

            List<String> lines =
                    Files.readAllLines(file);

            for (int i = 0;
                 i < lines.size();
                 i++) {

                String line =
                        lines.get(i);

                if (line.toLowerCase()
                        .contains(
                                searchTerm.toLowerCase())) {

                    String relativePath =
                            repositoryRoot
                                    .relativize(file)
                                    .toString()
                                    .replace("\\", "/");

                    matches.add(
                            relativePath
                                    + ":"
                                    + (i + 1)
                                    + ": "
                                    + line.trim());
                }
            }

        } catch (IOException e) {

            log.debug(
                    "Unable to search file: {}",
                    file,
                    e);
        }

        return matches;
    }
}