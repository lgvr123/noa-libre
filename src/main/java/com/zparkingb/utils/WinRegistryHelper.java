/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zparkingb.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Laurent van Roy based on work by Oleg Ryaboy and Miguel Enriquez
 * @source https://stackoverflow.com/a/1982033/2398993
 */
public class WinRegistryHelper {

    /**
     *
     * @param location path in the registry
     * @param key      registry key
     *
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key) {
        String value = readRegistry(location, key, true);
        if (value != null)
            return value;
        return readRegistry(location, key, false);
    }

    /**
     *
     * @param location path in the registry
     * @param key      registry key
     * @param is32     look in Win32 registry or in Win65 registry
     *
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key, boolean is32) {
        try {
            final StringBuilder query = new StringBuilder("reg query ");
            query.append('"').append(location).append('"');
            if (key != null) {
                query.append(" /v ").append('"').append(key).append('"');
            }
            else {
                query.append(" /ve ");
            }
            if (is32) {
                query.append(" /reg:32 ");
            }
            else {
                query.append(" /reg:64 ");
            }
            System.out.println(query);
            Process process = Runtime.getRuntime().exec(query.toString());
            InputStream is = process.getInputStream();
            StringBuilder sw = new StringBuilder();
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.append((char) c);
            } catch (IOException e) {
            }
            String output = sw.toString();
            // Output has the following format:
            // \n<Version information>\n\n<key>    <registry type>    <value>\r\n\r\n
            int i = output.indexOf("REG_SZ");
            if (i == -1) {
                return null;
            }
            sw = new StringBuilder();
            i += 6; // skip REG_SZ
            // skip spaces or tabs
            for (;;) {
                if (i > output.length())
                    break;
                char c = output.charAt(i);
                if (c != ' ' && c != '\t')
                    break;
                ++i;
            }
            // take everything until end of line
            for (;;) {
                if (i > output.length())
                    break;
                char c = output.charAt(i);
                if (c == '\r' || c == '\n')
                    break;
                sw.append(c);
                ++i;
            }
            return sw.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {

        // Sample usage
        String value;
//        value = WinRegistryHelper.readRegistry("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\"
//                + "Explorer\\Shell Folders", "Personal");
//        System.out.println(value);
//        value = WinRegistryHelper.readRegistry("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\"
//                + "Explorer\\Shell Folders",null);
//        System.out.println(value);
        value = WinRegistryHelper.readRegistry("HKEY_CURRENT_USER\\Software\\Classes\\MuseScore.mscz\\shell\\open\\command",null);
        System.out.println(value);
    }
}
