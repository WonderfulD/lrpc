package space.ruiwang.exception;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-25
 */
public class FaultTolerantException extends RuntimeException {
    public FaultTolerantException() {
        super();
    }

    public FaultTolerantException(String message, Throwable cause) {
        super(message, cause);
    }

    public FaultTolerantException(String message) {
        super(message);
    }

    public FaultTolerantException(Throwable cause) {
        super(cause);
    }
}
