package com.travelmemory.location.service;

import java.net.URI;

public interface GoogleMapsRedirectResolver {

    URI resolve(URI shortenedUrl);
}
