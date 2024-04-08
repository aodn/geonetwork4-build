package au.org.aodn.geonetwork_api.openapi.api;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import static org.junit.Assert.assertTrue;

public class ParserTest {

    protected Logger logger = LogManager.getLogger(HarvestersApiLegacy.class);

    @Test
    public void verifyConvertToXML() throws IOException {
        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_cdu_eretmochelys_imbricata.json"),
                StandardCharsets.UTF_8);

        String expected = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_cdu_eretmochelys_imbricata.xml"),
                StandardCharsets.UTF_8);

        Parser.Parsed parsed = new Parser().parseHarvestersConfig(json);

        Diff d = DiffBuilder
                .compare(parsed.getXml())
                .withTest(expected)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .checkForSimilar()
                .ignoreWhitespace()
                .ignoreComments()
                .build();

        assertTrue("Expected result", !d.hasDifferences());
    }
}
