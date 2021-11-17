package io.mosip.registration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;

public class DerbySlf4jBridge {


    public static final Writer loggingWriter = new LoggingWriter();
    private static final Logger logger = LoggerFactory.getLogger(DerbySlf4jBridge.class);

    private DerbySlf4jBridge()
    {
    }

    /**
     * A basic adapter that funnels Derby's logs through an SLF4J logger.
     */
    public static final class LoggingWriter extends Writer
    {
        @Override
        public void write(final char[] cbuf, final int off, final int len)
        {
            // Don't bother with empty lines.
            if (len > 1)
            {
                logger.info(new String(cbuf, off, len));
            }
        }

        @Override
        public void flush()
        {
            // noop.
        }

        @Override
        public void close()
        {
            // noop.
        }
    }

    public static Writer bridge()
    {
        return loggingWriter;
    }
}
