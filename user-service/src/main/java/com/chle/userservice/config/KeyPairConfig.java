package com.chle.userservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.security.*;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class KeyPairConfig {

    @Bean
    public KeyPair keyPair(JwtProperties properties) {
        try (InputStream is = properties.getLocation().getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, properties.getPassword().toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(properties.getAlias(), properties.getPassword().toCharArray());
            PublicKey publicKey = keyStore.getCertificate(properties.getAlias()).getPublicKey();

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load KeyPair", e);
        }
    }
}