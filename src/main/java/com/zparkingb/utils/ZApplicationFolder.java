/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zparkingb.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Différents utilitaires pour retrouver ou extraire des fichiers livrés avec l'application
 *
 * @author Laurent
 */
public final class ZApplicationFolder {

    private static Logger logger = Logger.getLogger(ZApplicationFolder.class.getName());
    private static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * Retrouve le répertoire d'où tourne l'application
     *
     * @param clazz Class
     *
     * @return File
     *
     * @throws IllegalArgumentException Si on ne parvient pas à trouver le
     *                                  fichier .class de la classe passée en entrée.
     * @deprecated use getApplicationPath instead
     */
    @Deprecated
    public static File getApplicationFolder(Class<?> clazz) throws IllegalArgumentException {
        Path path = getApplicationPath(clazz);
        return (path != null) ? path.toFile() : null;
    }

    /**
     * Retrouve le répertoire où tourne l'application ou un sous-élément dans ce répertoire.
     *
     * @param clazz Class
     *
     * @return Path
     *
     * @throws IllegalArgumentException Si on ne parvient pas à trouver le
     *                                  fichier .class de la classe passée en entrée.
     */
    public static Path getApplicationPath(Class<?> clazz, String... subpath) throws IllegalArgumentException {
        Path file = null;
        try {
            String resName = "/" + clazz.getName().replace('.', '/') + ".class";
            final URL url = clazz.getResource(resName);
            final String protocol = url.getProtocol();

            String path = URLDecoder.decode(url.toString(), "UTF-8");
            logger.fine("getClassRootDirectory=" + path);
            logger.fine("Running on " + getOS());

            int jarIdx = isWindows() ? 9 : 8;
            int fileIdx = isWindows() ? 5 : 4;

            // Cas 1): dans un jar.
            if (protocol.equals("jar")) {
                logger.fine("Resource in jar file.");
                // suppression de jar:file: de l'url d'un jar
                // ainsi que du path de la classe dans le jar
                int index = path.indexOf("!");
                if (index >= jarIdx) {
                    path = path.substring(jarIdx + 1, index);
                    logger.fine("Jar path=" + path);
                    file = Paths.get(path).getParent();
                    logger.fine("Application path=" + file);
                }
            }

            // Cas 2): dans un fichier
            if (protocol.equals("file")) {
                logger.fine("Resource in physical file.");
                // suppresion du file: de l'url si c'est une classe en dehors d'un jar
                // et suppression du path du package si il est prï¿½sent.
                int index = path.lastIndexOf(resName);
                if (index >= fileIdx) {
                    path = path.substring(fileIdx + 1, index); // 6 avec Path, 5 avec des File
                    file = Paths.get(path);
//                    file=file.getParent();
                    logger.fine("Application path=" + file);
                }
            }

            if (file == null) {
                // Tout autre format ne convient pas
                throw new IllegalArgumentException("Cannot find application path. Invalid path:  " + path);
            }

        } catch (UnsupportedEncodingException
                 | IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot find application path", e);
        }

        for (String string : subpath) {
            if (subpath != null) {
                file = file.resolve(string);
            }
        }

        return file;

    }

    /**
     * Retourne un stream dans le path de l'application
     *
     * @param clazz
     * @param path
     *
     * @return
     *
     * @throws IllegalArgumentException
     */
    public static InputStream getApplicationRessource(Class clazz, String path) throws IllegalArgumentException {
        return clazz.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
    }

    /**
     * Retourne un fichier dans le path de l'application
     *
     * @param clazz
     * @param path
     *
     * @return
     *
     * @deprecated use getApplicationPath instead
     */
    @Deprecated
    public static File getApplicationFile(Class clazz, String subpath) throws IllegalArgumentException {
        Path path = getApplicationPath(clazz, subpath);
        return (path != null) ? path.toFile() : null;
    }

    /**
     * Extrait une ressource (du jar typiquement) vers un fichier
     *
     * @param source String
     * @param dest   Path
     *
     * @throws FileNotFoundException si le fichier n'est pas créable ou si la
     *                               ressoucre n'existe pas
     * @throws IOException           si une erreur survient pendant la lecture et
     *                               l'écriture
     * @deprecated Use extractFileFromRessource
     */
    @Deprecated
    public static void extractFromRessourceToFile(Class clazz, String source, Path dest) throws FileNotFoundException, IOException {
        extractFileFromRessource(clazz, source, dest);
    }

    /**
     * Extrait une ressource (du jar typiquement) vers un fichier
     *
     * @param clazz  the value of clazz
     * @param source String
     * @param dest   Path
     * @param par3   the value of par3
     *
     * @throws FileNotFoundException si le fichier n'est pas créable ou si la
     *                               ressoucre n'existe pas
     * @throws IOException           si une erreur survient pendant la lecture et
     *                               l'écriture
     */
    public static void extractFileFromRessource(Class clazz, String source, Path dest) throws FileNotFoundException, IOException {
        if (source == null)
            throw new NullPointerException("The source resource path cannot be null");
        if (dest == null)
            throw new NullPointerException("The destination file cannot be null");
        InputStream ressource = getApplicationRessource(clazz, source);
        if (ressource == null) {
            throw new FileNotFoundException("Unable to locate the source ressource");
        }
        Path destFolder = dest.getParent();
        if (!Files.exists(destFolder))
            Files.createDirectories(destFolder);
        try (final BufferedInputStream in = new BufferedInputStream(ressource); final OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int n = 0;
            while ((n = in.read(buf, 0, buf.length)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    /**
     * Extrait un folder et de ses sous-éléments (du jar typiquement) vers un folder donné.
     *
     *
     * @param clazz  the value of clazz
     * @param source Path : le folder dans l'archive dont le contenu doit être copié
     * @param into   Path : la destination dans laquelle le contenu doit être copié
     *
     * @throws FileNotFoundException Si un fichier jar ne peut pas être identifié d'où extraire le
     *                               répertoire demandé
     * @throws IOException           si une erreur survient pendant la lecture et
     *                               l'écriture
     */
    public static void extractFolderFromRessource(Class clazz, Path source, Path into) throws FileNotFoundException, IOException {
        if (into == null)
            throw new NullPointerException("The destination folder cannot be null");

        String resName = "/" + clazz.getName().replace('.', '/') + ".class";
        final URL url = clazz.getResource(resName);
        final String protocol = url.getProtocol();

        String path = URLDecoder.decode(url.toString(), "UTF-8");
        logger.fine("getClassRootDirectory=" + path);

        // Cas 1): dans un jar.
        if (!protocol.equals("jar")) {
            throw new FileNotFoundException("Unsupported source file. Expected jar. Found " + protocol);
        }

        int index = path.indexOf("!");
        if (index < 9) {
            throw new FileNotFoundException("Cannot find jar file name in " + path);
        }

        path = path.substring(10, index);
        logger.fine("Jar path=" + path);

        JarFile jar = new JarFile(URLDecoder.decode(path, "UTF-8"));
        Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            Path from = Paths.get(entry.getName());

            if (source != null) {
                if (!from.startsWith(source))
                    continue;
                else
                    from = source.relativize(from);
            }

            Path dest = into.resolve(from);

            if (entry.isDirectory()) {
                // On crée le répertoire
                Files.createDirectories(dest);
            }
            else if (entry.getSize() > 0) { // je ne copie pas les fichiers vides
                // -- log
                StringBuilder sb = new StringBuilder("Extract ");
                if (source != null)
                    sb.append("(").append(source).append("\\)");
                sb.append(from).append(" -> ").append(dest);
//                logger.trace(sb.toString());
                // --log

                // On crée le répertoire *et* on copie le contenu du fichier
                Files.createDirectories(dest.getParent());

                // On lit sous forme de buffer
                java.io.InputStream in = new BufferedInputStream(jar.getInputStream(entry));
                OutputStream out = new BufferedOutputStream(Files.newOutputStream(dest));

                byte[] buffer = new byte[2048];
                for (;;) {
                    int nBytes = in.read(buffer);
                    if (nBytes <= 0) {
                        break;
                    }
                    out.write(buffer, 0, nBytes);
                }
                out.flush();
                out.close();
                in.close();

            }
        }

        jar.close();
    }

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS.contains("sunos");
    }

    public static String getOS() {
        if (isWindows()) {
            return "win";
        }
        else if (isMac()) {
            return "osx";
        }
        else if (isUnix()) {
            return "uni";
        }
        else if (isSolaris()) {
            return "sol";
        }
        else {
            return "err";
        }
    }

}
