package com.familywishes.dto;

import java.io.Serializable;

/** An email's image with its media type. Serializable because it is kept in the emailImages cache. */
public record EmailImage(byte[] data, String contentType) implements Serializable {}
