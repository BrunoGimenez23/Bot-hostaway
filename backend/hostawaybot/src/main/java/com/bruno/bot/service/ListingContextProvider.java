package com.bruno.bot.service;

public interface ListingContextProvider {
    String getContext(Long listingId);
    String getMode(); // "DEMO" o "HOSTAWAY"
}
