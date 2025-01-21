/*
 * Copyright 2020 SpotBugs plugin contributors
 *
 * This file is part of IntelliJ SpotBugs plugin.
 *
 * IntelliJ SpotBugs plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * IntelliJ SpotBugs plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ SpotBugs plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.spotbugs.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MergeMultipleResultsFiles {

    public static String mergeFiles(final String inputFile){

        if (inputFile == null || inputFile.trim().isEmpty()) {
            return null;
        }

        if (!Files.isDirectory(Path.of(inputFile))){
            return inputFile;
        }

        final String outputFile = Path.of(inputFile, "merged-spotbugs-report.xml").toString();
        // input file is directory - go over all the directories under this folder,
        // find spotbugs results file and merge them to one file
        try {
            final List<File> xmlFiles = new ArrayList<>();
            collectXmlFiles(new File(inputFile), xmlFiles);

            if (xmlFiles.isEmpty()) {
                System.out.println("No SpotBugs XML files found in the directory: " + inputFile);
                return null;
            }

            final Document mergedDocument = mergeSpotBugsReports(xmlFiles);

            saveDocumentToFile(mergedDocument, outputFile);
            System.out.println("Merged SpotBugs report written to: " + outputFile);
        } catch (Exception e) {
            System.err.println("Error while merging SpotBugs reports: " + e.getMessage());
            e.printStackTrace();
        }
        return outputFile;
    }

    /**
     * Recursively collects XML files from the given directory.
     */
    private static void collectXmlFiles(File directory, List<File> xmlFiles) {
        if (!directory.exists()) {
            System.err.println("Directory does not exist: " + directory.getAbsolutePath());
            return;
        }

        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectXmlFiles(file, xmlFiles);
                } else if (file.getName().equals("spotbugsXml.xml")) {
                    System.out.println("adding file "+ file);
                    xmlFiles.add(file);
                }
            }
        }
    }

    /**
     * Merges multiple SpotBugs XML files into a single Document.
     */
    private static Document mergeSpotBugsReports(List<File> xmlFiles)
        throws ParserConfigurationException, SAXException, IOException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document mergedDocument = builder.newDocument();

        // Create the root element for the merged document
        Element rootElement = mergedDocument.createElement("BugCollection");
        mergedDocument.appendChild(rootElement);

        for (File file : xmlFiles) {
            final Document document = builder.parse(file);
            final NodeList bugInstances = document.getElementsByTagName("BugInstance");

            for (int i = 0; i < bugInstances.getLength(); i++) {
                final Node importedNode = mergedDocument.importNode(bugInstances.item(i), true);
                rootElement.appendChild(importedNode);
            }
        }

        return mergedDocument;
    }

    /**
     * Saves the merged Document to an XML file.
     */
    private static void saveDocumentToFile(Document document, String outputFile) throws Exception {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        final DOMSource source = new DOMSource(document);
        final StreamResult result = new StreamResult(new FileWriter(outputFile));
        transformer.transform(source, result);
    }
}
