package burp.utils;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.testing.VulnerabilityTester.InjectionPoint;

public class RequestModifier {
    
    public static HttpRequest injectPayload(HttpRequest originalRequest,
                                           InjectionPoint injectionPoint,
                                           String payload) {
        HttpRequest modified = originalRequest;
        
        switch (injectionPoint.getType()) {
            case PARAMETER -> {
                // Replace parameter value
                modified = modified.withRemovedParameters(
                    HttpParameter.parameter(injectionPoint.getName(), 
                        injectionPoint.getOriginalValue())
                );
                modified = modified.withAddedParameters(
                    HttpParameter.parameter(injectionPoint.getName(), payload)
                );
            }
            case HEADER -> {
                // Replace header value
                modified = modified.withRemovedHeader(injectionPoint.getName());
                modified = modified.withAddedHeader(injectionPoint.getName(), payload);
            }
            case BODY -> {
                // Replace body content
                String originalBody = originalRequest.bodyToString();
                String newBody = originalBody.replace(
                    injectionPoint.getOriginalValue(), 
                    payload
                );
                modified = modified.withBody(newBody);
            }
            case COOKIE -> {
                // Replace cookie value
                String cookieHeader = originalRequest.headerValue("Cookie");
                if (cookieHeader != null) {
                    String newCookie = cookieHeader.replaceAll(
                        injectionPoint.getName() + "=[^;]*",
                        injectionPoint.getName() + "=" + payload
                    );
                    modified = modified.withRemovedHeader("Cookie");
                    modified = modified.withAddedHeader("Cookie", newCookie);
                }
            }
            case PATH -> {
                // Replace path segment
                String url = originalRequest.url();
                String newUrl = url.replace(injectionPoint.getOriginalValue(), payload);
                modified = modified.withPath(newUrl);
            }
        }
        
        return modified;
    }
    
    public static HttpRequest addHeader(HttpRequest request, String name, String value) {
        return request.withAddedHeader(name, value);
    }
    
    public static HttpRequest removeHeader(HttpRequest request, String name) {
        return request.withRemovedHeader(name);
    }
}
