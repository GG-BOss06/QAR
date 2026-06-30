package com.qar.securitysystem;

import com.qar.securitysystem.abe.AccessPurpose;
import com.qar.securitysystem.abe.lattice.LatticeUserSecretKeyService;
import com.qar.securitysystem.dto.EncryptedFileUploadRequest;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.model.AccountRequestEntity;
import com.qar.securitysystem.model.AccountRequestStatus;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.model.UserRole;
import com.qar.securitysystem.repo.AccountRequestRepository;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.service.AdminService;
import com.qar.securitysystem.service.FileService;
import com.qar.securitysystem.util.IdUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;

@SpringBootTest(properties = {
        "app.person.seedEnabled=false",
        "app.crypto.lattice-user-dir=target/test-data/lattice-users-lifecycle"
})
class LatticeKeyLifecycleTests {
    private static final Path LATTICE_USER_DIR = Path.of("target", "test-data", "lattice-users-lifecycle");

    @Autowired
    private AdminService adminService;

    @Autowired
    private PersonRecordRepository personRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRequestRepository accountRequestRepository;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private LatticeUserSecretKeyService latticeUserSecretKeyService;

    @Autowired
    private FileService fileService;

    @BeforeEach
    void setUp() throws IOException {
        accountRequestRepository.deleteAll();
        userRepository.deleteAll();
        fileRecordRepository.deleteAll();
        personRecordRepository.deleteAll();
        deleteDirectory(LATTICE_USER_DIR);
    }

    @Test
    void approveAccountRequestIssuesBundleImmediately() {
        PersonRecordEntity person = savePerson("20269991", "审批发钥用户", "飞行一部");
        AccountRequestEntity request = new AccountRequestEntity();
        request.setId(IdUtil.newId());
        request.setPersonNo(person.getPersonNo());
        request.setFullName(person.getFullName());
        request.setAirline(person.getAirline());
        request.setPositionTitle(person.getPositionTitle());
        request.setDepartment(person.getDepartment());
        request.setContact(person.getPhone());
        request.setIdLast4(person.getIdLast4());
        request.setPasswordHash("hashed-password");
        request.setStatus(AccountRequestStatus.PENDING);
        request.setCreatedAt(Instant.now());
        accountRequestRepository.save(request);

        adminService.approveAccountRequest(request.getId(), "admin-id", "ok");

        UserEntity user = userRepository.findByAccount(person.getPersonNo()).orElseThrow();
        LatticeUserSecretKeyService.UserSecretBundle bundle = latticeUserSecretKeyService.loadIfExists(user.getId());

        Assertions.assertNotNull(bundle);
        Assertions.assertEquals(1L, bundle.bundleVersion);
        Assertions.assertEquals("active", bundle.status);
        Assertions.assertEquals("account_approved", bundle.issuedReason);
        Assertions.assertTrue(bundle.attributes.contains("personno:" + person.getPersonNo().toLowerCase()));
    }

    @Test
    void updatePolicyAttributesRotatesBundleAndArchivesPreviousVersion() {
        PersonRecordEntity person = savePerson("20269992", "轮换用户", "飞行一部");
        UserEntity user = saveUser(person);
        LatticeUserSecretKeyService.UserSecretBundle initial = latticeUserSecretKeyService.issueForUser(user, "seed");

        PersonRecordEntity update = new PersonRecordEntity();
        update.setDepartment("运行控制部");
        adminService.updatePerson(person.getId(), update);

        LatticeUserSecretKeyService.UserSecretBundle current = latticeUserSecretKeyService.loadIfExists(user.getId());

        Assertions.assertNotNull(current);
        Assertions.assertEquals(2L, current.bundleVersion);
        Assertions.assertEquals("person_attributes_updated", current.issuedReason);
        Assertions.assertTrue(current.attributes.contains("department:运行控制部".toLowerCase()));
        Assertions.assertFalse(current.attributes.contains("department:飞行一部".toLowerCase()));
        Assertions.assertTrue(Files.exists(LATTICE_USER_DIR.resolve("history").resolve(user.getId() + ".v" + initial.bundleVersion + ".json")));
    }

    @Test
    void updateNonPolicyFieldDoesNotRotateBundle() {
        PersonRecordEntity person = savePerson("20269993", "电话变更用户", "飞行一部");
        UserEntity user = saveUser(person);
        latticeUserSecretKeyService.issueForUser(user, "seed");

        PersonRecordEntity update = new PersonRecordEntity();
        update.setPhone("13911112222");
        adminService.updatePerson(person.getId(), update);

        LatticeUserSecretKeyService.UserSecretBundle current = latticeUserSecretKeyService.loadIfExists(user.getId());

        Assertions.assertNotNull(current);
        Assertions.assertEquals(1L, current.bundleVersion);
        Assertions.assertEquals("seed", current.issuedReason);
    }

    @Test
    void freezeAndRestoreAccessRevokesBundleAndRestoresFileAccess() {
        PersonRecordEntity person = savePerson("20269994", "冻结恢复用户", "飞行一部");
        UserEntity user = saveUser(person);
        latticeUserSecretKeyService.issueForUser(user, "seed");

        EncryptedFileUploadRequest req = new EncryptedFileUploadRequest();
        req.setEncryptedData(Base64.getEncoder().encodeToString("freeze-access".getBytes()));
        req.setOriginalName("freeze.txt");
        req.setContentType("text/plain");
        req.setSizeBytes(13L);
        req.setPolicy("personNo:" + person.getPersonNo());
        FileRecordResponse saved = fileService.storeEncrypted(user, req);
        FileRecordEntity record = fileRecordRepository.findById(saved.getId()).orElseThrow();

        Assertions.assertTrue(fileService.canUserAccess(record, user));

        adminService.freezeLatticeAccessForPerson(person.getId(), "manual_freeze");

        UserEntity frozenUser = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertFalse(Boolean.TRUE.equals(frozenUser.getAccessEnabled()));
        Assertions.assertNull(latticeUserSecretKeyService.loadIfExists(user.getId()));
        Assertions.assertTrue(Files.exists(LATTICE_USER_DIR.resolve("history").resolve(user.getId() + ".v1.json")));
        Assertions.assertFalse(fileService.canUserAccess(record, frozenUser));

        LatticeUserSecretKeyService.UserSecretBundle restored = adminService.restoreLatticeAccessForPerson(person.getId(), "manual_restore");
        UserEntity restoredUser = userRepository.findById(user.getId()).orElseThrow();

        Assertions.assertTrue(Boolean.TRUE.equals(restoredUser.getAccessEnabled()));
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(2L, restored.bundleVersion);
        Assertions.assertEquals("manual_restore", restored.issuedReason);
        Assertions.assertTrue(fileService.canUserAccess(record, restoredUser));
        Assertions.assertDoesNotThrow(() -> fileService.decryptForUser(record, restoredUser, AccessPurpose.USER_PAYLOAD));
    }

    private PersonRecordEntity savePerson(String personNo, String fullName, String department) {
        PersonRecordEntity person = new PersonRecordEntity();
        person.setId(IdUtil.newId());
        person.setPersonNo(personNo);
        person.setFullName(fullName);
        person.setIdLast4("1234");
        person.setPhone("13800000000");
        person.setDepartment(department);
        person.setAirline("CAUC");
        person.setPositionTitle("机长");
        person.setPersonCategory("飞行机组");
        person.setDutyDomain("飞行运行");
        person.setFleetGroup("CAUC机队");
        person.setClearanceLevel("L1");
        person.setCreatedAt(Instant.now());
        return personRecordRepository.save(person);
    }

    private UserEntity saveUser(PersonRecordEntity person) {
        UserEntity user = new UserEntity();
        user.setId(IdUtil.newId());
        user.setAccount(person.getPersonNo());
        user.setPasswordHash("hashed-password");
        user.setPersonId(person.getId());
        user.setRole(UserRole.USER);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
