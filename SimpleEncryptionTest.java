import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class SimpleEncryptionTest {

    public static void main(String[] args) {
        try {
            // 测试数据
            String originalData = "这是一个测试数据，用于验证加密和解密功能";
            System.out.println("原始数据: " + originalData);

            // 使用固定的测试密钥
            byte[] keyBytes = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 加密
            System.out.println("\n=== 加密过程 ===");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] cipherText = cipher.doFinal(originalData.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedDataWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedDataWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedDataWithIv, iv.length, cipherText.length);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedDataWithIv);

            System.out.println("加密后的数据: " + encryptedBase64);

            // 解密
            System.out.println("\n=== 解密过程 ===");
            byte[] encryptedDataWithIvDecoded = Base64.getDecoder().decode(encryptedBase64);
            
            byte[] ivDecoded = new byte[12];
            byte[] cipherTextDecoded = new byte[encryptedDataWithIvDecoded.length - 12];
            System.arraycopy(encryptedDataWithIvDecoded, 0, ivDecoded, 0, ivDecoded.length);
            System.arraycopy(encryptedDataWithIvDecoded, ivDecoded.length, cipherTextDecoded, 0, cipherTextDecoded.length);

            GCMParameterSpec gcmSpecDecrypt = new GCMParameterSpec(128, ivDecoded);
            Cipher cipherDecrypt = Cipher.getInstance("AES/GCM/NoPadding");
            cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, gcmSpecDecrypt);
            byte[] plainText = cipherDecrypt.doFinal(cipherTextDecoded);
            
            String decryptedData = new String(plainText, StandardCharsets.UTF_8);
            System.out.println("解密后的数据: " + decryptedData);

            // 验证结果
            System.out.println("\n=== 验证结果 ===");
            System.out.println("原始数据与解密数据是否匹配: " + originalData.equals(decryptedData));

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
