package ru.otus.exception;

public class AtmException extends RuntimeException {

    public AtmException(AtmError error, String arg) {
        super(String.format(error.getMessege(), arg));
    }

    public AtmException(AtmError error, int arg) {
        super(String.format(error.getMessege(), arg));
    }

    public AtmException(AtmError error, int arg1, int arg2) {
        super(String.format(error.getMessege(), arg1, arg2));
    }
}
