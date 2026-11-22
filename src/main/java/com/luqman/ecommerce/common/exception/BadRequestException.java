// BadRequestException.java
package com.luqman.ecommerce.common.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}