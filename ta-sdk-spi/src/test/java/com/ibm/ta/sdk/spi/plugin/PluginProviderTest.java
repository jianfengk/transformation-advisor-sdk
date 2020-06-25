/*
 * (C) Copyright IBM Corp. 2019,2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.ta.sdk.spi.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.ta.sdk.spi.assess.UTRecommendation;
import com.ibm.ta.sdk.spi.collect.EnvironmentJson;
import com.ibm.ta.sdk.spi.collect.UTAssessmentUnit;
import com.ibm.ta.sdk.spi.collect.UTDataCollection;
import com.ibm.ta.sdk.spi.test.TestUtils;
import com.ibm.ta.sdk.spi.validation.TaCollectionZipValidator;
import com.ibm.ta.sdk.spi.validation.TaJsonFileValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipInputStream;

import static com.ibm.ta.sdk.spi.test.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ibm.ta.sdk.spi.test.ValidationUtils.*;

public class PluginProviderTest {
    private static Logger logger = LogManager.getLogger(PluginProviderTest.class.getName());


    /*
     * Test assessment of single collection that contains a single asssessment unit and no config files
     */
    @Test
    public void commandNotSupportedTest() {
        try {
            UTPluginProvider provider = new UTPluginProvider();
            assertThrows(IllegalArgumentException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_RUN, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_REPORT, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });
        } catch (Exception e) {
            throw new AssertionFailedError("Error with executing command:", e);
        }
    }

    /*
     * Test collect command that return null data collection
     */
    @Test
    public void collectCommandNullDataCollectionTest() {
        try {
            // Collect command
            UTPluginProvider provider = new UTPluginProvider();
            CliInputOption collectCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(collectCmdAllOpt));
            CliInputCommand collectCmd = CliInputCommand.buildCollectCommand(collectionCmdOpts, null,
                    Arrays.asList("dataPath"));
            provider.setCollectCommand(collectCmd);

            Exception e = assertThrows(TAException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });
            assertEquals("Collect failed. No recommendations generated by plugin provider.", e.getMessage());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with collect command for a null DataCollection", e);
        }
    }

    /*
     * Test a single collection that contains a single asssessment unit and no config files
     */
    @Test
    public void collectOneAssessmentUnitTest() {
        final String collectionUnitName = "TestCollectionUnit";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption collectCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(collectCmdAllOpt));
            CliInputCommand collectCmd = CliInputCommand.buildCollectCommand(collectionCmdOpts, null,
                    Arrays.asList("dataPath"));
            provider.setCollectCommand(collectCmd);

            UTDataCollection dc = new UTDataCollection(collectionUnitName, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName);
            provider.setDataCollection(Arrays.asList(dc));

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert collection unit output
            List<String> auList = Arrays.asList("NewYork");
            assertCollection(collectionUnitName, auList, new HashMap<>());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with collect command for a single assessment unit", e);
        }
    }

    /*
     * Verify the config files in a data collection
     */
    @Test
    public void collectAssessmentUnitConfigFilesTest() {
        final String collectionUnitName = "TestCollectionUnit";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption collectCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(collectCmdAllOpt));
            CliInputCommand collectCmd = CliInputCommand.buildCollectCommand(
                    collectionCmdOpts, null, Arrays.asList("dataPath"));
            provider.setCollectCommand(collectCmd);

            UTDataCollection dc = new UTDataCollection(collectionUnitName, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName);
            provider.setDataCollection(Arrays.asList(dc));

            // Add config files to assessment unit
            String configFile1 = "configFiles" + File.separator + "configFile1";
            String configFile2 = "configFiles" + File.separator + "data" + File.separator + "configFile2";
            List<String> configFiles = Arrays.asList(configFile1, configFile2);
            UTAssessmentUnit newYorkAu = (UTAssessmentUnit) dc.getAssessmentUnits().get(0);
            newYorkAu.setConfigFiles(configFiles);

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Check no config files in the assessment unit dir
            // TODO: Broken - Path in output is not preserved - changes made because of Windows
            List<String> expectedConfigFiles = Arrays.asList("configFile1", "configFile2");
            assertAssessmentUnitFiles(collectionUnitName, "NewYork", expectedConfigFiles);
        } catch (Exception e) {
            throw new AssertionFailedError("Error with collect command for assessment unit with config files", e);
        }
    }

    /*
     * Test a single collection that contains multiple asssessment units
     */
    @Test
    public void collectMultipleAssessmentUnitTest() {
        final String collectionUnitName = "TestCollectionUnit";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption collectCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(collectCmdAllOpt));
            CliInputCommand collectCmd = new CliInputCommand(CliInputCommand.CMD_COLLECT,
                    "Collects data",
                    collectionCmdOpts, null, Arrays.asList("dataPath"));
            provider.setCollectCommand(collectCmd);

            // 2 assessment units - NewYork.json and London.json
            UTDataCollection dc = new UTDataCollection(collectionUnitName, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json", "assessmentUnits/London/London.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName);
            provider.setDataCollection(Arrays.asList(dc));

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            List<String> auList = Arrays.asList("NewYork", "London");
            assertCollection(collectionUnitName, auList, new HashMap<>());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with collect command with multiple assessment units", e);
        }
    }

    /*
     * Test collection that returns more than one DataCollection
     */
    @Test
    public void collectMultipleCollectionsTest() {
        final String collectionUnitName1 = "TestCollectionUnit";
        final String collectionUnitName2 = "TestCollectionUnit2";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption collectCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(collectCmdAllOpt));
            CliInputCommand collectCmd = new CliInputCommand(CliInputCommand.CMD_COLLECT,
                    "Collects data",
                    collectionCmdOpts, null, Arrays.asList("dataPath"));
            provider.setCollectCommand(collectCmd);

            // 2 data collections
            UTDataCollection dc = new UTDataCollection(collectionUnitName1, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName1);
            UTDataCollection dc2 = new UTDataCollection(collectionUnitName2, "environment.json", Arrays.asList("assessmentUnits/London/London.json"));
            dc2.getEnvironmentJson().setCollectionUnitName(collectionUnitName2);
            provider.setDataCollection(Arrays.asList(dc, dc2));

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_COLLECT, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert collection unit 1 output
            assertCollection(collectionUnitName1, Arrays.asList("NewYork"), new HashMap<>());

            // Assert collection unit 2 output
            assertCollection(collectionUnitName2, Arrays.asList("London"), new HashMap<>());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with collect command for multiple data collections", e);
        }
    }

    /*
     *  Verifies the collected artifacts - environment.json and assessment unit artifacts
     */
    private void assertCollection(String collectionUnitName, List<String> auList, Map<String, List<String>> auConfigFiles) {
        // Assert collection unit output
        assertCollectionUnit(collectionUnitName);

        // Check environment json
        JsonObject middlewareMd = new JsonObject();
        middlewareMd.addProperty("feature", "drilling");
        JsonObject assessmentMd = new JsonObject();
        assessmentMd.addProperty("test", "value");
        assertEnvironmentJson("testDomain", "testMiddleware", "10.0",
                "RedHat Enterprise 7", "iib1.rtp.raleigh.ibm.com",
                "/opt/test", "/opt/testdata", middlewareMd,
                collectionUnitName, "Installation", assessmentMd,
                auList, "1.0.0");

        // Check assessment units
        assertAssessmemntUnits(collectionUnitName, auList);

        // Check assessment unit files
        for (String auName : auList) {
            List<String> configFiles = auConfigFiles.get(auName);
            if (configFiles == null) {
                configFiles = new ArrayList<>();
            }
            assertAssessmentUnitFiles(collectionUnitName, auName, configFiles);
        }
    }


    /*
     * Test assess command that return null data collection
     */
    @Test
    public void assessCommandNullDataCollectionTest() {
        try {
            // Assess command
            UTPluginProvider provider = new UTPluginProvider();
            CliInputOption assessCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> assessCmdOpts = new LinkedList<>(Arrays.asList(assessCmdAllOpt));
            CliInputCommand assessCmd = CliInputCommand.buildAssessCommand(assessCmdOpts, null,
                    Arrays.asList("dataPath"));
            provider.setAssessCommand(assessCmd);

            Exception e = assertThrows(TAException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });
            assertEquals("Collect failed. No recommendations generated by plugin provider.", e.getMessage());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with assess command for null data collection", e);
        }
    }

    /*
     * Test assess command that return null recommendations
     */
    @Test
    public void assessCommandNullRecommendationsTest() {
        final String collectionUnitName1 = "TestCollectionUnit";
        try {
            // Assess command
            UTPluginProvider provider = new UTPluginProvider();
            CliInputOption assessCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> assessCmdOpts = new LinkedList<>(Arrays.asList(assessCmdAllOpt));
            CliInputCommand assessCmd = CliInputCommand.buildAssessCommand(assessCmdOpts, null,
                    Arrays.asList("dataPath"));
            provider.setAssessCommand(assessCmd);

            UTDataCollection dc = new UTDataCollection(collectionUnitName1, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName1);
            provider.setDataCollection(Arrays.asList(dc));

            Exception e = assertThrows(TAException.class, () -> {
                List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "-a", "hello"));
                TestUtils.runPluginCommand(provider, cliCommands);
            });
            assertEquals("Assessment failed. No recommendations generated by plugin provider.", e.getMessage());
        } catch (Exception e) {
            throw new AssertionFailedError("Error with assess command", e);
        }
    }

    /*
     * Test assessment of single collection that contains a single asssessment unit and no config files
     */
    @Test
    public void assessOneCollectionTest() {
        final String collectionUnitName = "TestCollectionUnit";
        try {
            // Assess command
            UTPluginProvider provider = new UTPluginProvider();
            CliInputOption assessCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> assessCmdOpts = new LinkedList<>(Arrays.asList(assessCmdAllOpt));
            CliInputCommand assessCmd = CliInputCommand.buildAssessCommand(assessCmdOpts, null,
                    Arrays.asList("dataPath"));
            provider.setAssessCommand(assessCmd);

            // Data collection
            UTDataCollection dc = new UTDataCollection(collectionUnitName, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            EnvironmentJson envJson = dc.getEnvironmentJson();
            envJson.setCollectionUnitName(collectionUnitName);
            provider.setDataCollection(Arrays.asList(dc));

            // Recommendations
            Path recommendationsJsonFile = new File(TestUtils.TEST_RESOURCES_DIR,
                    "assessmentUnits/NewYork/recommendations.json").toPath();
            UTRecommendation recommendation = TestUtils.buildRecommendationsJsonObj(recommendationsJsonFile);
            provider.setRecommendations(Arrays.asList(recommendation));

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert collection unit output
            assertCollection(collectionUnitName, Arrays.asList("NewYork"), new HashMap<>());

            // Assert recommendations.json
            assertRecommendationsJson(collectionUnitName, recommendationsJsonFile);
        } catch (Exception e) {
            throw new AssertionFailedError("Error with assess command", e);
        }
    }


    /*
     * Test assessment that returns multiple collections and recommendations.json files
     */
    @Test
    public void assessMultipleCollectionsTest() {
        final String collectionUnitName1 = "TestCollectionUnit";
        final String collectionUnitName2 = "TestCollectionUnit2";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption assessCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(assessCmdAllOpt));
            CliInputCommand assessCmd = CliInputCommand.buildAssessCommand(
                    collectionCmdOpts, null, Arrays.asList("dataPath"));
            provider.setAssessCommand(assessCmd);

            // 2 data collections
            UTDataCollection dc = new UTDataCollection(collectionUnitName1, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName1);
            UTDataCollection dc2 = new UTDataCollection(collectionUnitName2, "environment.json", Arrays.asList("assessmentUnits/London/London.json"));
            dc2.getEnvironmentJson().setCollectionUnitName(collectionUnitName2);
            provider.setDataCollection(Arrays.asList(dc, dc2));

            // Recommendations
            Path recommendationsJsonFile = new File(TestUtils.TEST_RESOURCES_DIR,
                    "assessmentUnits/NewYork/recommendations.json").toPath();
            UTRecommendation recommendation = TestUtils.buildRecommendationsJsonObj(recommendationsJsonFile);
            Path recommendationsJsonFile2 = new File(TestUtils.TEST_RESOURCES_DIR,
                    "assessmentUnits/London/recommendations.json").toPath();
            UTRecommendation recommendation2 = TestUtils.buildRecommendationsJsonObj(recommendationsJsonFile2);
            provider.setRecommendations(Arrays.asList(recommendation, recommendation2));

            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "-a", "hello"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert collection unit 1 output
            assertCollection(collectionUnitName1, Arrays.asList("NewYork"), new HashMap<>());

            // Assert collection unit 2 output
            assertCollection(collectionUnitName2, Arrays.asList("London"), new HashMap<>());

            // Assert recommendations.json for collection unit 1
            assertRecommendationsJson(collectionUnitName1, recommendationsJsonFile);

            // Assert recommendations.json for collection unit 2
            assertRecommendationsJson(collectionUnitName2, recommendationsJsonFile2);

            // Validate zip archives
            File collectectionZip1 = new File(TEST_OUTPUT_DIR, collectionUnitName1 + ".zip");
            assertTrue(collectectionZip1.exists());
            TaCollectionZipValidator.validateArchive(new ZipInputStream(new FileInputStream(collectectionZip1)));

            File collectectionZip2 = new File(TEST_OUTPUT_DIR, collectionUnitName2 + ".zip");
            assertTrue(collectectionZip2.exists());
            TaCollectionZipValidator.validateArchive(new ZipInputStream(new FileInputStream(collectectionZip2)));
        } catch (Exception e) {
            throw new AssertionFailedError("Error with assess command for multiple DataCollections:", e);
        }
    }

    /*
     * Test assessment with the target option to select targets in the recommendations.json
     */
    @Test
    public void targetOptionTest() {
        final String collectionUnitName1 = "TestCollectionUnit";
        final String collectionUnitName2 = "TestCollectionUnit2";

        try {
            UTPluginProvider provider = new UTPluginProvider();
            // Collect command
            CliInputOption assessCmdAllOpt = new CliInputOption("a", "all", "Collect everything");
            List<CliInputOption> collectionCmdOpts = new LinkedList<>(Arrays.asList(assessCmdAllOpt));
            CliInputCommand assessCmd = CliInputCommand.buildAssessCommand(
                    collectionCmdOpts, null, Arrays.asList("dataPath"));
            provider.setAssessCommand(assessCmd);

            // 2 data collections
            UTDataCollection dc = new UTDataCollection(collectionUnitName1, "environment.json", Arrays.asList("assessmentUnits/NewYork/NewYork.json"));
            dc.getEnvironmentJson().setCollectionUnitName(collectionUnitName1);
            UTDataCollection dc2 = new UTDataCollection(collectionUnitName2, "environment.json", Arrays.asList("assessmentUnits/London/London.json"));
            dc2.getEnvironmentJson().setCollectionUnitName(collectionUnitName2);
            provider.setDataCollection(Arrays.asList(dc, dc2));

            // Recommendations
            Path recommendationsJsonFile = new File(TestUtils.TEST_RESOURCES_DIR,
                    "assessmentUnits/NewYork/recommendations_2targets.json").toPath();
            UTRecommendation recommendation = TestUtils.buildRecommendationsJsonObj(recommendationsJsonFile);
            Path recommendationsJsonFile2 = new File(TestUtils.TEST_RESOURCES_DIR,
                    "assessmentUnits/London/recommendations_2targets.json").toPath();
            UTRecommendation recommendation2 = TestUtils.buildRecommendationsJsonObj(recommendationsJsonFile2);
            provider.setRecommendations(Arrays.asList(recommendation, recommendation2));

            /************************************
             * target option specified in CLI with multiple targets
             ************************************/
            List<String> cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "--target", "OPEN_LIBERTY;WAS_LIBERTY", "dataPath"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert collection unit 1 output
            assertCollection(collectionUnitName1, Arrays.asList("NewYork"), new HashMap<>());

            // Assert collection unit 2 output
            assertCollection(collectionUnitName2, Arrays.asList("London"), new HashMap<>());

            // Assert recommendations.json for collection unit 1
            assertRecommendationsJson(collectionUnitName1, recommendationsJsonFile);

            // Assert recommendations.json for collection unit 2
            assertRecommendationsJson(collectionUnitName2, recommendationsJsonFile2);

            /************************************
             * No target option specified in CLI
             ************************************/
            cleanUp();
            cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "dataPath"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert recommendations.json for collection unit 1
            assertRecommendationsJson(collectionUnitName1, recommendationsJsonFile);

            // Assert recommendations.json for collection unit 2
            assertRecommendationsJson(collectionUnitName2, recommendationsJsonFile2);

            /************************************
             * target option specified in CLI with null value
             ************************************/
            cleanUp();
            cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "--target", "", "dataPath"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert recommendations.json for collection unit 1
            assertRecommendationsJson(collectionUnitName1, recommendationsJsonFile);

            // Assert recommendations.json for collection unit 2
            assertRecommendationsJson(collectionUnitName2, recommendationsJsonFile2);

            /************************************
             * target option specified in CLI with single target
             ************************************/
            cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "--target", "WAS_LIBERTY", "dataPath"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert recommendations.json for collection unit 1
            File recommendations1File = new File(TEST_OUTPUT_DIR, collectionUnitName1 + File.separator + RECOMMENDATIONS_JSON);
            JsonObject rec1Json = getJson(recommendations1File.toPath()).getAsJsonObject();
            JsonArray rec1Aus = rec1Json.get("assessmentUnits").getAsJsonArray();
            assertEquals(1, rec1Aus.size());
            JsonArray rec1Targets = rec1Aus.get(0).getAsJsonObject().get("targets").getAsJsonArray();
            assertEquals(1, rec1Targets.size());
            assertEquals("WAS_LIBERTY", rec1Targets.get(0).getAsJsonObject().get("id").getAsString());

            // Assert recommendations.json for collection unit 1
            File recommendations2File = new File(TEST_OUTPUT_DIR, collectionUnitName2 + File.separator + RECOMMENDATIONS_JSON);
            JsonObject rec2Json = getJson(recommendations2File.toPath()).getAsJsonObject();
            JsonArray rec2Aus = rec2Json.get("assessmentUnits").getAsJsonArray();
            assertEquals(1, rec2Aus.size());
            JsonArray rec2Targets = rec2Aus.get(0).getAsJsonObject().get("targets").getAsJsonArray();
            assertEquals(1, rec2Targets.size());
            assertEquals("WAS_LIBERTY", rec2Targets.get(0).getAsJsonObject().get("id").getAsString());


            /************************************
             * target option specified in CLI with no matching target
             ************************************/
            cliCommands = new LinkedList<>(Arrays.asList(CliInputCommand.CMD_ASSESS, "--target", "TARGET_NOT_FOUND", "dataPath"));
            TestUtils.runPluginCommand(provider, cliCommands);

            // Assert recommendations.json for collection unit 1
            rec1Json = getJson(recommendations1File.toPath()).getAsJsonObject();
            rec1Aus = rec1Json.get("assessmentUnits").getAsJsonArray();
            assertEquals(1, rec1Aus.size());
            assertNull(rec1Aus.get(0).getAsJsonObject().get("targets"));

            // Assert recommendations.json for collection unit 2
            rec2Json = getJson(recommendations2File.toPath()).getAsJsonObject();
            rec2Aus = rec2Json.get("assessmentUnits").getAsJsonArray();
            assertEquals(1, rec2Aus.size());
            assertNull(rec2Aus.get(0).getAsJsonObject().get("targets"));
        } catch (Exception e) {
            throw new AssertionFailedError("Error with assess command for multiple DataCollections:", e);
        }
    }

    @BeforeEach
    @AfterEach
    public void cleanUp() {
        TestUtils.deleteDir(new File(TestUtils.TEST_OUTPUT_DIR));
    }
}
