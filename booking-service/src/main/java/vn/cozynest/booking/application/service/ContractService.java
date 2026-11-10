package vn.cozynest.booking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.model.ContractAmendment;
import vn.cozynest.booking.domain.model.ContractOccupant;
import vn.cozynest.booking.domain.repository.ContractAmendmentRepository;
import vn.cozynest.booking.domain.repository.ContractOccupantRepository;
import vn.cozynest.booking.domain.repository.ContractRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractAmendmentRepository amendmentRepository;
    private final ContractOccupantRepository occupantRepository;

    public ContractService(ContractRepository contractRepository,
                           ContractAmendmentRepository amendmentRepository,
                           ContractOccupantRepository occupantRepository) {
        this.contractRepository = contractRepository;
        this.amendmentRepository = amendmentRepository;
        this.occupantRepository = occupantRepository;
    }

    public ContractDto createContract(CreateContractDto dto) {
        String code = "HD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String terms = dto.terms() != null && !dto.terms().isBlank()
                ? dto.terms()
                : "Hop dong thue nha tieu chuan theo Luat Nha o 2023 va Bo luat Dan su 2015.";

        Contract contract = Contract.create(
                code,
                dto.roomId(),
                dto.tenantId(),
                dto.landlordId(),
                dto.requestId(),
                dto.startDate(),
                dto.endDate(),
                dto.monthlyRent(),
                dto.depositAmount(),
                terms
        );

        Contract saved = contractRepository.save(contract);
        return ContractDto.from(saved);
    }

    public ContractDto signContract(String contractId, String userId, String otpCode, boolean isLandlord) {
        if (otpCode == null || otpCode.length() != 6) {
            throw new IllegalArgumentException("Ma OTP xac thuc khong hop le");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong: " + contractId));

        if (isLandlord) {
            if (!contract.getLandlordId().equals(userId)) {
                throw new IllegalArgumentException("Nguoi dung khong phai chu nha cua hop dong nay");
            }
            contract.signByLandlord();
        } else {
            if (!contract.getTenantId().equals(userId)) {
                throw new IllegalArgumentException("Nguoi dung khong phai nguoi thue cua hop dong nay");
            }
            contract.signByTenant();
        }

        return ContractDto.from(contractRepository.save(contract));
    }

    public ContractOccupantDto addOccupant(String contractId, AddOccupantDto dto) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong: " + contractId));

        ContractOccupant occupant = ContractOccupant.create(
                contractId,
                dto.fullName(),
                dto.phoneNumber(),
                dto.idCardNumber(),
                dto.relationship(),
                dto.primaryTenant()
        );
        return ContractOccupantDto.from(occupantRepository.save(occupant));
    }

    @Transactional(readOnly = true)
    public List<ContractOccupantDto> getOccupants(String contractId) {
        return occupantRepository.findByContractId(contractId).stream().map(ContractOccupantDto::from).toList();
    }

    public void removeOccupant(String contractId, String occupantId) {
        occupantRepository.deleteById(occupantId);
    }

    public ContractAmendmentDto createAmendment(String contractId, CreateAmendmentDto dto) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong: " + contractId));

        String code = "PL-" + contract.getContractCode() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        ContractAmendment amendment = ContractAmendment.create(
                contractId,
                code,
                dto.amendmentType(),
                dto.newRent(),
                dto.newEndDate(),
                dto.effectiveDate(),
                dto.reason()
        );

        return ContractAmendmentDto.from(amendmentRepository.save(amendment));
    }

    public ContractAmendmentDto signAmendment(String amendmentId, String userId, boolean isLandlord) {
        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phu luc: " + amendmentId));

        if (isLandlord) {
            amendment.signLandlord();
        } else {
            amendment.signTenant();
        }

        return ContractAmendmentDto.from(amendmentRepository.save(amendment));
    }

    @Transactional(readOnly = true)
    public List<ContractAmendmentDto> getAmendments(String contractId) {
        return amendmentRepository.findByContractId(contractId).stream().map(ContractAmendmentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ContractDto getContractById(String contractId) {
        return contractRepository.findById(contractId)
                .map(ContractDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hop dong: " + contractId));
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getTenantContracts(String tenantId) {
        return contractRepository.findByTenantId(tenantId).stream().map(ContractDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getLandlordContracts(String landlordId) {
        return contractRepository.findByLandlordId(landlordId).stream().map(ContractDto::from).toList();
    }
}

