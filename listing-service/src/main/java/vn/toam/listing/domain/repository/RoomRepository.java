package vn.toam.listing.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.toam.listing.domain.model.Room;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    List<Room> findByStatusAndApprovedTrue(String status, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.approved = true AND r.provinceCode = :provinceCode AND r.id != :roomId ORDER BY r.ratingAvg DESC")
    List<Room> findRecommendations(@Param("provinceCode") String provinceCode, @Param("roomId") Long roomId, Pageable pageable);

    Page<Room> findByLandlordId(Long landlordId, Pageable pageable);
}

