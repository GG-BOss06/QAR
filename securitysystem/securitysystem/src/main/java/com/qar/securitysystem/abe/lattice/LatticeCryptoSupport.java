package com.qar.securitysystem.abe.lattice;

import com.qar.securitysystem.util.AesGcmUtil;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMExtractor;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.kyber.KyberPublicKeyParameters;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

@Component
public class LatticeCryptoSupport {
    private static final SecureRandom RNG = new SecureRandom();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public KyberKeyPair generateKeyPair() {
        KyberKeyPairGenerator generator = new KyberKeyPairGenerator();
        generator.init(new KyberKeyGenerationParameters(RNG, KyberParameters.kyber768));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        return new KyberKeyPair(
                (KyberPublicKeyParameters) pair.getPublic(),
                (KyberPrivateKeyParameters) pair.getPrivate()
        );
    }

    public KyberEncapsulationResult encapsulate(byte[] publicKeyBytes) {
        KyberPublicKeyParameters publicKey = new KyberPublicKeyParameters(KyberParameters.kyber768, publicKeyBytes);
        SecretWithEncapsulation wrapped = new KyberKEMGenerator(RNG).generateEncapsulated(publicKey);
        try {
            return new KyberEncapsulationResult(wrapped.getEncapsulation(), wrapped.getSecret());
        } finally {
            destroyQuietly(wrapped);
        }
    }

    public byte[] decapsulate(byte[] privateKeyBytes, byte[] encapsulation) {
        KyberPrivateKeyParameters privateKey = new KyberPrivateKeyParameters(KyberParameters.kyber768, privateKeyBytes);
        return new KyberKEMExtractor(privateKey).extractSecret(encapsulation);
    }

    public byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        RNG.nextBytes(data);
        return data;
    }

    public byte[] xor(byte[] left, byte[] right) {
        if (left == null || right == null || left.length != right.length) {
            throw new IllegalArgumentException("invalid_xor_inputs");
        }
        byte[] out = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            out[i] = (byte) (left[i] ^ right[i]);
        }
        return out;
    }

    public byte[] digest(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
            for (byte[] part : parts) {
                if (part != null) {
                    digest.update(part);
                }
            }
            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException("lattice_digest_failed", e);
        }
    }

    public String fingerprint(byte[] value) {
        if (value == null) {
            return "";
        }
        byte[] hash = digest(value);
        int size = Math.min(16, hash.length);
        return Hex.toHexString(Arrays.copyOf(hash, size));
    }

    public byte[] encryptSecretWithAad(byte[] secret, byte[] payload, byte[] aad) {
        SecretKey key = new SecretKeySpec(secret, 0, 32, "AES");
        byte[] iv = AesGcmUtil.newIv();
        byte[] ciphertext = AesGcmUtil.encrypt(key, iv, payload, aad);
        byte[] out = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
        return out;
    }

    public byte[] decryptSecretWithAad(byte[] secret, byte[] payload, byte[] aad) {
        if (payload == null || payload.length < 12) {
            throw new IllegalArgumentException("invalid_wrapped_payload");
        }
        SecretKey key = new SecretKeySpec(secret, 0, 32, "AES");
        byte[] iv = Arrays.copyOfRange(payload, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(payload, 12, payload.length);
        return AesGcmUtil.decrypt(key, iv, ciphertext, aad);
    }

    private static void destroyQuietly(SecretWithEncapsulation wrapped) {
        try {
            wrapped.destroy();
        } catch (Exception ignored) {
        }
    }

    public record KyberKeyPair(KyberPublicKeyParameters publicKey, KyberPrivateKeyParameters privateKey) {
    }

    public record KyberEncapsulationResult(byte[] encapsulation, byte[] secret) {
    }
}
