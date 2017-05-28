package controllers;

import static play.test.Helpers.contentAsString;

import play.libs.Json;
import play.mvc.Result;

public class TestHelpers {
    public static String extractId(Result result) {
        return Json.parse(contentAsString(result)).get("id").asText();
    }
}
