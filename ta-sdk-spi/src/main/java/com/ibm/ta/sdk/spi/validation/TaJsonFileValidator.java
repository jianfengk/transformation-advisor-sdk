/*
 * (C) Copyright IBM Corp. 2019,2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.ta.sdk.spi.validation;

//import org.leadpony.justify.api.*;
//import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class TaJsonFileValidator {
    private static final String ISSUE_SCHEMA = "schema/issue.schema.json";
    private static final String TARGET_SCHEMA = "schema/target.schema.json";
    private static final String COMPLEXITY_SCHEMA = "schema/complexity.schema.json";
    private static final String RECOMMENDATION_SCHEMA = "schema/recommendation.schema.json";

/*
    // The only instance of JSON validation service.
    private static JsonValidationService service = JsonValidationService.newInstance();

    // The configured factory which will produce schema readers.
    private static JsonSchemaReaderFactory readerFactory;
*/
    /**
     * Reads the JSON schema from the specified path.
     *
     * @param inputStream the path to the schema.
     * @return the read schema.
     */
/*
    private static JsonSchema readSchema(InputStream inputStream) {
        try (JsonSchemaReader reader = readerFactory.createSchemaReader(inputStream)) {
            return reader.read();
        }
    }
*/
    /**
     * Resolves the referenced JSON schema.
     *
     * @param id the identifier of the referenced JSON schema.
     * @return referenced JSON schema.
     */
/*
    private static JsonSchema resolveSchema(URI id) {
        //System.out.println(id.getPath());
        // The schema is available in the local filesystem.
        InputStream schemaResources = TaJsonFileValidator.class.getClassLoader().getResourceAsStream(id.getPath());
        assert schemaResources != null;
        return readSchema(schemaResources);
    }

    private static ProblemHandler getProblemHandler(Consumer<String> handler) {
        return service.createProblemPrinter(handler);
    }
*/
    /**
     * Run a test to a json file at jsonResourcePath against a schema at schemaPath
     *
     * @param schemaPath       the path to the JSON schema file in resource.
     * @param jsonResourcePath the path to the JSON file to be validated in resource.
     */
    public static boolean validateJsonBySchema(String schemaPath, String jsonResourcePath) {
        boolean isValid = true;

/*
        List<String> problemList = new ArrayList<>();
        // Builds a factory of schema readers.
        readerFactory = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(TaJsonFileValidator::resolveSchema)
                .build();

        try {
            //System.out.println("zzzzz = "+schemaPath);
            InputStream jsonResources = getResource(jsonResourcePath);
            InputStream schemaResources = getResource(schemaPath);
            assert schemaResources != null;
            JsonSchema schema = readSchema(schemaResources);
            // Problem handler
            ProblemHandler handler = getProblemHandler(problemList::add);
            if (jsonResources != null) {
                JsonReader reader = service.createReader(jsonResources, schema, handler);
                reader.readValue();

                if (problemList.size() > 0) {
                    isValid = false;
                    System.out.println("For resource " + jsonResourcePath + ", ");
                    System.out.println("We have found problems below:");
                    problemList.forEach(System.out::println);
                }
            } else {
                System.err.println("Cannot find file " + jsonResourcePath + " to validate");
            }
        } catch (Exception e) {
            System.err.println("We have problem to process the json files.");
            e.printStackTrace();
            isValid = false;
        }
*/
        return isValid;
    }

    public static boolean validateIssue(String issueJsonFile) {
        return validateJsonBySchema(ISSUE_SCHEMA, issueJsonFile);
    }

    public static boolean validateTarget(String targetJsonFile) {
        return validateJsonBySchema(TARGET_SCHEMA, targetJsonFile);
    }

    public static boolean validateComplexity(String complexityJsonFile) {
        return validateJsonBySchema(COMPLEXITY_SCHEMA, complexityJsonFile);
    }

    public static boolean validateRecommendation(String recommendationJsonFile) {
        return validateJsonBySchema(RECOMMENDATION_SCHEMA, recommendationJsonFile);
    }

    private static InputStream getResource(String filePath) throws FileNotFoundException {
        File realFile = new File(filePath);
        if (realFile.exists()) {
            return new FileInputStream(realFile);
        } else {
            return TaJsonFileValidator.class.getClassLoader().getResourceAsStream(filePath);
        }
    }

    public static void main(String[] argu) {
        System.out.println("start");
        //validateComplexity("recommendation.json");
        boolean result = validateRecommendation("recommendation.json");
        System.out.println("end="+result);
    }

}
