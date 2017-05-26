package models;

import play.libs.Json;
import play.libs.F.Either;
import play.mvc.Controller;
import play.mvc.Result;

public class ModelResult<T> {
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

    
}
