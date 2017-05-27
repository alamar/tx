package models;

import play.libs.Json;

import java.util.Collections;
import java.util.Iterator;

import play.libs.F.Either;
import play.mvc.Controller;
import play.mvc.Result;

public class ModelResult<T> implements Iterable<T> {
    private final Either<T, Result> payload;

    private ModelResult(Either<T, Result> payload) {
        this.payload = payload;
    }

    public static <T> ModelResult<T> fail(Result reason) {
        return new ModelResult<T>(Either.Right(reason));
    }

    public static <T> ModelResult<T> model(T model) {
        return new ModelResult<T>(Either.Left(model));
    }

    public Result asJsonResult() {
        return payload.left.isPresent()
                ? Controller.ok(Json.toJson(payload.left.get()))
                : payload.right.get();
    }

    @Override
    public Iterator<T> iterator() {
        return payload.left.isPresent()
                ? Collections.singleton(payload.left.get()).iterator()
                : Collections.<T>emptyIterator();
    }

    public <Y> ModelResult<Y> cast() {
        return payload.left.isPresent()
                ? ModelResult.<Y>fail(Controller.internalServerError("Invalid cast"))
                : ModelResult.<Y>fail(payload.right.get());
    }
}
