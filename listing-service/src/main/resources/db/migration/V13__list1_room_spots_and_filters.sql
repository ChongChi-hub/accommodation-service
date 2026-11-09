-- Migration V13: Add available_spots and total_spots to rooms table for shared room filters
ALTER TABLE rooms ADD COLUMN available_spots INT NULL AFTER max_occupants;
ALTER TABLE rooms ADD COLUMN total_spots INT NULL AFTER available_spots;

