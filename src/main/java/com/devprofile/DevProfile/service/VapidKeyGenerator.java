package com.devprofile.DevProfile.service;


import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.UrlBase64;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;

@Service
public class VapidKeyGenerator {

    public Pair<String, String> generateVapidKeys() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyGen.initialize(256, new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] privateKeyEncoded = privateKey.getEncoded();

        String publicKeyBase64 = new String(UrlBase64.encode(publicKeyEncoded), StandardCharsets.UTF_8);
        String privateKeyBase64 = new String(UrlBase64.encode(privateKeyEncoded), StandardCharsets.UTF_8);

        return Pair.of(publicKeyBase64, privateKeyBase64);
    }
}