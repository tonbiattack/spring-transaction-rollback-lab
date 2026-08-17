package com.example.rollbacklab;

public class OrderRejectedException extends Exception {
    public OrderRejectedException(String message) {
        super(message);
    }
}
