package ch.so.agi.search;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class LuceneSearcherException extends Exception {

    private static final long serialVersionUID = 1L;

    public LuceneSearcherException(String msg) {
        super(msg);
    }
}
