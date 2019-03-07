package com.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FopRotationTest {
    private static final Logger log = LoggerFactory.getLogger(FopRotationTest.class);

    private Path baseDirectory;
    private Path settingsFile;
    private Path foFile;
    private Path workaroundFoFile;
    private FopFactory fopFactory;

    @Before
    public void setUp() throws URISyntaxException, ConfigurationException, SAXException, IOException {
        URL url = FopRotationTest.class.getClassLoader().getResource("fop");
        assertThat(url).isNotNull();
        this.baseDirectory = Paths.get(url.toURI());
        this.settingsFile = baseDirectory.resolve("fop-settings.xml");
        this.foFile = baseDirectory.resolve("test.fo");
        this.workaroundFoFile = baseDirectory.resolve("workaround.fo");

        Configuration cfg = new DefaultConfigurationBuilder().buildFromFile(settingsFile.toFile());
        FopFactoryBuilder fopFactoryBuilder = new FopFactoryBuilder(baseDirectory.toUri()).setConfiguration(cfg);

        // create FOP factory
        this.fopFactory = fopFactoryBuilder.build();
    }

    @Test
    public void reproduce() throws IOException, FOPException, TransformerException {
        Path testPdf = Files.createTempFile("test_reproducer_", ".pdf");

        createPdf(this.foFile, testPdf);

        assertThat(testPdf).as("Output PDF file should exist!").exists();
    }

    @Test
    public void workaround() throws IOException, FOPException, TransformerException {
        Path testPdf = Files.createTempFile("test_workaround_", ".pdf");

        createPdf(this.workaroundFoFile, testPdf);

        assertThat(testPdf).as("Output PDF file should exist!").exists();
    }

    private void createPdf(Path xslFile, Path outputPdf) throws IOException, FOPException, TransformerException {
        log.info("FOP base directory: {}", this.baseDirectory);
        log.info("FOP settings file:  {}", this.settingsFile);
        log.info("FOP file:           {}", xslFile);
        log.info("PDF output file:    {}", outputPdf);

        try (OutputStream os = Files.newOutputStream(outputPdf)) {
            FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.setProducer("producer");
            userAgent.setCreator("creator");
            userAgent.setAuthor("author");
            userAgent.setCreationDate(new Date());
            userAgent.setTitle("Misplaced PDF graphic");

            // create FOP for PDF output generation
            Fop fop = this.fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, os);

            Source source = new StreamSource(xslFile.toFile());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(source);

            Result result = new SAXResult(fop.getDefaultHandler());

            transformer.transform(source, result);
        }
    }
}
