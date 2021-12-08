package io.mosip.registration.test.config;

import io.mosip.kernel.clientcrypto.constant.ClientCryptoErrorConstants;
import io.mosip.kernel.clientcrypto.exception.ClientCryptoException;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;

import javax.crypto.SecretKey;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class TestClientCryptoServiceImpl implements ClientCryptoService {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_LENGTH = 2048;
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    protected CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

    public TestClientCryptoServiceImpl(CryptoCoreSpec cryptoCore) {
        this.cryptoCore = cryptoCore;
    }

    @Override
    public byte[] signData(@NotNull byte[] dataToSign) throws ClientCryptoException {
        try {
            Signature sign = Signature.getInstance(SIGN_ALGORITHM);
            sign.initSign(getPrivateKey());

            try(ByteArrayInputStream in = new ByteArrayInputStream(dataToSign)) {
                byte[] buffer = new byte[2048];
                int len = 0;
                while((len = in.read(buffer)) != -1) {
                    sign.update(buffer, 0, len);
                }
                return sign.sign();
            }
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    @Override
    public boolean validateSignature(@NotNull byte[] signature, @NotNull byte[] actualData) throws ClientCryptoException {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(getPublicKey().getEncoded());
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            return validateSignature(kf.generatePublic(keySpec), signature, actualData);
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    @Override
    public byte[] asymmetricEncrypt(@NotNull byte[] plainData) throws ClientCryptoException {
        try {
            return cryptoCore.asymmetricEncrypt(getPublicKey(), plainData);
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    @Override
    public byte[] asymmetricDecrypt(@NotNull byte[] cipher) throws ClientCryptoException {
        try {
            return cryptoCore.asymmetricDecrypt(getPrivateKey(), cipher);
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    @Override
    public byte[] getSigningPublicPart() {
        try {
            return getPublicKey().getEncoded();
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    @Override
    public void closeSecurityInstance() throws ClientCryptoException {

    }

    @Override
    public boolean isTPMInstance() {
        return false;
    }

    @Override
    public byte[] getEncryptionPublicPart() {
        try {
            return getPublicKey().getEncoded();
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }

    private PrivateKey getPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream key = this.getClass().getResourceAsStream("private.key");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(keySpec);
    }

    private PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream key = this.getClass().getResourceAsStream("public.key");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.readAllBytes());
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(keySpec);
    }

    private static boolean validateSignature(PublicKey publicKey, byte[] signature, byte[] actualData)
            throws ClientCryptoException {
        try {
            Signature sign = Signature.getInstance(SIGN_ALGORITHM);
            sign.initVerify(publicKey);

            try(ByteArrayInputStream in = new ByteArrayInputStream(actualData)) {
                byte[] buffer = new byte[2048];
                int len = 0;

                while((len = in.read(buffer)) != -1) {
                    sign.update(buffer, 0, len);
                }
                return sign.verify(signature);
            }
        } catch (Exception ex) {
            throw new ClientCryptoException(ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorCode(),
                    ClientCryptoErrorConstants.CRYPTO_FAILED.getErrorMessage(), ex);
        }
    }
}
