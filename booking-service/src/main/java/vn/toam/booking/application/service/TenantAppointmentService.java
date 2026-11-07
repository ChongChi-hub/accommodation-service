package vn.toam.booking.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.booking.application.dto.ViewingAppointmentListItemDto;
import vn.toam.booking.domain.repository.ViewingAppointmentRepository;

@Service
@Transactional(readOnly = true)
public class TenantAppointmentService {

    private final ViewingAppointmentRepository appointmentRepository;

    public TenantAppointmentService(ViewingAppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Page<ViewingAppointmentListItemDto> getTenantAppointments(String tenantId, int page, int size) {
        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "scheduledStart"));

        return appointmentRepository.findByTenantId(tenantId, pageable)
                .map(ViewingAppointmentListItemDto::fromEntity);
    }
}
