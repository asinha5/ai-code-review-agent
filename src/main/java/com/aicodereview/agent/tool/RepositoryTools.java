package com.aicodereview.agent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.aicodereview.agent.review.ReviewContext;
import com.aicodereview.agent.review.ReviewContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public RepositoryTools(ReviewContextManager reviewContextManager) {
        this.reviewContextManager = reviewContextManager;
    }

    @Tool(description = """
            Lists files and directories within the repository.
            The path must be relative to the repository root.
            Use "." to list the repository root.
            """)
    public List<String> listFiles(
            String reviewId,
            String relativePath) throws IOException {

        Path path = resolveSecurePath(reviewId, relativePath);

        log.info(
                "TOOL CALLED: listFiles | reviewId={} | relativePath={}",
                reviewId,
                relativePath);

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    "Path is not a directory: " + relativePath);
        }

        try (var paths = Files.list(path)) {
                return paths
                        .filter(child ->
                                !Files.isDirectory(child)
                                || !IGNORED_DIRECTORIES.contains(
                                        child.getFileName().toString()))
                        .map(child -> {
                            Path relative = reviewContextManager
                                    .get(reviewId)
                                    .repositoryRoot()
                                    .relativize(child);
            
                            return relative.toString()
                                    .replace("\\", "/");
                        })
                        .sorted()
                        .toList();
            }
    }

    @Tool(description = """
            Reads a text file within the repository.
            The file path must be relative to the repository root.
            """)
    public String readFile(
            String reviewId,
            String relativePath) throws IOException {

        Path path = resolveSecurePath(reviewId, relativePath);

        log.info(
                "TOOL CALLED: readFile | reviewId={} | relativePath={}",
                reviewId,
                relativePath);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Path is not a file: " + relativePath);
        }

        return Files.readString(path);
    }

    private Path resolveSecurePath(
            String reviewId,
            String relativePath) {

        ReviewContext context = reviewContextManager.get(reviewId);

        Path repositoryRoot = context.repositoryRoot();

        Path resolved = repositoryRoot
                .resolve(relativePath)
                .normalize();

        

        if (!resolved.startsWith(repositoryRoot)) {
            throw new IllegalArgumentException(
                    "Access outside repository is not allowed: "
                            + relativePath);
        }

        return resolved;
    }

    @Tool(description = """
        Searches text files inside the repository for a given search term.
        Returns matching relative file paths and matching lines.
        Searches only within the repository root.
        """)
public List<String> searchCode(
        String reviewId,
        String searchTerm) throws IOException {

    ReviewContext context = reviewContextManager.get(reviewId);
    Path repositoryRoot = context.repositoryRoot();

    log.info(
        "TOOL CALLED: searchCode | reviewId={} | searchTerm={}",
        reviewId,
        searchTerm);

    try (var paths = Files.walk(repositoryRoot)) {

        return paths
                .filter(Files::isRegularFile)
                .filter(this::isSearchableFile)
                .filter(path -> !isIgnoredPath(repositoryRoot, path))
                .flatMap(path -> findMatches(repositoryRoot, path, searchTerm).stream())
                .limit(100)
                .toList();
    }
}

private boolean isSearchableFile(Path path) {

        String fileName = path.getFileName()
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

    Path relativePath = repositoryRoot.relativize(path);

    for (Path part : relativePath) {
        if (IGNORED_DIRECTORIES.contains(part.toString())) {
            return true;
        }
    }

    return false;
}

private List<String> findMatches(
        Path repositoryRoot,
        Path file,
        String searchTerm) {

    List<String> matches = new ArrayList<>();

    try {
        List<String> lines = Files.readAllLines(file);

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line.toLowerCase()
                    .contains(searchTerm.toLowerCase())) {

                String relativePath = repositoryRoot
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
        // Ignore unreadable files for now.
    }

    return matches;
}
}
