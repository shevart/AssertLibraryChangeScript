package com.shevart;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    static ArrayList<String> testFolders = new ArrayList<>();

    static {
        testFolders.add("/Users/shevart/Programming/Work/ProjectName");
    }

    public static void main(String[] args) {
        for (String path : testFolders) {
            try {
                performTestFolder(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Done!");
    }

    private static void performTestFolder(String path) throws IOException {
        final List<TestFileData> filesWithOldLib = getAllFilesWithOldAssertLib(path);

        int lines = 0;
        for (TestFileData fileData : filesWithOldLib) {
            if (!fileData.file.getAbsolutePath().contains(path)) {
                throw new RuntimeException("No!");
            }
            lines += fixTestFile(fileData);
        }
        System.out.println("Changed lines: " + lines);
    }

    // return changed lines count
    static int fixTestFile(TestFileData fileData) throws IOException {
        final List<String> editedFile = new ArrayList<>();
        int changedLines = 0;

        final String lineSuffix = fileData.isJava() ? ";" : "";
        final Set<String> importsSet = new HashSet<>();
        int importLinesPosition = 3; // default

        int currentLine = 0;
        for (String line : fileData.content) {
            currentLine++;
            if (line.contains("import static org.fest.assertions.api.Assertions.assertThat")) {
//                editedFile.add("// import static org.fest.assertions.api.Assertions.assertThat;");
                importLinesPosition = currentLine;
                changedLines++;
                continue;
            }
            if (line.contains("import static org.fest.assertions.Assertions.*")) {
                // just remove this line
                changedLines++;
                continue;
            }
            if (line.contains("import org.fest.assertions.Assertions;")) {
                // just remove this line
                changedLines++;
                continue;
            }

            if (line.contains("import static org.fest.assertions.api.Assertions.fail")) {
                importsSet.add("import static org.junit.Assert.fail" + lineSuffix);
                changedLines++;
                continue;
            }
            final String prefixSpace = getStartSpaceSymbols(line);
            if (line.trim().startsWith("assertThat") && line.trim().endsWith(";")) {
                if (line.contains("isEqualTo")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    final String expected = extractAssertThatExpectedEqualsResult(line.trim());
                    editedFile.add(prefixSpace + "assertEquals(" + expected + ", " + actual + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertEquals" + lineSuffix);
                    continue;
                }
                if (line.contains("isFalse")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    editedFile.add(prefixSpace + "assertFalse(" + actual + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertFalse" + lineSuffix);
                    continue;
                }
                if (line.contains("isTrue")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    editedFile.add(prefixSpace + "assertTrue(" + actual + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertTrue" + lineSuffix);
                    continue;
                }
                if (line.contains("isNotNull")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    editedFile.add(prefixSpace + "assertNotNull(" + actual + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertNotNull" + lineSuffix);
                    continue;
                }
                if (line.contains("isNull")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    editedFile.add(prefixSpace + "assertNull(" + actual + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertNull" + lineSuffix);
                    continue;
                }
                if (line.contains("hasSize")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    final String expected = extractAssertThatExpectedHasSizeResult(line.trim());
                    editedFile.add(prefixSpace + "assertEquals(" + expected + ", " + actual + ".size());");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertEquals" + lineSuffix);
                    continue;
                }
                if (line.contains("isInstanceOf")) {
                    final String actual = extractAssertThatActualResult(line.trim());
                    final String expected = extractAssertThatExpectedInstanceOfResult(line.trim());
                    editedFile.add(prefixSpace + "assertTrue(" + actual + " instanceof " + expected + ");");
                    changedLines++;
                    importsSet.add("import static org.junit.Assert.assertTrue" + lineSuffix);
                    continue;
                }
            }

            if (line.trim().startsWith("failBecauseExceptionWasNotThrown") && line.trim().endsWith(";")) {
                final String expectedExceptionClass = extractAssertThatExpectedException(line.trim());
                if (expectedExceptionClass.endsWith(".class")) {
                    editedFile.add(prefixSpace + "fail(\"Expected " + expectedExceptionClass + " but it wasn't thrown\");");
                } else {
                    editedFile.add(prefixSpace + "fail(\"Expected \" + " + expectedExceptionClass + ".getName() + \" but it wasn't thrown\");");
                }
                changedLines++;
                importsSet.add("import static org.junit.Assert.fail" + lineSuffix);
                continue;
            }

            editedFile.add(line);
        }

        final int index = importLinesPosition;
        importsSet.forEach(s -> {
            editedFile.add(index, s);
        });

        writeContent(fileData.file, editedFile);
        return changedLines;
    }

    private static String extractAssertThatActualResult(String line) {
        assert line.startsWith("assertThat");
        final int startIndex = 11; // assertThat(
        if (line.contains("isEqualTo")) {
            final int endIndex = line.indexOf("isEqualTo");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("isFalse")) {
            final int endIndex = line.indexOf("isFalse");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("isTrue")) {
            final int endIndex = line.indexOf("isTrue");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("isNotNull")) {
            final int endIndex = line.indexOf("isNotNull");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("isNull")) {
            final int endIndex = line.indexOf("isNull");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("hasSize")) {
            final int endIndex = line.indexOf("hasSize");
            return line.substring(startIndex, endIndex - 2);
        } else if (line.contains("isInstanceOf")) {
            final int endIndex = line.indexOf("isInstanceOf");
            return line.substring(startIndex, endIndex - 2);
        }
        throw new RuntimeException("Wow");
    }

    private static String extractAssertThatExpectedEqualsResult(String line) {
        final int startIndex = line.indexOf("isEqualTo") + 10; // isEqualTo(
        return line.substring(startIndex, line.length() - 2);
    }

    // failBecauseExceptionWasNotThrown
    private static String extractAssertThatExpectedException(String line) {
        final int startIndex = 33; // failBecauseExceptionWasNotThrown(
        return line.substring(startIndex, line.length() - 2);
    }

    private static String extractAssertThatExpectedHasSizeResult(String line) {
        final int startIndex = line.indexOf("hasSize") + 8; // hasSize(
        return line.substring(startIndex, line.length() - 2);
    }

    private static String extractAssertThatExpectedInstanceOfResult(String line) {
        final int startIndex = line.indexOf("isInstanceOf") + 13; // isInstanceOf(
        return line.substring(startIndex, line.length() - 2).replace(".class", "");
    }

    private static String getStartSpaceSymbols(String line) {
        int spaceCount = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') {
                spaceCount = i;
                break;
            }
        }
        String result = "";
        for (int i = 0; i < spaceCount; i++) {
            result += " ";
        }
        return result;
    }

    private static void writeContent(File file, List<String> content) throws IOException {
        FileWriter writer = new FileWriter(file);
        for (String s : content) {
            writer.append(s);
            writer.append("\n");
        }
        writer.flush();
        writer.close();
    }

    static List<TestFileData> getAllFilesWithOldAssertLib(String path) throws IOException {
        final File resourceFile = openTestFile(path);
        final List<File> javaFiles = collectAllJavaKtFiles(resourceFile);
        final List<TestFileData> filesWithOldLib = new ArrayList<>();
        for (File f : javaFiles) {
            if (isFileHasOldAssertsLib(f)) {
                filesWithOldLib.add(fileToFileData(f));
            }
        }
        System.out.println("Test classes with old lib count: " + filesWithOldLib.size());
        System.out.println("Total test classes count: " + javaFiles.size());
        for (TestFileData f : filesWithOldLib) {
//            System.out.println("-- " + f.file.getName());
        }
        return filesWithOldLib;
    }

    static boolean isFileHasOldAssertsLib(File file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("org.fest.assertions")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    static TestFileData fileToFileData(File file) throws IOException {
        final List<String> fileContent = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                fileContent.add(line);
            }
        } finally {
            br.close();
        }
        return new TestFileData(file, fileContent);
    }

    static List<File> collectAllJavaKtFiles(File file) {
        final List<File> result = new ArrayList<>();
        if (file.isDirectory() && file.listFiles() != null) {
            for (File innerFile : file.listFiles()) {
                result.addAll(collectAllJavaKtFiles(innerFile));
            }
        }
        if (file.isFile() && (file.getAbsolutePath().contains(".java") || file.getAbsolutePath().contains(".kt"))) {
            result.add(file);
        }
        return result;
    }

    static File openTestFile(String path) {
        return new File(path);
    }
}
