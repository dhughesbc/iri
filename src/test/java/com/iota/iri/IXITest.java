package com.iota.iri;

import com.iota.iri.conf.Configuration;
import com.iota.iri.service.dto.AbstractResponse;
import com.iota.iri.service.dto.ErrorResponse;
import com.iota.iri.service.dto.IXIResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.*;

/**
 * Created by paul on 1/4/17.
 */
public class IXITest {
    static TemporaryFolder ixiDir = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        ixiDir.create();
        Configuration.put(Configuration.DefaultConfSettings.IXI_DIR, ixiDir.getRoot().getAbsolutePath());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ixiDir.delete();
    }

    @Test
    public void init() throws Exception {
        AbstractResponse response;
        IXIResponse ixiResponse;
        IXI.instance().init();

        final String testJs =
        "var Callable = Java.type(\"com.iota.iri.service.CallableRequest\");\n" +
        "var IXIResponse = Java.type(\"com.iota.iri.service.dto.IXIResponse\");\n" +
        "API.put(\"getParser\", new Callable({\n" +
                "call: function(req) {\n" +
                    "var IntArray = Java.type(\"int[]\");\n" +
                    "var out = new IntArray(Math.floor(Math.random()*9)+1);\n" +
                    "out[0] = 2;\n" +
                    "var r = IXIResponse.create({\n" +
                            "myArray: out,\n" +
                            "name: \"Foo\"\n" +
                    "});\n" +
                    "return r;\n" +
                "}\n" +
        "}));";


        final File testFile = ixiDir.newFile("test.js");
        testFile.createNewFile();
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(testFile.toPath(), CREATE))) {
            out.write(testJs.getBytes());
        }
        // Allow IXI to load the file
        Map<String, Object> request = new HashMap<>();
        Thread.sleep(1000);
        response = IXI.processCommand("test.getParser", request);

        assertFalse(response instanceof ErrorResponse);
        assertTrue(response instanceof IXIResponse);

        ixiResponse = ((IXIResponse) response);
        assertNotNull(ixiResponse.getResponse());

        testFile.delete();

        IXI.shutdown();
    }

    @Test
    public void processCommand() throws Exception {

    }

}
