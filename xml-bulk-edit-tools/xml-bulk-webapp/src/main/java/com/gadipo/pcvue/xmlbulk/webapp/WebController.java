package com.gadipo.pcvue.xmlbulk.webapp;

import com.gadipo.pcvue.xmlbulk.core.EditOperation;
import com.gadipo.pcvue.xmlbulk.core.EditRequest;
import com.gadipo.pcvue.xmlbulk.core.EditResult;
import com.gadipo.pcvue.xmlbulk.core.SearchCriteria;
import com.gadipo.pcvue.xmlbulk.core.XmlBulkEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final XmlBulkEditor editor = new XmlBulkEditor();

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("operationValues", EditOperation.values());
        return "index";
    }

    @PostMapping("/apply")
    public String apply(
            @RequestParam("xmlInput") String xmlInput,
            @RequestParam(value = "tagName", required = false) String tagName,
            @RequestParam(value = "attributeName", required = false) String attributeName,
            @RequestParam(value = "attributeValueContains", required = false) String attributeValueContains,
            @RequestParam(value = "textContains", required = false) String textContains,
            @RequestParam(value = "textRegex", required = false) String textRegex,
            @RequestParam(value = "xpathContains", required = false) String xpathContains,
            @RequestParam(value = "xpathExpression", required = false) String xpathExpression,
            @RequestParam("operation") EditOperation operation,
            @RequestParam(value = "replacement", required = false) String replacement,
            @RequestParam(value = "operationAttributeName", required = false) String operationAttributeName,
            @RequestParam(value = "operationAttributeValue", required = false) String operationAttributeValue,
            @RequestParam(value = "nodeXml", required = false) String nodeXml,
            @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun,
            Model model) {

        model.addAttribute("operationValues", EditOperation.values());
        model.addAttribute("xmlInput", xmlInput);

        try {
            EditResult result = editor.edit(xmlInput, new EditRequest(
                    new SearchCriteria(tagName, attributeName, attributeValueContains, textContains, textRegex, xpathContains, xpathExpression),
                    operation,
                    replacement,
                    operationAttributeName,
                    operationAttributeValue,
                    nodeXml,
                    dryRun
            ));
            model.addAttribute("resultXml", result.outputXml());
            model.addAttribute("changes", result.changes());
            model.addAttribute("matchedNodeCount", result.matchedNodeCount());
            model.addAttribute("error", null);
        } catch (Exception ex) {
            model.addAttribute("resultXml", "");
            model.addAttribute("changes", java.util.List.of());
            model.addAttribute("matchedNodeCount", 0);
            model.addAttribute("error", ex.getMessage());
        }

        return "index";
    }
}
