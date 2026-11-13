-- ============================================================
-- V18__add_initial_meter_readings_to_rooms.sql
-- Sprint 3 / Enhancement: Initial meter readings for transparent utilities
-- ============================================================

ALTER TABLE rooms
    ADD COLUMN initial_electricity_reading DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Chỉ số công tơ điện ban đầu bàn giao',
    ADD COLUMN initial_water_reading DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Chỉ số đồng hồ nước ban đầu bàn giao';

