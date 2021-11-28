/*
 * Copyright (c) 2021 AuroraMC Ltd. All Rights Reserved.
 */

package net.md_5.bungee.log;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jline.console.ConsoleReader;
import net.auroramc.missioncontrol.backend.util.DiscordWebhook;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class MissionControlLogger extends Logger
{

    private final LogDispatcher dispatcher = new LogDispatcher( this );

    // CHECKSTYLE:OFF
    @SuppressWarnings(
            {
                "CallToPrintStackTrace", "CallToThreadStartDuringObjectConstruction"
            })
    // CHECKSTYLE:ON
    @SuppressFBWarnings("SC_START_IN_CTOR")
    public MissionControlLogger(String loggerName, String filePattern, ConsoleReader reader)
    {
        super( loggerName, null );
        setLevel( Level.INFO );

        try
        {
            FileHandler fileHandler = new FileHandler( filePattern, 1 << 24, 8, true );
            fileHandler.setLevel( Level.parse( System.getProperty( "net.md_5.bungee.file-log-level", "INFO" ) ) );
            fileHandler.setFormatter( new ConciseFormatter( false ) );
            addHandler( fileHandler );

            ColouredWriter consoleHandler = new ColouredWriter( reader );
            consoleHandler.setLevel( Level.parse( System.getProperty( "net.md_5.bungee.console-log-level", "INFO" ) ) );
            consoleHandler.setFormatter( new ConciseFormatter( true ) );
            addHandler( consoleHandler );
        } catch ( IOException ex )
        {
            System.err.println( "Could not register logger!" );
            ex.printStackTrace();
        }

        dispatcher.start();
    }

    @Override
    public void log(LogRecord record)
    {
        DiscordWebhook webhook = new DiscordWebhook("https://discord.com/api/webhooks/914598634094485514/pMAeCxyzWwCHlDNhmr_hSvGGGjuQgRNgUPJW-Jq_7jVFf4NEcYykrHq7v_kxrPl5XxUs");
        webhook.setContent("**[" + record.getLevel().getName() + "]** " + record.getMessage() + ((record.getThrown() != null)?"\n" +
                ExceptionUtils.getStackTrace(record.getThrown()) :""));
        try {
            webhook.execute();
        } catch (Exception ignored) {
        }
        dispatcher.queue( record );

    }

    void doLog(LogRecord record)
    {
        super.log( record );
    }
}