package com.qar.securitysystem.service;

import com.qar.securitysystem.abe.AttributeAuthorityService;
import com.qar.securitysystem.abe.FileKeyEnvelopeService;
import com.qar.securitysystem.abe.lattice.LatticeAuthorityKeyService;
import com.qar.securitysystem.abe.lattice.LatticeCryptoSupport;
import com.qar.securitysystem.abe.lattice.LatticeUserSecretKeyService;
import com.qar.securitysystem.dto.LabeAuthorityResponse;
import com.qar.securitysystem.dto.LabeOverviewResponse;
import com.qar.securitysystem.dto.LabePersonResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LabeAdminService {
    private final FileRecordRepository fileRecordRepository;
    private final PersonRecordRepository personRecordRepository;
    private final UserRepository userRepository;
    private final FileKeyEnvelopeService fileKeyEnvelopeService;
    private final AttributeAuthorityService attributeAuthorityService;
    private final LatticeAuthorityKeyService latticeAuthorityKeyService;
    private final LatticeUserSecretKeyService latticeUserSecretKeyService;
    private final LatticeCryptoSupport latticeCryptoSupport;

    public LabeAdminService(FileRecordRepository fileRecordRepository,
                            PersonRecordRepository personRecordRepository,
                            UserRepository userRepository,
                            FileKeyEnvelopeService fileKeyEnvelopeService,
                            AttributeAuthorityService attributeAuthorityService,
                            LatticeAuthorityKeyService latticeAuthorityKeyService,
                            LatticeUserSecretKeyService latticeUserSecretKeyService,
                            LatticeCryptoSupport latticeCryptoSupport) {
        this.fileRecordRepository = fileRecordRepository;
        this.personRecordRepository = personRecordRepository;
        this.userRepository = userRepository;
        this.fileKeyEnvelopeService = fileKeyEnvelopeService;
        this.attributeAuthorityService = attributeAuthorityService;
        this.latticeAuthorityKeyService = latticeAuthorityKeyService;
        this.latticeUserSecretKeyService = latticeUserSecretKeyService;
        this.latticeCryptoSupport = latticeCryptoSupport;
    }

    public LabeOverviewResponse getOverview() {
        List<FileRecordEntity> files = fileRecordRepository.findAll();
        long latticeFiles = files.stream().filter(file -> fileKeyEnvelopeService.isLatticeEnvelope(file.getWrappedKey())).count();
        long prototypeFiles = files.stream().filter(file -> {
            String wrappedKey = file.getWrappedKey();
            return wrappedKey != null
                    && wrappedKey.startsWith(FileKeyEnvelopeService.LABE_PREFIX)
                    && !fileKeyEnvelopeService.isLatticeEnvelope(wrappedKey);
        }).count();
        long legacyFiles = files.size() - latticeFiles - prototypeFiles;

        LabeOverviewResponse response = new LabeOverviewResponse();
        response.setTotalFiles(files.size());
        response.setLatticeFiles(latticeFiles);
        response.setPrototypeFiles(prototypeFiles);
        response.setLegacyFiles(Math.max(legacyFiles, 0L));
        response.setTotalPersons(personRecordRepository.count());
        response.setTotalUsers(userRepository.count());
        response.setUserSecretBundles(latticeUserSecretKeyService.countExistingBundles());
        response.setAuthorityCount(latticeAuthorityKeyService.getAllAuthorities().size());
        return response;
    }

    public List<LabeAuthorityResponse> listAuthorities() {
        return latticeAuthorityKeyService.getAllAuthorities().values().stream()
                .map(material -> {
                    LabeAuthorityResponse response = new LabeAuthorityResponse();
                    response.setAuthorityId(material.authorityId);
                    response.setAlgorithm(material.algorithm);
                    response.setAttributeScopes(scopesForAuthority(material.authorityId));
                    response.setPublicKeyFingerprint(latticeCryptoSupport.fingerprint(decodeBase64(material.publicKey)));
                    response.setActive(true);
                    return response;
                })
                .sorted(Comparator.comparing(LabeAuthorityResponse::getAuthorityId))
                .toList();
    }

    public List<LabePersonResponse> listPersonViews() {
        return personRecordRepository.findAll().stream()
                .sorted(Comparator.comparing(PersonRecordEntity::getPersonNo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toPersonResponse)
                .toList();
    }

    private LabePersonResponse toPersonResponse(PersonRecordEntity person) {
        UserEntity linkedUser = userRepository.findByPersonId(person.getId()).orElse(null);
        LatticeUserSecretKeyService.UserSecretBundle bundle = linkedUser == null ? null : latticeUserSecretKeyService.loadIfExists(linkedUser.getId());
        Set<String> attrs = attributeAuthorityService.resolvePersonAttributes(person);
        ArrayList<String> attributes = new ArrayList<>(attrs);
        attributes.sort(String::compareToIgnoreCase);
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        for (String attribute : attributes) {
            authorities.add(latticeAuthorityKeyService.resolveAuthorityForAttribute(attribute));
        }

        LabePersonResponse response = new LabePersonResponse();
        response.setId(person.getId());
        response.setUserId(linkedUser == null ? null : linkedUser.getId());
        response.setPersonNo(person.getPersonNo());
        response.setFullName(person.getFullName());
        response.setDepartment(person.getDepartment());
        response.setAirline(person.getAirline());
        response.setPositionTitle(person.getPositionTitle());
        response.setPersonCategory(person.getPersonCategory());
        response.setDutyDomain(person.getDutyDomain());
        response.setFleetGroup(person.getFleetGroup());
        response.setClearanceLevel(person.getClearanceLevel());
        response.setPhone(person.getPhone());
        response.setCreatedAt(person.getCreatedAt() == null ? null : person.getCreatedAt().toString());
        response.setAccountReady(linkedUser != null);
        boolean accessEnabled = LatticeUserSecretKeyService.isUserAccessEnabled(linkedUser);
        response.setAccessEnabled(accessEnabled);
        response.setAccessStatus(accessEnabled ? "active" : "frozen");
        response.setAccessRevokedAt(linkedUser == null || linkedUser.getAccessRevokedAt() == null ? null : linkedUser.getAccessRevokedAt().toString());
        response.setAccessRevokedReason(linkedUser == null ? null : linkedUser.getAccessRevokedReason());
        response.setSecretBundleReady(bundle != null);
        response.setSecretBundleVersion(bundle == null ? null : bundle.bundleVersion);
        response.setSecretBundleStatus(bundle == null ? null : bundle.status);
        response.setSecretBundleIssuedAt(bundle == null || bundle.issuedAt == null ? null : bundle.issuedAt.toString());
        response.setSecretBundleIssuedReason(bundle == null ? null : bundle.issuedReason);
        response.setSecretBundleAttributeDigest(bundle == null ? null : bundle.attributeDigest);
        response.setAttributes(attributes);
        response.setAuthorities(new ArrayList<>(authorities));
        return response;
    }

    private static List<String> scopesForAuthority(String authorityId) {
        String id = authorityId == null ? "" : authorityId.toLowerCase(Locale.ROOT);
        return switch (id) {
            case "person" -> List.of("personNo", "personId", "fullName", "account", "userId");
            case "org" -> List.of("department", "airline", "fleetGroup");
            case "ops" -> List.of("position", "positionTitle", "personCategory", "dutyDomain");
            case "security" -> List.of("clearanceLevel");
            default -> List.of("role", "system", "purpose");
        };
    }

    private static byte[] decodeBase64(String value) {
        try {
            return java.util.Base64.getDecoder().decode(value);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
