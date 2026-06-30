package com.qar.securitysystem.abe.lattice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qar.securitysystem.model.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LatticeAbeService {
    public static final String LATTICE_PREFIX = "LABE_LATTICE_BC:";
    private static final HttpClient DEBUG_HTTP = HttpClient.newHttpClient();

    private final LatticePolicyParser policyParser;
    private final LatticeCryptoSupport cryptoSupport;
    private final LatticeAuthorityKeyService authorityKeyService;
    private final LatticeUserSecretKeyService userSecretKeyService;
    private final ObjectMapper objectMapper;

    public LatticeAbeService(LatticePolicyParser policyParser, LatticeCryptoSupport cryptoSupport, LatticeAuthorityKeyService authorityKeyService, LatticeUserSecretKeyService userSecretKeyService, ObjectMapper objectMapper) {
        this.policyParser = policyParser;
        this.cryptoSupport = cryptoSupport;
        this.authorityKeyService = authorityKeyService;
        this.userSecretKeyService = userSecretKeyService;
        this.objectMapper = objectMapper;
    }

    public String wrap(byte[] fileKey, String policy) {
        try {
            LatticePolicyNode root = policyParser.parse(policy);
            if (root == null) {
                throw new IllegalArgumentException("empty_lattice_policy");
            }
            byte[] rootSecret = cryptoSupport.randomBytes(32);
            Envelope envelope = new Envelope();
            envelope.version = 1;
            envelope.scheme = "multi-authority-lattice-cpabe-prototype-kyber768-v1";
            envelope.policy = policy == null ? "" : policy.trim();
            envelope.policyTree = root;
            envelope.rootDigest = b64(cryptoSupport.digest(rootSecret, aad(policy)));
            envelope.wrappedFileKey = b64(cryptoSupport.encryptSecretWithAad(rootSecret, fileKey, aad(policy)));
            envelope.leaves = new ArrayList<>();
            share(root, rootSecret, envelope.leaves);
            // #region debug-point C:wrap-envelope
            debugReport("C", "LatticeAbeService:wrap",
                    "[DEBUG] wrapped lattice file key",
                    Map.of(
                            "policy", envelope.policy,
                            "rootDigest", envelope.rootDigest,
                            "leafCount", envelope.leaves.size(),
                            "leafSummary", summarizeLeaves(envelope.leaves)
                    ));
            // #endregion
            return LATTICE_PREFIX + b64(objectMapper.writeValueAsBytes(envelope));
        } catch (Exception e) {
            // #region debug-point C:wrap-error
            debugReport("C", "LatticeAbeService:wrap:error",
                    "[DEBUG] failed to wrap lattice file key",
                    Map.of("policy", policy == null ? "" : policy.trim(), "error", e.toString()));
            // #endregion
            throw new RuntimeException("failed_to_wrap_lattice_key", e);
        }
    }

    public byte[] unwrapForUser(String wrappedKey, String policy, UserEntity user) {
        try {
            Envelope envelope = decode(wrappedKey);
            LatticeUserSecretKeyService.UserSecretBundle bundle = userSecretKeyService.getOrCreate(user);
            Set<String> attrs = bundle.attributes;
            Map<String, LatticeUserSecretKeyService.AttributeSecretKey> keys = bundle.attributeKeys;
            byte[] rootSecret = recoverSecret(envelope.policyTree, attrs, keys, indexLeaves(envelope.leaves));
            // #region debug-point D:unwrap-user
            debugReport("D", "LatticeAbeService:unwrapForUser",
                    "[DEBUG] unwrap lattice file key for user",
                    Map.of(
                            "userId", user == null ? null : user.getId(),
                            "policy", policy == null ? envelope.policy : policy,
                            "attributeCount", attrs == null ? 0 : attrs.size(),
                            "leafSummary", summarizeLeaves(envelope.leaves),
                            "rootRecovered", rootSecret != null
                    ));
            // #endregion
            if (rootSecret == null) {
                throw new AccessDeniedException("lattice_policy_not_satisfied");
            }
            byte[] expectedDigest = cryptoSupport.digest(rootSecret, aad(policy == null ? envelope.policy : policy));
            if (!constantTimeEquals(expectedDigest, b64d(envelope.rootDigest))) {
                throw new AccessDeniedException("lattice_root_secret_mismatch");
            }
            return cryptoSupport.decryptSecretWithAad(rootSecret, b64d(envelope.wrappedFileKey), aad(policy == null ? envelope.policy : policy));
        } catch (AccessDeniedException e) {
            // #region debug-point D:unwrap-user-denied
            debugReport("D", "LatticeAbeService:unwrapForUser:denied",
                    "[DEBUG] unwrap lattice file key denied",
                    Map.of("userId", user == null ? null : user.getId(), "error", e.toString(), "policy", policy));
            // #endregion
            throw e;
        } catch (Exception e) {
            // #region debug-point D:unwrap-user-error
            debugReport("D", "LatticeAbeService:unwrapForUser:error",
                    "[DEBUG] unwrap lattice file key failed for user",
                    Map.of("userId", user == null ? null : user.getId(), "error", e.toString(), "policy", policy));
            // #endregion
            throw new RuntimeException("failed_to_unwrap_lattice_key", e);
        }
    }

    public byte[] unwrapForSystem(String wrappedKey, String policy) {
        try {
            Envelope envelope = decode(wrappedKey);
            Map<Integer, LeafCiphertext> leafIndex = indexLeaves(envelope.leaves);
            Map<String, LatticeAuthorityKeyService.AuthorityKeyMaterial> authorities = authorityKeyService.getAllAuthorities();
            byte[] rootSecret = recoverForSystem(envelope.policyTree, leafIndex, authorities);
            // #region debug-point E:unwrap-system
            debugReport("E", "LatticeAbeService:unwrapForSystem",
                    "[DEBUG] unwrap lattice file key for system",
                    Map.of(
                            "policy", policy == null ? envelope.policy : policy,
                            "authorityFingerprints", summarizeAuthorities(authorities),
                            "leafSummary", summarizeLeaves(envelope.leaves),
                            "rootRecovered", rootSecret != null
                    ));
            // #endregion
            if (rootSecret == null) {
                throw new AccessDeniedException("lattice_policy_not_satisfied");
            }
            byte[] expectedDigest = cryptoSupport.digest(rootSecret, aad(policy == null ? envelope.policy : policy));
            if (!constantTimeEquals(expectedDigest, b64d(envelope.rootDigest))) {
                throw new AccessDeniedException("lattice_root_secret_mismatch");
            }
            return cryptoSupport.decryptSecretWithAad(rootSecret, b64d(envelope.wrappedFileKey), aad(policy == null ? envelope.policy : policy));
        } catch (AccessDeniedException e) {
            // #region debug-point E:unwrap-system-denied
            debugReport("E", "LatticeAbeService:unwrapForSystem:denied",
                    "[DEBUG] unwrap lattice file key denied for system",
                    Map.of("policy", policy, "error", e.toString()));
            // #endregion
            throw e;
        } catch (Exception e) {
            // #region debug-point E:unwrap-system-error
            debugReport("E", "LatticeAbeService:unwrapForSystem:error",
                    "[DEBUG] unwrap lattice file key failed for system",
                    Map.of("policy", policy, "error", e.toString()));
            // #endregion
            throw new RuntimeException("failed_to_unwrap_lattice_key", e);
        }
    }

    public boolean isLatticeEnvelope(String wrappedKey) {
        return wrappedKey != null && wrappedKey.startsWith(LATTICE_PREFIX);
    }

    private void share(LatticePolicyNode node, byte[] secret, List<LeafCiphertext> leaves) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            leaves.add(createLeaf(node, secret));
            return;
        }
        if (node.getType() == LatticePolicyNode.Type.OR) {
            for (LatticePolicyNode child : node.getChildren()) {
                share(child, secret, leaves);
            }
            return;
        }
        if (node.getChildren().isEmpty()) {
            return;
        }
        List<byte[]> childSecrets = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size() - 1; i++) {
            childSecrets.add(cryptoSupport.randomBytes(secret.length));
        }
        byte[] tail = secret.clone();
        for (byte[] part : childSecrets) {
            tail = cryptoSupport.xor(tail, part);
        }
        childSecrets.add(tail);
        for (int i = 0; i < node.getChildren().size(); i++) {
            share(node.getChildren().get(i), childSecrets.get(i), leaves);
        }
    }

    private LeafCiphertext createLeaf(LatticePolicyNode node, byte[] secret) {
        String authority = authorityKeyService.resolveAuthorityForAttribute(node.getAttribute());
        LatticeAuthorityKeyService.AuthorityKeyMaterial material = authorityKeyService.getAuthorityMaterial(authority);
        LatticeCryptoSupport.KyberEncapsulationResult result = cryptoSupport.encapsulate(b64d(material.publicKey));
        LeafCiphertext leaf = new LeafCiphertext();
        leaf.nodeId = node.getNodeId();
        leaf.attribute = node.getAttribute();
        leaf.authorityId = authority;
        leaf.attributeFingerprint = cryptoSupport.fingerprint(node.getAttribute().getBytes(StandardCharsets.UTF_8));
        leaf.encapsulation = b64(result.encapsulation());
        leaf.maskedSecret = b64(cryptoSupport.xor(secret, crop(result.secret(), secret.length)));
        // #region debug-point C:create-leaf
        debugReport("C", "LatticeAbeService:createLeaf",
                "[DEBUG] created lattice leaf",
                Map.of(
                        "nodeId", leaf.nodeId,
                        "attribute", leaf.attribute,
                        "authorityId", leaf.authorityId,
                        "attributeFingerprint", leaf.attributeFingerprint,
                        "authorityPublicKeyFingerprint", cryptoSupport.fingerprint(b64d(material.publicKey))
                ));
        // #endregion
        return leaf;
    }

    private byte[] recoverSecret(LatticePolicyNode node,
                                 Set<String> attrs,
                                 Map<String, LatticeUserSecretKeyService.AttributeSecretKey> keys,
                                 Map<Integer, LeafCiphertext> leaves) {
        if (node == null) {
            return null;
        }
        if (node.isLeaf()) {
            if (attrs == null || !attrs.contains(node.getAttribute())) {
                return null;
            }
            LatticeUserSecretKeyService.AttributeSecretKey key = keys == null ? null : keys.get(node.getAttribute());
            LeafCiphertext leaf = leaves.get(node.getNodeId());
            if (key == null || leaf == null) {
                // #region debug-point D:recover-missing
                debugReport("D", "LatticeAbeService:recoverSecret:missing",
                        "[DEBUG] missing leaf or key while recovering secret",
                        Map.of("attribute", node.getAttribute(), "nodeId", node.getNodeId(), "hasKey", key != null, "hasLeaf", leaf != null));
                // #endregion
                return null;
            }
            byte[] secret = cryptoSupport.decapsulate(b64d(key.privateKey), b64d(leaf.encapsulation));
            // #region debug-point D:recover-leaf
            debugReport("D", "LatticeAbeService:recoverSecret:leaf",
                    "[DEBUG] recovered user leaf secret",
                    Map.of(
                            "attribute", node.getAttribute(),
                            "nodeId", node.getNodeId(),
                            "authorityId", key.authorityId,
                            "bundlePublicKeyFingerprint", cryptoSupport.fingerprint(b64d(key.publicKey)),
                            "bundlePrivateKeyFingerprint", cryptoSupport.fingerprint(b64d(key.privateKey))
                    ));
            // #endregion
            return cryptoSupport.xor(crop(secret, 32), b64d(leaf.maskedSecret));
        }
        if (node.getType() == LatticePolicyNode.Type.OR) {
            for (LatticePolicyNode child : node.getChildren()) {
                byte[] recovered = recoverSecret(child, attrs, keys, leaves);
                if (recovered != null) {
                    return recovered;
                }
            }
            return null;
        }
        byte[] combined = new byte[32];
        boolean any = false;
        for (LatticePolicyNode child : node.getChildren()) {
            byte[] recovered = recoverSecret(child, attrs, keys, leaves);
            if (recovered == null) {
                return null;
            }
            combined = cryptoSupport.xor(combined, recovered);
            any = true;
        }
        return any ? combined : null;
    }

    private byte[] recoverForSystem(LatticePolicyNode node,
                                    Map<Integer, LeafCiphertext> leaves,
                                    Map<String, LatticeAuthorityKeyService.AuthorityKeyMaterial> authorities) {
        if (node == null) {
            return null;
        }
        if (node.isLeaf()) {
            LeafCiphertext leaf = leaves.get(node.getNodeId());
            if (leaf == null) {
                return null;
            }
            LatticeAuthorityKeyService.AuthorityKeyMaterial material = authorities.get(leaf.authorityId);
            if (material == null) {
                return null;
            }
            byte[] secret = cryptoSupport.decapsulate(b64d(material.privateKey), b64d(leaf.encapsulation));
            // #region debug-point E:recover-leaf-system
            debugReport("E", "LatticeAbeService:recoverForSystem:leaf",
                    "[DEBUG] recovered system leaf secret",
                    Map.of(
                            "attribute", leaf.attribute,
                            "nodeId", leaf.nodeId,
                            "authorityId", leaf.authorityId,
                            "authorityPublicKeyFingerprint", cryptoSupport.fingerprint(b64d(material.publicKey)),
                            "authorityPrivateKeyFingerprint", cryptoSupport.fingerprint(b64d(material.privateKey))
                    ));
            // #endregion
            return cryptoSupport.xor(crop(secret, 32), b64d(leaf.maskedSecret));
        }
        if (node.getType() == LatticePolicyNode.Type.OR) {
            for (LatticePolicyNode child : node.getChildren()) {
                byte[] recovered = recoverForSystem(child, leaves, authorities);
                if (recovered != null) {
                    return recovered;
                }
            }
            return null;
        }
        byte[] combined = new byte[32];
        boolean any = false;
        for (LatticePolicyNode child : node.getChildren()) {
            byte[] recovered = recoverForSystem(child, leaves, authorities);
            if (recovered == null) {
                return null;
            }
            combined = cryptoSupport.xor(combined, recovered);
            any = true;
        }
        return any ? combined : null;
    }

    private static Map<Integer, LeafCiphertext> indexLeaves(List<LeafCiphertext> leaves) {
        LinkedHashMap<Integer, LeafCiphertext> map = new LinkedHashMap<>();
        if (leaves == null) {
            return map;
        }
        for (LeafCiphertext leaf : leaves) {
            map.put(leaf.nodeId, leaf);
        }
        return map;
    }

    private Envelope decode(String wrappedKey) throws Exception {
        String raw = wrappedKey.substring(LATTICE_PREFIX.length()).trim();
        return objectMapper.readValue(Base64.getDecoder().decode(raw), Envelope.class);
    }

    private static byte[] aad(String policy) {
        return ("LATTICE-LABE:" + (policy == null ? "" : policy.trim())).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] crop(byte[] input, int size) {
        byte[] out = new byte[size];
        System.arraycopy(input, 0, out, 0, Math.min(size, input.length));
        return out;
    }

    private static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static byte[] b64d(String value) {
        return Base64.getDecoder().decode(value);
    }

    private static boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i++) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }

    public static class Envelope {
        public int version;
        public String scheme;
        public String policy;
        public String rootDigest;
        public String wrappedFileKey;
        public LatticePolicyNode policyTree;
        public List<LeafCiphertext> leaves;
    }

    public static class LeafCiphertext {
        public int nodeId;
        public String attribute;
        public String attributeFingerprint;
        public String authorityId;
        public String encapsulation;
        public String maskedSecret;
    }

    private Map<String, Object> summarizeLeaves(List<LeafCiphertext> leaves) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        if (leaves == null) {
            return summary;
        }
        for (LeafCiphertext leaf : leaves) {
            summary.put(String.valueOf(leaf.nodeId), Map.of(
                    "attribute", leaf.attribute,
                    "authorityId", leaf.authorityId,
                    "attributeFingerprint", leaf.attributeFingerprint
            ));
        }
        return summary;
    }

    private Map<String, String> summarizeAuthorities(Map<String, LatticeAuthorityKeyService.AuthorityKeyMaterial> authorities) {
        LinkedHashMap<String, String> summary = new LinkedHashMap<>();
        if (authorities == null) {
            return summary;
        }
        for (Map.Entry<String, LatticeAuthorityKeyService.AuthorityKeyMaterial> entry : authorities.entrySet()) {
            LatticeAuthorityKeyService.AuthorityKeyMaterial material = entry.getValue();
            summary.put(entry.getKey(),
                    cryptoSupport.fingerprint(b64d(material.publicKey)) + ":" + cryptoSupport.fingerprint(b64d(material.privateKey)));
        }
        return summary;
    }

    private static void debugReport(String hypothesisId, String location, String msg, Map<String, Object> data) {
        try {
            Path envPath = Path.of(".dbg", "lattice-unwrap.env");
            String url = "http://127.0.0.1:7777/event";
            String sessionId = "lattice-unwrap";
            if (Files.exists(envPath)) {
                String env = Files.readString(envPath, StandardCharsets.UTF_8);
                for (String line : env.split("\\R")) {
                    if (line.startsWith("DEBUG_SERVER_URL=")) {
                        url = line.substring("DEBUG_SERVER_URL=".length()).trim();
                    } else if (line.startsWith("DEBUG_SESSION_ID=")) {
                        sessionId = line.substring("DEBUG_SESSION_ID=".length()).trim();
                    }
                }
            }
            String payload = new ObjectMapper().writeValueAsString(Map.of(
                    "sessionId", sessionId,
                    "runId", "pre-fix",
                    "hypothesisId", hypothesisId,
                    "location", location,
                    "msg", msg,
                    "data", data,
                    "ts", System.currentTimeMillis()
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            DEBUG_HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
