package com.relatosdepapel.Gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * This class represents a request that is being processed by the gateway.
 * It contains information about the target HTTP method, query parameters, body, exchange, and headers of the request.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class GatewayRequest {

    /**
     * The target HTTP method of the request.
     */
    private HttpMethod targetMethod;

    /**
     * The query parameters of the request.
     */
    private LinkedMultiValueMap<String, String> queryParams;

    /**
     * The body of the request.
     */
    private Object body;

    /**
     * The current server web exchange. This is ignored when the object is serialized to JSON.
     */
    @JsonIgnore
    private ServerWebExchange exchange;

    /**
     * The headers of the request. This is ignored when the object is serialized to JSON.
     */
    @JsonIgnore
    private HttpHeaders headers;

    public URI GetURI() {
        Object attr = this.exchange.getAttributes()
                .get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        URI baseUri = null;
        if (attr instanceof URI) {
            baseUri = (URI) attr;
        } else if (attr != null) {
            try {
                baseUri = URI.create(String.valueOf(attr));
            } catch (Exception e) {
                log.warn("Could not parse GATEWAY_REQUEST_URL_ATTR value to URI: {}", attr, e);
            }
        }

        if (baseUri == null) {
            // fallback: use the current request URI (defensive)
            baseUri = this.exchange.getRequest().getURI();
            log.debug("GATEWAY_REQUEST_URL_ATTR missing; falling back to request URI: {}", baseUri);
        }

        // queryParams puede ser null, manejarlo
        var qparams = this.queryParams;
        var builder = UriComponentsBuilder.fromUri(baseUri);
        if (qparams != null && !qparams.isEmpty()) {
            builder.queryParams(qparams);
        }
        return builder.build().toUri();
    }
}
