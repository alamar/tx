package controllers;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static controllers.TestHelpers.extractId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.CONFLICT;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.route;
import static play.test.Helpers.contentAsString;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class UsersTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    @Test
    public void testCreateAndRead() {
        for (String login : Arrays.asList("j", "awesomE8power-rangers",
                "johndoe@example.com", "john.frederic.doe+123e4567@longmail.phys.ac.edu"))
        {
            Http.RequestBuilder createRequest = new Http.RequestBuilder()
                    .method(POST)
                    .uri("/users")
                    .bodyForm(Collections.singletonMap("login", login));
            Result createResult = route(app, createRequest);

            Http.RequestBuilder readLoginRequest = new Http.RequestBuilder()
                    .method(GET)
                    .uri("/users/" + login);
            Result readLoginResult = route(app, readLoginRequest);

            Http.RequestBuilder readIdRequest = new Http.RequestBuilder()
                    .method(GET)
                    .uri("/users/" + extractId(createResult));
            Result readIdResult = route(app, readIdRequest);

            for (Result result : Arrays.asList(createResult, readLoginResult, readIdResult)) {
                assertEquals(OK, result.status());
                JsonNode json = Json.parse(contentAsString(result));
                assertEquals(login, json.get("login").asText());
                assertTrue(json.get("created").asDouble() > 0);
                UUID.fromString(json.get("id").asText());
            }
        }
    }

    @Test
    public void testUniqueLogin() {
        Http.RequestBuilder createRequest = new Http.RequestBuilder()
                .method(POST)
                .uri("/users")
                .bodyForm(Collections.singletonMap("login", "johnym"));
        Result createResult = route(app, createRequest);
        assertEquals(OK, createResult.status());

        Result recreateResult = route(app, createRequest);
        assertEquals(CONFLICT, recreateResult.status());

        Http.RequestBuilder readRequest = new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johnym");
        Result readLoginResult = route(app, readRequest);
        assertEquals(OK, readLoginResult.status());
        assertEquals(extractId(createResult), extractId(readLoginResult));
    }

    @Test
    public void testCreateNegative() {
        for (String badLogin : Arrays.asList("123abc", "", "batman apollo", "/etc/passwd",
                "123e4567-e89b-12d3-a456-426655440000", "@gmail.com",
                "thisisaVeryLongLoginofMorethan36Characters"))
        {
            Http.RequestBuilder request = new Http.RequestBuilder()
                    .method(POST)
                    .uri("/users")
                    .bodyForm(Collections.singletonMap("login", badLogin));
            Result result = route(app, request);
            assertEquals(BAD_REQUEST, result.status());
        }
    }
}
