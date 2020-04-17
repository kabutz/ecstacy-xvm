#pragma once

#define DEFAULT_EXEC "java"
#define DEFAULT_OPTS "-Xms256m -Xmx1024m -ea"
#define PROTO_JAR    "xvm.jar"
#define PROTO_LIB    "xvm.xtc"

#ifdef windowsLauncher
#define FILE_SEPERATOR '\\'
#define PROTO_DIR      "..\\prototype\\"
#define LIB_DIR        "..\\lib\\"
#else
#define FILE_SEPERATOR '/'
#define PROTO_DIR      "../prototype/"
#define LIB_DIR        "../lib/"
#endif

/**
 * Determine the path of this executable.
 *
 * @return the path of the launcher
 */
extern const char* findLauncherPath();

/**
 * Execute the JVM against the specified JAR.
 *
 * @param javaPath  the path to use to execute the JVM (e.g. "java")
 * @param javaOpts  the JVM options (e.g. "-Xmx=512m")
 * @param jarPath   the directory path containing the xvm.jar to execute and xvm.xtc
 * @param libPath   the directory path containing Ecstasy.xtc and other modules
 * @param argc      the number of arguments to pass along
 * @param argv      the arguments to pass along
 */
extern void execJava(const char* javaPath,
                     const char* javaOpts,
                     const char* jarPath,
                     const char* libPath,
                     int         argc,
                     const char* argv[]);