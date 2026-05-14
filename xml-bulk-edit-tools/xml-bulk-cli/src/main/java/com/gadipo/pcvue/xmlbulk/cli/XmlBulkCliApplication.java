package com.gadipo.pcvue.xmlbulk.cli;

import com.gadipo.pcvue.xmlbulk.core.ChangeRecord;
import com.gadipo.pcvue.xmlbulk.core.EditOperation;
import com.gadipo.pcvue.xmlbulk.core.EditRequest;
import com.gadipo.pcvue.xmlbulk.core.EditResult;
import com.gadipo.pcvue.xmlbulk.core.SearchCriteria;
import com.gadipo.pcvue.xmlbulk.core.XmlBulkEditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class XmlBulkCliApplication {

    private XmlBulkCliApplication() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || contains(args, "--help")) {
            printUsage();
            return;
        }

        CliArguments parsed = CliArguments.parse(args);
        String xml = Files.readString(parsed.inputPath());

        XmlBulkEditor editor = new XmlBulkEditor();
        EditResult result = editor.edit(xml, new EditRequest(
                new SearchCriteria(
                        parsed.tagName(),
                        parsed.criteriaAttributeName(),
                        parsed.criteriaAttributeValueContains(),
                        parsed.textContains(),
                        parsed.textRegex(),
                        parsed.xpathContains(),
                        parsed.ancestorTagName(),
                        parsed.ancestorTextContains()),
                parsed.operation(),
                parsed.replacement(),
                parsed.operationAttributeName(),
                parsed.operationAttributeValue(),
                parsed.nodeXml(),
                parsed.dryRun()
        ));

        String report = buildReport(result, parsed);
        System.out.println(report);

        if (parsed.reportPath() != null) {
            Files.writeString(parsed.reportPath(), report);
        }

        if (parsed.outputPath() != null) {
            Files.writeString(parsed.outputPath(), result.outputXml());
        } else {
            System.out.println(result.outputXml());
        }
    }

    private static String buildReport(EditResult result, CliArguments parsed) {
        StringBuilder report = new StringBuilder();
        report.append("XML Bulk Edit Report\n");
        report.append("Timestamp: ").append(Instant.now()).append('\n');
        report.append("Dry run: ").append(parsed.dryRun()).append('\n');
        report.append("Matched nodes: ").append(result.matchedNodeCount()).append('\n');
        report.append("Changes: ").append(result.changes().size()).append('\n');
        for (ChangeRecord change : result.changes()) {
            report.append("- ").append(change.changeType())
                    .append(" @ ").append(change.path())
                    .append(" | before=").append(change.beforeValue())
                    .append(" | after=").append(change.afterValue())
                    .append('\n');
        }
        return report.toString();
    }

    private static boolean contains(String[] args, String value) {
        for (String arg : args) {
            if (value.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java -jar xml-bulk-cli-*-jar-with-dependencies.jar --input <input.xml> [options]

                Search criteria options:
                  --tag <tagName>
                  --attr-name <attributeName>
                  --attr-value-contains <substring>
                  --text-contains <substring>
                  --text-regex <pattern>
                  --xpath-contains <xpath-like-substring>
                  --ancestor-tag <tagName>
                  --ancestor-text-contains <substring>

                Operations (choose one):
                  --replace-text <newText>
                  --replace-attr <attributeName>=<newValue>
                  --add-attr <attributeName>=<value>
                  --remove-attr <attributeName>
                  --add-node-xml <xmlFragment>
                  --remove-node

                Output options:
                  --output <output.xml>
                  --report <report.txt>
                  --dry-run
                """);
    }

    private record CliArguments(
            Path inputPath,
            Path outputPath,
            Path reportPath,
            String tagName,
            String criteriaAttributeName,
            String criteriaAttributeValueContains,
            String textContains,
            String textRegex,
            String xpathContains,
            String ancestorTagName,
            String ancestorTextContains,
            EditOperation operation,
            String replacement,
            String operationAttributeName,
            String operationAttributeValue,
            String nodeXml,
            boolean dryRun) {

        static CliArguments parse(String[] args) {
            Path input = null;
            Path output = null;
            Path report = null;
            String tagName = null;
            String criteriaAttributeName = null;
            String criteriaAttributeValueContains = null;
            String textContains = null;
            String textRegex = null;
            String xpathContains = null;
            String ancestorTagName = null;
            String ancestorTextContains = null;
            EditOperation operation = null;
            String replacement = null;
            String operationAttributeName = null;
            String operationAttributeValue = null;
            String nodeXml = null;
            boolean dryRun = false;

            List<String> tokens = new ArrayList<>(List.of(args));
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                switch (token) {
                    case "--input" -> input = Path.of(requireValue(tokens, ++i, token));
                    case "--output" -> output = Path.of(requireValue(tokens, ++i, token));
                    case "--report" -> report = Path.of(requireValue(tokens, ++i, token));
                    case "--tag" -> tagName = requireValue(tokens, ++i, token);
                    case "--attr-name" -> criteriaAttributeName = requireValue(tokens, ++i, token);
                    case "--attr-value-contains" -> criteriaAttributeValueContains = requireValue(tokens, ++i, token);
                    case "--text-contains" -> textContains = requireValue(tokens, ++i, token);
                    case "--text-regex" -> textRegex = requireValue(tokens, ++i, token);
                    case "--xpath-contains" -> xpathContains = requireValue(tokens, ++i, token);
                    case "--ancestor-tag" -> ancestorTagName = requireValue(tokens, ++i, token);
                    case "--ancestor-text-contains" -> ancestorTextContains = requireValue(tokens, ++i, token);
                    case "--replace-text" -> {
                        operation = EditOperation.REPLACE_TEXT;
                        replacement = requireValue(tokens, ++i, token);
                    }
                    case "--replace-attr" -> {
                        operation = EditOperation.REPLACE_ATTRIBUTE_VALUE;
                        String[] pair = splitKeyValue(requireValue(tokens, ++i, token), token);
                        operationAttributeName = pair[0];
                        replacement = pair[1];
                    }
                    case "--add-attr" -> {
                        operation = EditOperation.ADD_ATTRIBUTE;
                        String[] pair = splitKeyValue(requireValue(tokens, ++i, token), token);
                        operationAttributeName = pair[0];
                        operationAttributeValue = pair[1];
                    }
                    case "--remove-attr" -> {
                        operation = EditOperation.REMOVE_ATTRIBUTE;
                        operationAttributeName = requireValue(tokens, ++i, token);
                    }
                    case "--add-node-xml" -> {
                        operation = EditOperation.ADD_CHILD_XML;
                        nodeXml = requireValue(tokens, ++i, token);
                    }
                    case "--remove-node" -> operation = EditOperation.REMOVE_NODE;
                    case "--dry-run" -> dryRun = true;
                    default -> {
                        if (!"--help".equals(token)) {
                            throw new IllegalArgumentException("Unsupported argument: " + token);
                        }
                    }
                }
            }

            if (input == null) {
                throw new IllegalArgumentException("--input is required");
            }
            if (operation == null) {
                throw new IllegalArgumentException("One edit operation must be selected");
            }

            return new CliArguments(
                    input,
                    output,
                    report,
                    tagName,
                    criteriaAttributeName,
                    criteriaAttributeValueContains,
                    textContains,
                    textRegex,
                    xpathContains,
                    ancestorTagName,
                    ancestorTextContains,
                    operation,
                    replacement,
                    operationAttributeName,
                    operationAttributeValue,
                    nodeXml,
                    dryRun);
        }

        private static String requireValue(List<String> args, int index, String option) {
            if (index >= args.size()) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args.get(index);
        }

        private static String[] splitKeyValue(String input, String option) {
            int index = input.indexOf('=');
            if (index <= 0) {
                throw new IllegalArgumentException(option + " value must be in key=value format");
            }
            return new String[]{input.substring(0, index), input.substring(index + 1)};
        }
    }
}
