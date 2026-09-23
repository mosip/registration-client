/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher.common;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SignatureVerifierTest {

    private static KeyPair keyPair;
    private static final byte[] DATA = "manifest-content-to-sign".getBytes(StandardCharsets.UTF_8);

    @BeforeClass
    public static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    private static byte[] sign(byte[] data, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(data);
        return signer.sign();
    }

    @Test
    public void verify_validSignature_true() throws Exception {
        byte[] signature = sign(DATA, keyPair.getPrivate());
        assertTrue(SignatureVerifier.verify(DATA, signature, keyPair.getPublic()));
    }

    @Test
    public void verify_tamperedData_false() throws Exception {
        byte[] signature = sign(DATA, keyPair.getPrivate());
        byte[] tampered = "manifest-content-to-sign!".getBytes(StandardCharsets.UTF_8);
        assertFalse(SignatureVerifier.verify(tampered, signature, keyPair.getPublic()));
    }

    @Test
    public void verify_tamperedSignature_false() throws Exception {
        byte[] signature = sign(DATA, keyPair.getPrivate());
        signature[signature.length - 1] ^= 0x01;
        assertFalse(SignatureVerifier.verify(DATA, signature, keyPair.getPublic()));
    }

    @Test
    public void verify_wrongKey_false() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PublicKey otherKey = generator.generateKeyPair().getPublic();

        byte[] signature = sign(DATA, keyPair.getPrivate());
        assertFalse(SignatureVerifier.verify(DATA, signature, otherKey));
    }

    @Test
    public void loadPublicKeyFromCertificate_providerPem_returnsKey() throws Exception {
        try (InputStream pem = getClass().getClassLoader().getResourceAsStream("provider.pem")) {
            assertNotNull("provider.pem must be on the test classpath", pem);
            PublicKey publicKey = SignatureVerifier.loadPublicKeyFromCertificate(pem);
            assertNotNull(publicKey);
        }
    }
}
