/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zparkingb.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 *
 * @author laurent
 */
public class RuntimeHelper {

    public static String buildCommand(String params, File file) throws IOException {
        return buildCommand(params, null, file);
    }

    public static String buildCommand(String params, String program, File file) throws IOException {
        if (params == null)
            throw new NullPointerException("The parameters cannot be null");
        if (program == null) {
            // Should test if on Windows
            program = getDefaultProgram(file);
            if (program == null) {
//                throw new IOException("Do not find how to open \"" + file + "\"");
                return null;
            }
        }

        // Validate the right syntax
        if (!program.contains("%0") && !program.contains("%1")) {
            throw new IOException("Invalid program syntax. Expecting \"%0\" or \"%1\"");
        }

        // Command streamlined for the file parameter
        if (program.contains("%0")) {
            program = program.replaceAll("\"?%1\"?", "");
            program = program.replaceAll("\"?%0\"?", "\"%1\"");
        }
        else {
            program = program.replaceAll("(?<!\")%1(?!\")", "\"%1\"");
        }

        // Add the parameters
        String filepath = file.getPath();
        filepath=filepath.replace("\\", "\\\\");

        if (program.contains("%2")) {
            program = program.replaceAll("\"?%2\"?", params);
            program = program.replaceAll("%1", filepath);
        }
        else if (program.contains("%*")) {
            program = program.replaceAll("\"?%[*]\"?", params);
            program = program.replaceAll("%1", filepath);
        }
        else {
            program = program.replaceAll("\"%1\"", params + " \"" + filepath + "\"");
        }

        // Cleanup non used parameters
        program = program.replaceAll("\"?%[3456789*]\"?", "");

        System.out.println(program);

        return program;
//        Runtime.getRuntime().exec(program);

    }

    private static String command(Runtime rt, String command) throws IOException {
        Process pr = rt.exec("cmd /c " + command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            return reader.readLine();
        }
    }

    private static String getDefaultProgram(File file) throws IOException {

        // pre-conditions and file extension
        if (file == null)
            throw new NullPointerException("File cannot be null");
        String ext = getFileExtension(file);
        if (ext == null || ext.isEmpty())
            throw new IOException("The file \"" + file.getPath() + "\" has no exetnsion");

        Runtime rt = Runtime.getRuntime();
        String assoc_cmd = String.valueOf("assoc \"." + ext + '"');
        System.out.println(assoc_cmd);
        String assoc_result = command(rt, assoc_cmd);
        System.out.println(assoc_result);
        if (assoc_result == null)
            return null;
        String file_type = assoc_result.split("=")[1];
        final String ftype_cmd = "ftype " + file_type;
        String ftype_result = command(rt, ftype_cmd);
        System.out.println(ftype_result);
        if (ftype_result == null)
            return null;
        String app = ftype_result.split("=")[1]; //error prone, consider using regex
        return app;

    }

    public static String getFileExtension(File file) {
        final String filename = file.getName();
        return Optional.ofNullable(filename).filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1)).orElse(null);
    }

    public static void main(String[] args) throws IOException {
        String value;
        value = getDefaultProgram(new File("C:\\TEMP\\gimmicks.xml"));
        System.out.println(value);
//        value = getDefaultProgram(new File("C:\\TEMP\\gimmicks.mscz"));
//        System.out.println(value);
//        final File file = new File("C:\\TEMP\\gimmicks.mscz");
//        final String params = "-PP";
//        openWithParam(params, "C:\\Program\\test %0 %2", file);
//        openWithParam(params, "C:\\Program\\test %0 %3", file);
//        openWithParam(params, "C:\\Program\\test \"%*\" \"%0\"", file);
//        openWithParam(params, "C:\\Program\\test \"%0\" \"%2\"", file);
//        openWithParam(params, "C:\\Program\\test \"%1\"", file);
//        openWithParam(" ", new File("\"C:\\TEMP\\vendome.pdf\""));
    }
}
