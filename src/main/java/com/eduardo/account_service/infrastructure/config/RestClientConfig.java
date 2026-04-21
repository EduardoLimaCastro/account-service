package com.eduardo.account_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String CLIENT_REGISTRATION_ID = "account-service";

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean("userRestClient")
    public RestClient userRestClient(
            @Value("${clients.user-service.base-url}") String baseUrl,
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestInterceptor(clientCredentialsInterceptor(authorizedClientManager))
                .build();
    }

    @Bean("agencyRestClient")
    public RestClient agencyRestClient(
            @Value("${clients.agency-service.base-url}") String baseUrl,
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestInterceptor(clientCredentialsInterceptor(authorizedClientManager))
                .build();
    }

    private ClientHttpRequestInterceptor clientCredentialsInterceptor(
            OAuth2AuthorizedClientManager manager) {
        return (request, body, execution) -> {
            var authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                    .principal(CLIENT_REGISTRATION_ID)
                    .build();
            var authorizedClient = manager.authorize(authorizeRequest);
            if (authorizedClient != null) {
                request.getHeaders().setBearerAuth(
                        authorizedClient.getAccessToken().getTokenValue());
            }
            return execution.execute(request, body);
        };
    }
}
