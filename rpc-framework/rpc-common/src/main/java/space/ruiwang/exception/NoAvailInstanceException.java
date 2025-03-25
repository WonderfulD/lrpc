package space.ruiwang.exception;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-25
 */
public class NoAvailInstanceException extends RuntimeException {
    public NoAvailInstanceException() {
        super();
    }

    public NoAvailInstanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoAvailInstanceException(String message) {
        super(message);
    }

    public NoAvailInstanceException(Throwable cause) {
        super(cause);
    }
}
