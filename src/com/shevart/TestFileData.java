package com.shevart;

import java.io.File;
import java.util.List;

public class TestFileData {
    final File file;
    final List<String> content;

    public TestFileData(File file, List<String> content) {
        this.file = file;
        this.content = content;
    }

    boolean isJava() {
        return file.getAbsolutePath().endsWith(".java");
    }

    boolean isKotlin() {
        return file.getAbsolutePath().endsWith(".kt");
    }
}
