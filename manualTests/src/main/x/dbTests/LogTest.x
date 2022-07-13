/**
 * A stand-alone test for DBLog functionality.
 *
 * To run:
 *      gradle compileOne -PtestName=dbTests/LogDB
 *      gradle runOne -PtestName=dbTests/LogTest
 */
module LogTest
    {
    package oodb   import oodb.xtclang.org;
    package jsondb import jsondb.xtclang.org;

    package logDB import LogDB;

    import logDB.LogSchema;

    void run()
        {
        @Inject Directory homeDir;

        Directory dataDir  = homeDir.dirFor("Development/xvm/manualTests/data/logDB").ensure();
        Directory buildDir = homeDir.dirFor("Development/xvm/manualTests/build").ensure();

        reportLogFiles(dataDir, "*** Before");

        using (LogSchema schema = jsondb.createConnection("LogDB", dataDir, buildDir).as(LogSchema))
            {
            for (Int i : 1..10000)
                {
                schema.logger.add(
                    $"This is a message to test the log truncation policy: {schema.counter.next()}");
                }
            }

        reportLogFiles(dataDir, "*** After");
        }

    void reportLogFiles(Directory dataDir, String prefix)
        {
        @Inject Console console;

        console.println(prefix);
        Int size = 0;
        for (File file : dataDir.dirFor("logger").files())
            {
            console.println($"\t-{file.size} {file.name}");
            size += file.size;
            }
        console.println($"total size {size} bytes");
        }
    }