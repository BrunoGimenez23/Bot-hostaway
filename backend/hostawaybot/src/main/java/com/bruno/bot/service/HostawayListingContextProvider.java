package com.bruno.bot.service;

import com.bruno.bot.client.hostaway.HostawayListingsClient;
import com.bruno.bot.config.HostawayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "hostaway", name = "enabled", havingValue = "true")
public class HostawayListingContextProvider implements ListingContextProvider {

    private final HostawayListingsClient listingsClient;
    private final ListingContextBuilder builder;
    private final HostawayProperties props;

    public HostawayListingContextProvider(HostawayListingsClient listingsClient,
                                          ListingContextBuilder builder,
                                          HostawayProperties props) {
        this.listingsClient = listingsClient;
        this.builder = builder;
        this.props = props;
    }

    @Override
    public String getContext(Long listingId) {
        var listing = listingsClient.getListing(listingId);
        return builder.build(listing);
    }

    @Override
    public String getMode() {
        return "HOSTAWAY";
    }
}
