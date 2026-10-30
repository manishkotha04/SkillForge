package com.skillforge.skillforge.service;

public class PortfolioItemNotFoundException extends RuntimeException {

    public PortfolioItemNotFoundException(Long itemId) {
        super("Portfolio item not found: " + itemId);
    }
}