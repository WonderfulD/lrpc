package space.ruiwang.exception;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-25
 */
public class RetryLimitExceededException extends RuntimeException {
    public RetryLimitExceededException() {
        super();
    }

    public RetryLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryLimitExceededException(String message) {
        super(message);
    }

    public RetryLimitExceededException(Throwable cause) {
        super(cause);
    }
}
