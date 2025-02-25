
package gaya.pe.kr.recipe.exception;

public class InsufficientRecipeDataException
        extends Exception {
    public InsufficientRecipeDataException() {
    }

    public InsufficientRecipeDataException(String message) {
        super(message);
    }

    public InsufficientRecipeDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

