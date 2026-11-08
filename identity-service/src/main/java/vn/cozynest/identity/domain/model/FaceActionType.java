package vn.cozynest.identity.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum FaceActionType {
    CHOP_MAT("Chớp mắt 2 lần"),
    QUAY_TRAI("Quay đầu sang trái"),
    QUAY_PHAI("Quay đầu sang phải"),
    MO_MIENG("Mở và khép miệng nhẹ");

    private final String description;

    FaceActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static List<FaceActionType> pickRandomTwo() {
        List<FaceActionType> all = new java.util.ArrayList<>(List.of(values()));
        Collections.shuffle(all, new Random());
        return List.of(all.get(0), all.get(1));
    }
}

