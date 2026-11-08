package vn.cozynest.booking.application.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.cozynest.booking.application.dto.AppointmentFilterParams;
import vn.cozynest.booking.domain.model.ViewingAppointment;

import java.util.ArrayList;
import java.util.List;

public final class AppointmentSpecificationBuilder {

    private AppointmentSpecificationBuilder() {}

    public static Specification<ViewingAppointment> build(String landlordId, AppointmentFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("landlordId"), landlordId));

            if (params.status() != null) {
                predicates.add(cb.equal(root.get("status"), params.status()));
            }

            if (params.roomId() != null && !params.roomId().isBlank()) {
                predicates.add(cb.equal(root.get("roomId"), params.roomId().trim()));
            }

            if (params.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledStart"), params.dateFrom()));
            }

            if (params.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledStart"), params.dateTo()));
            }

            if (params.search() != null && !params.search().isBlank()) {
                String searchPattern = "%" + params.search().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("contactName")), searchPattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("contactPhone")), searchPattern);
                Predicate roomMatch = cb.like(cb.lower(root.get("roomTitle")), searchPattern);
                predicates.add(cb.or(nameMatch, phoneMatch, roomMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
