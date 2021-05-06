/*
 * This file is part of noa-libre.
 *
 * The Contents of this file are made available subject to
 * the terms of GNU Lesser General Public License Version 2.1.
 *
 * GNU Lesser General Public License Version 2.1
 * ========================================================================
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Publi
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston
 * MA  02111-1307  USA
 *
 * This file incorporates work covered by the following license notice:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements. See the NOTICE file distributed
 *   with this work for additional information regarding copyright
 *   ownership. The ASF licenses this file to you under the Apache
 *   License, Version 2.0 (the "License"); you may not use this file
 *   except in compliance with the License. You may obtain a copy of
 *   the License at http://www.apache.org/licenses/LICENSE-2.0 .
 */

package ag.ion.bion.officelayer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * This class finds a UNO installation on the system.
 *
 * <p>A UNO installation can be specified by the user by either setting the
 * com.sun.star.lib.loader.unopath system property or by setting the
 * UNO_PATH environment variable to the program directory of a UNO
 * installation.
 * Note, that Java 1.3.1 and Java 1.4 don't support environment variables
 * (System.getenv() throws java.lang.Error) and therefore setting the UNO_PATH
 * environment variable won't work with those Java versions.
 * If no UNO installation is specified by the user, the default installation
 * on the system will be returned.</p>
 *
 * <p>On the Windows platform the default installation is read from the Windows
 * Registry.</p>
 *
 * <p>On the Unix/Linux platforms the default installation is found from the
 * PATH environment variable. Note, that for Java 1.3.1 and Java 1.4 the
 * default installation is found by using the 'which' command, because
 * environment variables are not supported with those Java versions.
 * Both methods require that the 'soffice' executable or a symbolic
 * link is in one of the directories listed in the PATH environment variable.
 * For older versions than OOo 2.0 the above described methods may fail.
 * In this case the default installation is taken from the .sversionrc file in
 * the user's home directory. Note, that the .sversionrc file will be omitted
 * for OOo 2.0</p>
 */
//FIXME: split this class into InstallationFinderUnix and InstallationFinderWindows
final class InstallationFinder {

    private static final String SYSPROP_NAME =
        "com.sun.star.lib.loader.unopath";
    private static final String ENVVAR_NAME = "UNO_PATH";
    private static final String SOFFICE = "libreoffice"; // Unix/Linux only

    private InstallationFinder() {} // do not instantiate

    /**
     * Gets the path of a UNO installation.
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was specified or found, or if an error occurred
     */
    public static String getPath() {

        String path = null;

        // get the installation path from the Java system property
        // com.sun.star.lib.loader.unopath
        // (all platforms)
        path = getPathFromProperty( SYSPROP_NAME );
        if ( path != null ) {
            return path;
        }
        // get the installation path from the UNO_PATH environment variable
        // (all platforms, not working for Java 1.3.1 and Java 1.4)
        path = getPathFromEnvVar( ENVVAR_NAME );
        if ( path != null ) {
            return path;
        }

        String osname = null;
        try {
            osname = System.getProperty( "os.name" );
        } catch ( SecurityException e ) {
            // if a SecurityException was thrown,
            // return <code>null</code>
            System.err.println( InstallationFinder.class.getName() +
                    "::getPath: cannot get system property " +
		    "os.name" + e );
            return null;
        }
        if ( osname == null ) {
            return null;
        }

        if ( osname.startsWith( "Windows" ) ) {
            // get the installation path from the Windows Registry
            // (Windows platform only)
	    // FIXME: this doesn't work currently as it needs unowinreg.dll
	    // (part of Libreoffice SDK) which we don't have
            path = getPathFromWindowsRegistry();
        } else {
            // get the installation path from the PATH environment
            // variable (Unix/Linux platforms only, not working for
            // Java 1.3.1 and Java 1.4)
            path = getPathFromPathEnvVar();
            if ( path == null ) {
                // get the installation path from the 'which'
                // command (Unix/Linux platforms only)
                path = getPathFromWhich();
                if ( path == null ) {
                    // get the installation path from the
                    // .sversionrc file (Unix/Linux platforms only,
                    // for older versions than OOo 2.0)
                    path = getPathFromSVersionFile();
                }
            }
        }

        return path;
    }

    /**
     * Gets the installation path from a Java system property.
     *
     * <p>This method is called on all platforms.
     * The Java system property can be passed into the application by using
     * the -D flag, e.g.
     * java -D<property name>=<installation path> -jar application.jar.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was specified in the Java system property or if an error occurred
     */
    private static String getPathFromProperty( String prop ) {

        String path = null;

        try {
            path = System.getProperty( prop );
        } catch ( SecurityException e ) {
            // if a SecurityException was thrown, return <code>null</code>
            System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromProperty: cannot get system property " +
		    prop + ": " + e );
        }

        return path;
    }

    /**
     * Gets the installation path from an environment variable.
     *
     * <p>This method is called on all platforms.
     * Note, that in Java 1.3.1 and Java 1.4 System.getenv() throws
     * java.lang.Error and therefore this method returns null for those
     * Java versions.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was specified in the environment variable or if an error occurred
     */
    private static String getPathFromEnvVar( String var ) {

        String path = null;

        try {
            path = System.getenv( var );
        } catch ( SecurityException e ) {
            // if a SecurityException was thrown, return <code>null</code>
            System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromEnvVar: cannot get environment variable " +
		    var + ": " + e );
        } catch ( java.lang.Error err ) {
            // System.getenv() throws java.lang.Error in Java 1.3.1 and
            // Java 1.4
            System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromEnvVar: getting environment variables "
		    + "not supported " + err );
        }

        return path;
    }

    /**
     * Gets the installation path from the Windows Registry.
     *
     * <p>This method is called on the Windows platform only.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was found or if an error occurred
     */
    private static String getPathFromWindowsRegistry() {

        final String SUBKEYNAME = "Software\\LibreOffice\\UNO\\InstallPath";

        String path = null;

        try {
            // read the key's default value from HKEY_CURRENT_USER
            WinRegKey key = new WinRegKey( "HKEY_CURRENT_USER", SUBKEYNAME );
            path = key.getStringValue( "" ); // default
        } catch ( WinRegKeyException e ) {
            try {
                // read the key's default value from HKEY_LOCAL_MACHINE
                WinRegKey key = new WinRegKey( "HKEY_LOCAL_MACHINE",
                                               SUBKEYNAME );
                path = key.getStringValue( "" ); // default
            } catch ( WinRegKeyException we ) {
                System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromWindowsRegistry: " +
                    "reading key from Windows Registry failed: " + we );
            }
        }

        return path;
    }

    /**
     * Gets the installation path from the PATH environment variable.
     *
     * <p>This method is called on Unix/Linux platforms only.
     * An installation is found, if the executable 'soffice' or a symbolic link
     * is in one of the directories listed in the PATH environment variable.
     * Note, that in Java 1.3.1 and Java 1.4 System.getenv() throws
     * java.lang.Error and therefore this method returns null for those
     * Java versions.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was found or if an error occurred
     */
    private static String getPathFromPathEnvVar() {

        final String PATH_ENVVAR_NAME = "PATH";

        String path = null;
        String str = null;

        try {
            str = System.getenv( PATH_ENVVAR_NAME );
        } catch ( SecurityException e ) {
            // if a SecurityException was thrown, return <code>null</code>
            System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromPathEnvVar: cannot get environment variable " +
		    PATH_ENVVAR_NAME + ": " + e );
            return null;
        } catch ( java.lang.Error err ) {
            // System.getenv() throws java.lang.Error in Java 1.3.1 and
            // Java 1.4
            System.err.println( InstallationFinder.class.getName() +
                    "::getPathFromPathEnvVar: getting environment variables "
		    + "not supported " + err );
            return null;
        }

        if ( str != null ) {
            StringTokenizer tokens = new StringTokenizer(
                str, File.pathSeparator );
            while ( tokens.hasMoreTokens() ) {
                File file = new File( tokens.nextToken(), SOFFICE );
                try {
                    if ( file.exists() ) {
                        try {
                            // resolve symlink
                            path = file.getCanonicalFile().getParent();
                            if ( path != null )
                                break;
                        } catch ( IOException e ) {
                            // if an I/O exception is thrown, ignore this
                            // path entry and try the next one
                            System.err.println( InstallationFinder.class.getName() +
                                "::getPathFromPathEnvVar: " +
                                "bad path: " + e );
                        }
                    }
                } catch ( SecurityException e ) {
                    // if a SecurityException was thrown, ignore this path
                    // entry and try the next one
                    System.err.println( InstallationFinder.class.getName() +
                        "::getPathFromPathEnvVar: " + 
			"security exception accessing path: "+ e );
                }
            }
        }

        return path;
    }

    /**
     * Gets the installation path from the 'which' command on Unix/Linux
     * platforms.
     *
     * <p>This method is called on Unix/Linux platforms only.
     * An installation is found, if the executable 'soffice' or a symbolic link
     * is in one of the directories listed in the PATH environment variable.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was found or if an error occurred
     */
    private static String getPathFromWhich() {

        final String WHICH = "which";

        String path = null;

        // start the which process
        String[] cmdArray = new String[] { WHICH, SOFFICE };
        Process proc = null;
        Runtime rt = Runtime.getRuntime();
        try {
            proc = rt.exec( cmdArray );
        } catch ( SecurityException e ) {
            System.err.println( InstallationFinder.class.getName() +
                "::getPathFromWhich: " +
                "security exception while executing which command: " + e );
            return null;
        } catch ( IOException e ) {
            // if an I/O exception is thrown, return <code>null</null>
            System.err.println( InstallationFinder.class.getName() +
                "::getPathFromWhich: " +
                "which command failed: " + e );
            return null;
        }

        // empty standard error stream in a separate thread
        StreamGobbler gobbler = new StreamGobbler( proc.getErrorStream() );
        gobbler.start();

        try {
            // read the which output from standard input stream
            BufferedReader br = new BufferedReader(
                new InputStreamReader( proc.getInputStream(), "UTF-8" ) );
            String line = null;
            try {
                while ( ( line = br.readLine() ) != null ) {
                    if ( path == null ) {
                        // get the path from the which output
                        int index = line.lastIndexOf( SOFFICE );
                        if ( index != -1 ) {
                            int end = index + SOFFICE.length();
                            for ( int i = 0; i <= index; i++ ) {
                                File file = new File( line.substring( i, end ) );
                                try {
                                    if ( file.exists() ) {
                                        // resolve symlink
                                        path = file.getCanonicalFile().getParent();
                                        if ( path != null )
                                            break;
                                    }
                                } catch ( SecurityException e ) {
                                    System.err.println( InstallationFinder.class.getName() +
                                        "::getPathFromWhich: " +
                                        "security exception accessing file: " + e );
                                    return null;
                                }
                            }
                        }
                    }
                }
            } catch ( IOException e ) {
                // if an I/O exception is thrown, return <code>null</null>
                System.err.println( InstallationFinder.class.getName() +
                                    "::getPathFromWhich: " +
                                    "reading which command output failed: " + e );
                return null;
            } finally {
                try {
                    br.close();
                } catch ( IOException e ) {
                    // closing standard input stream failed, ignore
                    System.err.println( InstallationFinder.class.getName() +
                                        "::getPathFromWhich: " +
                                        "reading which command output failed: " + e );
                }
            }
        } catch ( UnsupportedEncodingException e ) {
            // if an Encoding exception is thrown, return <code>null</null>
            System.err.println( InstallationFinder.class.getName() +
                                "::getPathFromWhich: " +
                                "encoding failed: " + e );
            return null;
        }

        try {
            // wait until the which process has terminated
            proc.waitFor();
        } catch ( InterruptedException e ) {
            // the current thread was interrupted by another thread,
            // kill the which process
            proc.destroy();
            // set the interrupted status
            Thread.currentThread().interrupt();
        }

        return path;
    }

    /**
     * Gets the installation path from the .sverionrc file in the user's home
     * directory.
     *
     * <p>This method is called on Unix/Linux platforms only.
     * The .sversionrc file is written during setup and will be omitted for
     * OOo 2.0.</p>
     *
     * @return the installation path or <code>null</code>, if no installation
     *         was found or if an error occurred
     */
    private static String getPathFromSVersionFile() {

        final String SVERSION = ".sversionrc"; // Unix/Linux only
        final String VERSIONS = "[Versions]";

        String path = null;

        try {
            File fSVersion = new File(
                System.getProperty( "user.home" ) ,SVERSION );
            if ( fSVersion.exists() ) {
                ArrayList<String> lines = new ArrayList<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader( new InputStreamReader(
                        new FileInputStream( fSVersion ), "UTF-8" ) );
                    String line = null;
                    while ( ( line = br.readLine() ) != null &&
                            !line.equals( VERSIONS ) ) {
                        // read lines until [Versions] is found
                    }
                    while ( ( line = br.readLine() ) != null &&
                            line.length() != 0 ) {
                        if ( !line.startsWith( ";" ) )
                            lines.add( line );
                    }
                } catch ( IOException e ) {
                    // if an I/O exception is thrown, try to analyze the lines
                    // read so far
                    System.err.println( InstallationFinder.class.getName() +
                        "::getPathFromSVersionFile: " +
                        "reading .sversionrc file failed: " + e );
                } finally {
                    if ( br != null ) {
                        try {
                            br.close();
                        } catch ( IOException e ) {
                            // closing .sversionrc failed, ignore
                        }
                    }
                }
                for ( int i = lines.size() - 1; i >= 0; i-- ) {
                    StringTokenizer tokens = new StringTokenizer(
                        lines.get( i ), "=" );
                    if ( tokens.countTokens() != 2 )
                        continue;
                    tokens.nextToken(); // key
                    String url = tokens.nextToken();
                    path = getCanonicalPathFromFileURL( url );
                    if ( path != null )
                        break;
                }
            }
        } catch ( SecurityException e ) {
            System.err.println( InstallationFinder.class.getName() +
                "::getPathFromSVersionFile: " +
                "security exception accessing file: " + e );
            return null;
        }

        return path;
    }

    /**
     * Translates an OOo-internal absolute file URL reference (encoded using
     * UTF-8) into a Java canonical pathname.
     *
     * @param oooUrl any URL reference; any fragment part is ignored
     *
     * @return if the given URL is a valid absolute, local (that is, the host
     * part is empty or equal to "localhost", ignoring case) file URL, it is
     * converted into an absolute canonical pathname; otherwise,
     * <code>null</code> is returned
     */
    private static String getCanonicalPathFromFileURL( String oooUrl ) {

        String prefix = "file://";
        if (oooUrl.length() < prefix.length()
            || !oooUrl.substring(0, prefix.length()).equalsIgnoreCase(
                prefix))
        {
            return null;
        }
        StringBuffer buf = new StringBuffer(prefix);
        int n = oooUrl.indexOf('/', prefix.length());
        if (n < 0) {
            n = oooUrl.length();
        }
        String host = oooUrl.substring(prefix.length(), n);
        if (host.length() != 0 && !host.equalsIgnoreCase("localhost")) {
            return null;
        }
        buf.append(host);
        if (n == oooUrl.length()) {
            buf.append('/');
        } else {
        loop:
            while (n < oooUrl.length()) {
                buf.append('/');
                ++n;
                int n2 = oooUrl.indexOf('/', n);
                if (n2 < 0) {
                    n2 = oooUrl.length();
                }
                while (n < n2) {
                    char c = oooUrl.charAt(n);
                    switch (c) {
                    case '%':
                        byte[] bytes = new byte[(n2 - n) / 3];
                        int len = 0;
                        while (oooUrl.length() - n > 2
                               && oooUrl.charAt(n) == '%')
                        {
                            int d1 = Character.digit(oooUrl.charAt(n + 1), 16);
                            int d2 = Character.digit(oooUrl.charAt(n + 2), 16);
                            if (d1 < 0 || d2 < 0) {
                                break;
                            }
                            int d = 16 * d1 + d2;
                            if (d == '/') {
                                return null;
                            }
                            bytes[len++] = (byte) d;
                            n += 3;
                        }
                        String s;
                        try {
                            s = new String(bytes, 0, len, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                             System.err.println( InstallationFinder.class.getName() +
                                                 "::getCanonicalPathFromFileURL: " +
                                                 "encoding failed: " + e );
                            return null;
                        }
                        buf.append(s);
                        break;

                    case '#':
                        break loop;

                    default:
                        buf.append(c);
                        ++n;
                        break;
                    }
                }
            }
        }
        URL url;
        try {
            url = new URL(buf.toString());
        } catch (MalformedURLException e) {
             System.err.println( InstallationFinder.class.getName() +
                                 "::getCanonicalPathFromFileURL: " +
                                 "invalid URL: " + e );
            return null;
        }
        String path = url.getFile();
        String fragment = url.getRef();
        if (fragment != null) {
            path += '#' + fragment;
        }
        String ret = null;
        File file = new File( path, SOFFICE );
        try {
            if ( file.isAbsolute() && file.exists() ) {
                try {
                    // resolve symlink
                    ret = file.getCanonicalFile().getParent();
                } catch ( IOException e ) {
                     System.err.println( InstallationFinder.class.getName() +
                                         "::getCanonicalPathFromFileURL: " +
                                         "error accessing file: " + e );
                    return null;
                }
            }
        } catch ( SecurityException e ) {
            System.err.println( InstallationFinder.class.getName() +
                "::getCanonicalPathFromFileURL: security exception accessing file: "+ e );
            return null;
        }

        return ret;
    }

    /**
       This class is used for emptying any stream which is passed into it in
       a separate thread.
     */
    private static final class StreamGobbler extends Thread {

        InputStream m_istream;

        StreamGobbler( InputStream istream ) {
            m_istream = istream;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader( m_istream, "UTF-8" ) );
                // read from input stream
                while ( br.readLine() != null ) {
                    // don't handle line content
                }
                br.close();
            } catch (UnsupportedEncodingException e) {
                // cannot read from input stream
                System.err.println( StreamGobbler.class.getName() +
                                    "::run: encoding failed: " + e );
            } catch ( IOException e ) {
                // stop reading from input stream
                System.err.println( StreamGobbler.class.getName() +
                                    "::run: read from input stream failed: " + e );
            }
        }
    }
}
