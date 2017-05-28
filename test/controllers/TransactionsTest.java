package controllers;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.F.Tuple3;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import static controllers.TestHelpers.extractId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.route;
import static play.test.Helpers.contentAsString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionsTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    private Tuple3<String, String, String> prepareAccounts() {
        Result createJohnResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users")
                .bodyForm(Collections.singletonMap("login", "johndoe")));
        assertEquals(OK, createJohnResult.status());

        Result createJaneResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users")
                .bodyForm(Collections.singletonMap("login", "janedoe")));
        assertEquals(OK, createJaneResult.status());

        Result createJohnMainAccountResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs")
                .bodyForm(Collections.singletonMap("title", "Main Account")));
        assertEquals(OK, createJohnMainAccountResult.status());
        verifyAccount(createJohnMainAccountResult, "johndoe", "Main Account", 0L);

        Result createJaneAccountResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri(String.format("/users/%s/accs", extractId(createJaneResult)))
                .bodyForm(Collections.singletonMap("title", "account")));
        assertEquals(OK, createJaneAccountResult.status());
        verifyAccount(createJaneAccountResult, "janedoe", "account", 0L);

        Result createJohnAuxAccountResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs")
                .bodyForm(Collections.singletonMap("title", "Additional Account")));
        assertEquals(OK, createJohnAuxAccountResult.status());
        verifyAccount(createJohnAuxAccountResult, "johndoe", "Additional Account", 0L);

        String johnMainId = extractId(createJohnMainAccountResult);
        // Transfer from internal account in order to create balance
        Result addFundsResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", "00000000-0000-0000-c000-000000000046")
                        .put("amount", "-10")
                        .put("source", "internal").build()));
        assertEquals(OK, addFundsResult.status());
        verifyTransaction(addFundsResult, "00000000-0000-0000-c000-000000000046", johnMainId, 10L);

        return new Tuple3<>(johnMainId,
                extractId(createJohnAuxAccountResult), extractId(createJaneAccountResult));
    }

    @Test
    public void testPositive() {
        Tuple3<String, String, String> accounts = prepareAccounts();

        String johnMainId = accounts._1;
        String johnAuxId = accounts._2;
        String janeId = accounts._3;

        Http.RequestBuilder readJohnRequest = new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johndoe");
        Result readJohnResult = route(app, readJohnRequest);
        String johnUserId = extractId(readJohnResult);

        // Several transfers between accounts
        Result moveToJane = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", janeId)
                        .put("amount", "4").build()));
        assertEquals(OK, moveToJane.status());
        verifyTransaction(moveToJane, johnMainId, janeId, 4L);

        Result moveToAux = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnAuxId)
                        .put("amount", "3").build()));
        assertEquals(OK, moveToAux.status());
        verifyTransaction(moveToAux, johnMainId, johnAuxId, 3L);

        Result moveJaneToAux = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/janedoe/accs/" + janeId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnAuxId)
                        .put("amount", "2").build()));
        assertEquals(OK, moveJaneToAux.status());
        verifyTransaction(moveJaneToAux, janeId, johnAuxId, 2L);

        Result moveFromAux = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri(String.format("/users/%s/accs/%s/txs", johnUserId, johnAuxId))
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnMainId)
                        .put("amount", "5").build()));
        assertEquals(OK, moveFromAux.status());
        verifyTransaction(moveFromAux, johnAuxId, johnMainId, 5L);

        Result readJohnAccountsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/" + johnUserId + "/accs"));
        assertEquals(OK, readJohnAccountsResult.status());
        JsonNode johnAccounts = Json.parse(contentAsString(readJohnAccountsResult));
        assertEquals(2, johnAccounts.size());
        verifyAccount(johnAccounts.get(0), "johndoe", "Main Account", 8L);
        verifyAccount(johnAccounts.get(1), "johndoe", "Additional Account", 0L);

        Result readJohnsMainTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri(String.format("/users/%s/accs/%s/txs", johnUserId, johnMainId)));
        assertEquals(OK, readJohnsMainTransactionsResult.status());
        JsonNode johnMainTransactions = Json.parse(contentAsString(readJohnsMainTransactionsResult));
        assertEquals(4, johnMainTransactions.size());
        verifyTransaction(johnMainTransactions.get(0),
                "00000000-0000-0000-c000-000000000046", johnMainId, 10L);
        verifyTransaction(johnMainTransactions.get(1), johnMainId, janeId, 4L);
        verifyTransaction(johnMainTransactions.get(2), johnMainId, johnAuxId, 3L);
        verifyTransaction(johnMainTransactions.get(3), johnAuxId, johnMainId, 5L);

        Result readJohnsAuxTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johndoe/accs/" + johnAuxId + "/txs"));
        assertEquals(OK, readJohnsAuxTransactionsResult.status());
        JsonNode johnAuxTransactions = Json.parse(contentAsString(readJohnsAuxTransactionsResult));
        assertEquals(3, johnAuxTransactions.size());
        verifyTransaction(johnAuxTransactions.get(0), johnMainId, johnAuxId, 3L);
        verifyTransaction(johnAuxTransactions.get(1), janeId, johnAuxId, 2L);
        verifyTransaction(johnAuxTransactions.get(2), johnAuxId, johnMainId, 5L);

        Result readJaneAccountResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/janedoe/accs"));
        assertEquals(OK, readJaneAccountResult.status());
        verifyAccount(Json.parse(contentAsString(readJaneAccountResult)).get(0),
                "janedoe", "account", 2L);

        Result readJaneTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/janedoe/accs/" + janeId + "/txs"));
        assertEquals(OK, readJaneAccountResult.status());
        JsonNode janeTransactions = Json.parse(contentAsString(readJaneTransactionsResult));
        assertEquals(2, janeTransactions.size());
        verifyTransaction(janeTransactions.get(0), johnMainId, janeId, 4L);
        verifyTransaction(janeTransactions.get(1), janeId, johnAuxId, 2L);
    }

    private void verifyAccount(Result result, String login, String title, long balance) {
        JsonNode account = Json.parse(contentAsString(result));
        verifyAccount(account, login, title, balance);
    }

    private void verifyAccount(JsonNode account, String login, String title, long balance) {
        UUID.fromString(account.get("id").asText());
        assertEquals(title, account.get("title").asText());
        assertEquals(login, account.get("user").get("login").asText());
        assertEquals((double) balance, account.get("balance").get("value").asDouble(), 0.0);
        assertTrue(account.get("created").asDouble() > 0);
    }

    private void verifyTransaction(Result result, String from, String to, long amount) {
        JsonNode account = Json.parse(contentAsString(result));
        verifyTransaction(account, from, to, amount);
    }

    private void verifyTransaction(JsonNode account, String from, String to, long amount) {
        UUID.fromString(account.get("id").asText());
        UUID.fromString(account.get("from").asText());
        UUID.fromString(account.get("to").asText());
        assertEquals(from, account.get("from").asText());
        assertEquals(to, account.get("to").asText());
        assertEquals(amount, account.get("amount").get("value").asLong());
        assertTrue(account.get("created").asDouble() > 0);
    }

    @Test
    public void testNegative() {
        Tuple3<String, String, String> accounts = prepareAccounts();

        String johnMainId = accounts._1;
        String johnAuxId = accounts._2;
        String janeId = accounts._3;

        Http.RequestBuilder readJaneRequest = new Http.RequestBuilder()
                .method(GET)
                .uri("/users/janedoe");
        Result readJaneResult = route(app, readJaneRequest);
        String janeUserId = extractId(readJaneResult);

        Result insufficientResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnAuxId)
                        .put("amount", "13").build()));
        assertEquals(FORBIDDEN, insufficientResult.status());

        Result insufficientReturnInternalResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri(String.format("/users/%s/accs/%s/txs", janeUserId, janeId))
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", "00000000-0000-0000-c000-000000000046")
                        .put("amount", "5")
                        .put("source", "internal").build()));
        assertEquals(FORBIDDEN, insufficientReturnInternalResult.status());

        Result wrongInternalAccountResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/janedoe/accs/" + janeId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", "00000000-0000-0000-c000-00000000004b")
                        .put("amount", "-15")
                        .put("source", "internal").build()));
        assertEquals(BAD_REQUEST, wrongInternalAccountResult.status());

        Result internalAccountWrongSourceResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri(String.format("/users/%s/accs/%s/txs", janeUserId, janeId))
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", "00000000-0000-0000-c000-00000000004b")
                        .put("amount", "-15")
                        .put("source", "enternal").build()));
        assertEquals(BAD_REQUEST, internalAccountWrongSourceResult.status());

        Result accountWrongOwnerResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/janedoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnAuxId)
                        .put("amount", "5").build()));
        assertEquals(NOT_FOUND, accountWrongOwnerResult.status());

        Result accountWrongTargetResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", janeUserId)
                        .put("amount", "5").build()));
        assertEquals(BAD_REQUEST, accountWrongTargetResult.status());

        Result sameAccountResult = route(app, new Http.RequestBuilder()
                .method(POST)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs")
                .bodyForm(ImmutableMap.<String, String>builder()
                        .put("to", johnMainId)
                        .put("amount", "5").build()));
        assertEquals(BAD_REQUEST, sameAccountResult.status());
    }

    private static final int LOAD_REQUESTS = 12345;
    @Test
    public void testLoad() throws InterruptedException {
        Tuple3<String, String, String> accounts = prepareAccounts();

        String johnMainId = accounts._1;
        String johnAuxId = accounts._2;
        final String janeId = accounts._3;

        final List<String> accountsList = Arrays.asList(johnMainId, johnAuxId, janeId);
        final AtomicInteger oks = new AtomicInteger(0);
        final AtomicInteger fails = new AtomicInteger(0);
        final AtomicBoolean isFailure = new AtomicBoolean(false);

        ThreadPoolExecutor load = new ThreadPoolExecutor(8, 8, 50, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(LOAD_REQUESTS));
        for (int i = 0; i < LOAD_REQUESTS; i++) {
            load.execute(new Runnable() {
                private Random r = new Random();

                public void run() {
                    String from = accountsList.get(r.nextInt(3));
                    String to;
                    do {
                        to = accountsList.get(r.nextInt(3));
                    } while (to.equals(from));

                    Result tryMove = route(app, new Http.RequestBuilder()
                            .method(POST)
                            .uri(String.format("/users/%s/accs/%s/txs",
                                    from.equals(janeId) ? "janedoe" : "johndoe", from))
                            .bodyForm(ImmutableMap.<String, String>builder()
                                    .put("to", to)
                                    .put("amount", Integer.toString(r.nextInt(5) + 1)).build()));
                    if (FORBIDDEN == tryMove.status()) {
                        fails.incrementAndGet();
                    } else if (OK == tryMove.status()) {
                        oks.incrementAndGet();
                    } else {
                        isFailure.set(true);
                        throw new IllegalStateException("Unexpected result: " + tryMove.status());
                    }
                }
            });
        }

        load.shutdown();
        load.awaitTermination(1, TimeUnit.MINUTES);

        assertTrue(!isFailure.get());
        assertTrue(oks.get() > 0);
        assertTrue(fails.get() > 0);
        assertEquals(LOAD_REQUESTS, oks.get() + fails.get());

        Result readJohnMainTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johndoe/accs/" + johnMainId + "/txs"));
        assertEquals(OK, readJohnMainTransactionsResult.status());
        JsonNode johnMainTransactions = Json.parse(contentAsString(readJohnMainTransactionsResult));
        long johnMainBalance = balanceAccount(johnMainId, johnMainTransactions);

        Result readJohnAuxTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johndoe/accs/" + johnAuxId + "/txs"));
        assertEquals(OK, readJohnAuxTransactionsResult.status());
        JsonNode johnAuxTransactions = Json.parse(contentAsString(readJohnAuxTransactionsResult));
        long johnAuxBalance = balanceAccount(johnAuxId, johnAuxTransactions);

        Result readJaneTransactionsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/janedoe/accs/" + janeId + "/txs"));
        assertEquals(OK, readJaneTransactionsResult.status());
        JsonNode janeTransactions = Json.parse(contentAsString(readJaneTransactionsResult));
        long janeBalance = balanceAccount(janeId, janeTransactions);

        assertEquals(10, johnMainBalance + janeBalance + johnAuxBalance);

        Result readJohnAccountsResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/johndoe/accs"));
        assertEquals(OK, readJohnAccountsResult.status());
        JsonNode johnAccounts = Json.parse(contentAsString(readJohnAccountsResult));
        assertEquals(2, johnAccounts.size());
        verifyAccount(johnAccounts.get(0), "johndoe", "Main Account", johnMainBalance);
        verifyAccount(johnAccounts.get(1), "johndoe", "Additional Account", johnAuxBalance);

        Result readJaneAccountResult = route(app, new Http.RequestBuilder()
                .method(GET)
                .uri("/users/janedoe/accs"));
        assertEquals(OK, readJaneAccountResult.status());
        verifyAccount(Json.parse(contentAsString(readJaneAccountResult)).get(0),
                "janedoe", "account", janeBalance);
    }

    private long balanceAccount(String accId, JsonNode txs) {
        assertTrue(txs.size() > 0);
        long accountTotal = 0;
        for (int i = 0; i < txs.size(); i++) {
            JsonNode tx = txs.get(i);
            long amount = tx.get("amount").get("value").asLong();
            assertTrue(amount > 0L);
            if (tx.get("to").asText().equals(accId)) {
                accountTotal += amount;
            } else if (tx.get("from").asText().equals(accId)) {
                accountTotal -= amount;
            } else {
                fail();
            }
            assertTrue(accountTotal >= 0L);
        }
        return accountTotal;
    }

}
