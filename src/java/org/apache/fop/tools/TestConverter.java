/*
 * $Id: TestConverter.java,v 1.23 2003/03/07 10:09:30 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package org.apache.fop.tools;

import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOFileHandler;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.InputHandler;
import org.apache.fop.apps.XSLTInputHandler;
import org.apache.fop.apps.FOUserAgent;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TestConverter is used to process a set of tests specified in
 * a testsuite.
 * This class retrieves the data in the testsuite and uses FOP
 * to convert the xml and xsl file into either an xml representation
 * of the area tree or a pdf document.
 * The area tree can be used for automatic comparisons between different
 * versions of FOP or the pdf can be view for manual checking and
 * pdf rendering.
 *
 * Modified by Mark Lillywhite mark-fop@inomial.com to use the new Driver
 * interface.
 */
public class TestConverter extends AbstractLogEnabled {
    
    private boolean failOnly = false;
    private boolean outputPDF = false;
    private File destdir;
    private File compare = null;
    private String baseDir = "./";
    private Map differ = new java.util.HashMap();

    /**
     * This main method can be used to run the test converter from
     * the command line.
     * This will take a specified testsuite xml and process all
     * tests in it.
     * The command line options are:
     * -b to set the base directory for where the testsuite and associated files are
     * -failOnly to process only the tests which are specified as fail in the test results
     * -pdf to output the result as pdf
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("test suite file name required");
        }
        TestConverter tc = new TestConverter();
        tc.enableLogging(new ConsoleLogger(ConsoleLogger.LEVEL_ERROR));

        String testFile = null;
        for (int count = 0; count < args.length; count++) {
            if (args[count].equals("-failOnly")) {
                tc.setFailOnly(true);
            } else if (args[count].equals("-pdf")) {
                tc.setOutputPDF(true);
            } else if (args[count].equals("-b")) {
                tc.setBaseDir(args[count + 1]);
            } else {
                testFile = args[count];
            }
        }
        if (testFile == null) {
            System.out.println("test suite file name required");
        }

        tc.runTests(testFile, "results", null);
    }

    /**
     * Controls whether to generate PDF or XML.
     * @param pdf If True, PDF is generated, Area Tree XML otherwise.
     */
    public void setOutputPDF(boolean pdf) {
        outputPDF = pdf;
    }

    /**
     * Controls whether to process only the tests which are specified as fail 
     * in the test results.
     * @param fail True if only fail tests should be processed
     */
    public void setFailOnly(boolean fail) {
        failOnly = fail;
    }

    /**
     * Sets the base directory.
     * @param str base directory
     */
    public void setBaseDir(String str) {
        baseDir = str;
    }

    /**
     * Run the Tests.
     * This runs the tests specified in the xml file fname.
     * The document is read as a dom and each testcase is covered.
     * @param fname filename of the input file
     * @param dest destination directory
     * @param compDir comparison directory
     * @return Map a Map containing differences
     */
    public Map runTests(String fname, String dest, String compDir) {
        getLogger().debug("running tests in file:" + fname);
        try {
            if (compDir != null) {
                compare = new File(baseDir + "/" + compDir);
            }
            destdir = new File(baseDir + "/" + dest);
            destdir.mkdirs();
            File f = new File(baseDir + "/" + fname);
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(f);

            NodeList suitelist = doc.getChildNodes();
            if (suitelist.getLength() == 0) {
                return differ;
            }

            Node testsuite = null;
            testsuite = doc.getDocumentElement();

            if (testsuite.hasAttributes()) {
                String profile =
                    testsuite.getAttributes().getNamedItem("profile").getNodeValue();
                getLogger().debug("testing test suite:" + profile);
            }

            NodeList testcases = testsuite.getChildNodes();
            for (int count = 0; count < testcases.getLength(); count++) {
                Node testcase = testcases.item(count);
                if (testcase.getNodeName().equals("testcases")) {
                    runTestCase(testcase);
                }
            }
        } catch (Exception e) {
            getLogger().error("Error while running tests", e);
        }
        return differ;
    }

    /**
     * Run a test case.
     * This goes through a test case in the document.
     * A testcase can contain a test, a result or more test cases.
     * A test case is handled recursively otherwise the test is run.
     * @param tcase Test case node to run
     */
    protected void runTestCase(Node tcase) {
        if (tcase.hasAttributes()) {
            String profile =
                tcase.getAttributes().getNamedItem("profile").getNodeValue();
            getLogger().debug("testing profile:" + profile);
        }

        NodeList cases = tcase.getChildNodes();
        for (int count = 0; count < cases.getLength(); count++) {
            Node node = cases.item(count);
            String nodename = node.getNodeName();
            if (nodename.equals("testcases")) {
                runTestCase(node);
            } else if (nodename.equals("test")) {
                runTest(tcase, node);
            } else if (nodename.equals("result")) {
                //nop
            }

        }

    }

    /**
     * Run a particular test.
     * This runs a test defined by the xml and xsl documents.
     * If the test has a result specified it is checked.
     * This creates an XSLTInputHandler to provide the input
     * for FOP and writes the data out to an XML are tree.
     * @param testcase Test case to run
     * @param test Test
     */
    protected void runTest(Node testcase, Node test) {
        String id = test.getAttributes().getNamedItem("id").getNodeValue();
        Node result = locateResult(testcase, id);
        boolean pass = false;
        if (result != null) {
            String agreement =
                result.getAttributes().getNamedItem("agreement").getNodeValue();
            pass = agreement.equals("full");
        }

        if (pass && failOnly) {
            return;
        }

        String xml = test.getAttributes().getNamedItem("xml").getNodeValue();
        Node xslNode = test.getAttributes().getNamedItem("xsl");
        String xsl = null;
        if (xslNode != null) {
            xsl = xslNode.getNodeValue();
        }
        getLogger().debug("converting xml:" + xml + " and xsl:" 
                  + xsl + " to area tree");

        try {
            File xmlFile = new File(baseDir + "/" + xml);
            String baseURL = null;
            try {
                baseURL = xmlFile.getParentFile().toURL().toExternalForm();
            } catch (Exception e) {
                getLogger().error("Error setting base directory");
            }

            InputHandler inputHandler = null;
            if (xsl == null) {
                inputHandler = new FOFileHandler(xmlFile);
            } else {
                inputHandler = new XSLTInputHandler(xmlFile,
                                                    new File(baseDir + "/"
                                                             + xsl));
            }

            Driver driver = new Driver();
            setupLogger(driver, "fop");
            FOUserAgent userAgent = new FOUserAgent();
            userAgent.setBaseURL(baseURL);
            driver.setUserAgent(userAgent);
            if (outputPDF) {
                driver.setRenderer(Driver.RENDER_PDF);
            } else {
                driver.setRenderer(Driver.RENDER_XML);
            }

            Map rendererOptions = new java.util.HashMap();
            rendererOptions.put("fineDetail", new Boolean(false));
            rendererOptions.put("consistentOutput", new Boolean(true));
            driver.getRenderer().setOptions(rendererOptions);
            driver.getRenderer().setProducer("Testsuite Converter");

            String outname = xmlFile.getName();
            if (outname.endsWith(".xml")) {
                outname = outname.substring(0, outname.length() - 4);
            }
            driver.setOutputStream(new java.io.BufferedOutputStream(
                                       new java.io.FileOutputStream(new File(destdir,
                                       outname + (outputPDF ? ".pdf" : ".at.xml")))));
            getLogger().debug("ddir:" + destdir + " on:" + outname + ".pdf");
            driver.render(inputHandler);

            // check difference
            if (compare != null) {
                File f1 = new File(destdir, outname + ".at.xml");
                File f2 = new File(compare, outname + ".at.xml");
                if (!compareFiles(f1, f2)) {
                    differ.put(outname + ".at.xml", new Boolean(pass));
                }
            }
        } catch (Exception e) {
            getLogger().error("Error while running tests", e);
        }
    }

    /**
     * Compare files.
     * @param f1 first file
     * @param f2 second file
     * @return true if equal
     */
    protected boolean compareFiles(File f1, File f2) {
        if (f1.length() != f2.length()) {
            return false;
        }
        try {
            InputStream is1 = new java.io.BufferedInputStream(new java.io.FileInputStream(f1));
            InputStream is2 = new java.io.BufferedInputStream(new java.io.FileInputStream(f2));
            while (true) {
                int ch1 = is1.read();
                int ch2 = is2.read();
                if (ch1 == ch2) {
                    if (ch1 == -1) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            getLogger().error("Error while comparing files", e);
        }

        return false;
    }

    private Node locateResult(Node testcase, String id) {
        NodeList cases = testcase.getChildNodes();
        for (int count = 0; count < cases.getLength(); count++) {
            Node node = cases.item(count);
            String nodename = node.getNodeName();
            if (nodename.equals("result")) {
                String resultid =
                    node.getAttributes().getNamedItem("id").getNodeValue();
                if (id.equals(resultid)) {
                    return node;
                }
            }
        }
        return null;
    }

}
