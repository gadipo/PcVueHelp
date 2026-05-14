package com.gadipo.pcvue.xmlbulk.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlBulkEditorTest {

    private final XmlBulkEditor editor = new XmlBulkEditor();

    @Test
    void shouldReplaceAttributeByNestedCriteria() {
        String xml = """
                <Root>
                  <Object>
                    <FreeParameter Name=\"BACnet_EDEKeyname\">Vitania'Floor3'Fire_Det_F1</FreeParameter>
                    <Property id=\"Alarm\">0</Property>
                  </Object>
                </Root>
                """;

        EditResult result = editor.edit(xml, new EditRequest(
                new SearchCriteria("Property", "id", "Alarm", null, null, "/Root/Object", "Object", "Fire_Det"),
                EditOperation.REPLACE_TEXT,
                "-1",
                null,
                null,
                null,
                false
        ));

        assertEquals(1, result.matchedNodeCount());
        assertTrue(result.outputXml().contains("<Property id=\"Alarm\">-1</Property>"));
        assertEquals(1, result.changes().size());
    }

    @Test
    void shouldPreviewWithoutChangingOutputWhenDryRun() {
        String xml = """
                <Root><Item status=\"old\">value</Item></Root>
                """;

        EditResult result = editor.edit(xml, new EditRequest(
                new SearchCriteria("Item", "status", "old", null, null, null, null, null),
                EditOperation.REPLACE_ATTRIBUTE_VALUE,
                "new",
                "status",
                null,
                null,
                true
        ));

        assertEquals(xml, result.outputXml());
        assertEquals(1, result.changes().size());
        assertEquals("new", result.changes().get(0).afterValue());
    }

    @Test
    void shouldRemoveNodeUsingTextMatch() {
        String xml = """
                <Root>
                  <TemplateInstance Name=\"binary_input\">
                    <FreeParameter Name=\"BACnet_EDEKeyname\">Vitania'Floor3'Fire_Det_F1</FreeParameter>
                  </TemplateInstance>
                  <TemplateInstance Name=\"other\">
                    <FreeParameter Name=\"BACnet_EDEKeyname\">Vitania'Floor3'Other</FreeParameter>
                  </TemplateInstance>
                </Root>
                """;

        EditResult result = editor.edit(xml, new EditRequest(
                new SearchCriteria("TemplateInstance", "Name", "binary_input", "Fire_Det", null, null, null, null),
                EditOperation.REMOVE_NODE,
                null,
                null,
                null,
                null,
                false
        ));

        assertEquals(1, result.matchedNodeCount());
        assertTrue(result.outputXml().contains("Name=\"other\""));
        assertFalse(result.outputXml().contains("Name=\"binary_input\""));
    }
}
