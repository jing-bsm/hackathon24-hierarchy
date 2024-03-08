package csv.hierarchy.pg.persistent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoRetryableException extends RuntimeException {
    private List<String> errors;

    public NoRetryableException(String message, Throwable t) {
        super(message, t);
    }

    public NoRetryableException(String message) {
        super(message);
    }
}
