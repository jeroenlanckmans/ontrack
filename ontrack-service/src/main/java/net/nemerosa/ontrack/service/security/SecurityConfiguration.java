package net.nemerosa.ontrack.service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Autowired
    private List<AuthenticationProvider> authenticationProviders;

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProviders);
    }

}
