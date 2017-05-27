package controllers;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import models.Account;
import models.Balance;
import models.ModelResult;
import models.Transaction;
import models.User;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Result;
import services.DataStore;


public class TransactionsController extends UsersSupport {
    private static final UUID INTERNAL_ACCOUNT = UUID.fromString(
            "00000000-0000-0000-c000-000000000046");
    private static final String INTERNAL_SOURCE = "internal";

    @Inject
    private DataStore dataStore = null;
    
    public Result getTransactions(String id, String acc) {
        ModelResult<Account> modelAccount = findAccount(id, acc);
        for (Account account : modelAccount) {
            return ok(Json.toJson(dataStore.getTransactionsOn(account)));
        }
        return modelAccount.asJsonResult();
    }

    public Result createTransaction(String id, String from) {
        ModelResult<Account> modelAccount = findAccount(id, from);
        for (Account fromAccount : modelAccount) {
            DynamicForm form = DynamicForm.form().bindFromRequest();
            String to = form.get("to");
            String amount = form.get("amount");
            String source = form.get("source");
            boolean internal = INTERNAL_SOURCE.equals(source);

            if (amount == null) {
                return badRequest("Absent amount parameter");
            }
            long amountValue;
            try {
                amountValue = Long.parseLong(amount);
                if (amountValue == 0L) {
                    return badRequest("Amount is zero");
                } else if (!internal && amountValue < 0L) {
                    return badRequest("Negative amount");
                }
            } catch (NumberFormatException nfe) {
                return badRequest("Unreadable amount");
            }

            if (to == null) {
                return badRequest("Absent to parameter");
            }

            UUID toId;
            try {
                toId = UUID.fromString(to);
                if (internal && !toId.equals(INTERNAL_ACCOUNT)) {
                    return badRequest("Unsupported internal account");
                }
            } catch (IllegalArgumentException iae) {
                return badRequest("Invalid to account");
            }

            Transaction tx = new Transaction(UUID.randomUUID(), fromAccount.id, toId,
                    Balance.fromValue(amountValue), Instant.now());

            if (internal) {
                synchronized (fromAccount) {
                    if (fromAccount.balance.value < amountValue) {
                        return forbidden("Insufficient funds");
                    }

                    if (amountValue < 0) {
                        tx = tx.invert();
                    }

                    dataStore.addTransactionToAccount(fromAccount, tx);
                    if (amountValue > 0) {
                        fromAccount.withdraw(amountValue);
                    } else {
                        fromAccount.deposit(-amountValue);
                    }
                }
            } else {
                Account toAccount = dataStore.getAccount(toId);
                if (toAccount == null) {
                    return badRequest("Absent to account");
                } else if (toAccount.equals(fromAccount)) {
                    return badRequest("Same from and to account");
                }

                boolean fromFirst = fromAccount.id.compareTo(toAccount.id) < 0;
                synchronized (fromFirst ? fromAccount : toAccount) {
                    synchronized (fromFirst ? toAccount : fromAccount) {
                        if (fromAccount.balance.value < amountValue) {
                            return forbidden("Insufficient funds");
                        }

                        dataStore.addTransactionToAccount(fromAccount, tx);
                        dataStore.addTransactionToAccount(toAccount, tx);
                        fromAccount.withdraw(amountValue);
                        toAccount.deposit(amountValue);
                    }
                }
            }
            return ok(Json.toJson(tx));
        }
        return modelAccount.asJsonResult();
    }

    private ModelResult<Account> findAccount(String id, String acc) {
        ModelResult<User> modelUser = findUser(id);
        for (User user : modelUser) {
            UUID uuid;
            try {
                uuid = UUID.fromString(acc);
            } catch (IllegalArgumentException iae) {
                return ModelResult.<Account>fail(badRequest("Invalid account"));
            }
            Account account = dataStore.getAccount(uuid);
            if (account == null || !account.user.equals(user)) {
                return ModelResult.<Account>fail(notFound("Account not found"));
            }
            return ModelResult.<Account>model(account);
        }
        return modelUser.<Account>cast();
    }

    public Result createAccount(String id) {
        ModelResult<User> modelUser = findUser(id);
        for (User user : modelUser) {
            DynamicForm form = DynamicForm.form().bindFromRequest();
            String title = form.get("title");
            if (title == null) {
                return badRequest("Absent title parameter");
            }
            if (!title.matches("^[-_.a-zA-Z0-9 ]{0,35}$")) {
                return badRequest("Invalid title");
            }
            Account account = new Account(UUID.randomUUID(), user, title, Instant.now());
            boolean success = (dataStore.addAccount(account) != null);
            assert (success);
            return ok(Json.toJson(account));
        }
        return modelUser.asJsonResult();
    }
}
