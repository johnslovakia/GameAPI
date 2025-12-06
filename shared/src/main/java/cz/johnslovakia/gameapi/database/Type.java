package cz.johnslovakia.gameapi.database;

import lombok.Getter;

@Getter
public enum Type {

    VARCHAR32("VARCHAR(32)"), VARCHAR128("VARCHAR(128)"), VARCHAR256("VARCHAR(256)"), VARCHAR512("VARCHAR(512)"), VARCHAR1024("VARCHAR(1024)"), VARCHAR2048("VARCHAR(2048)"), INT("INT"), JSON("JSON");

    final String b;

    Type(String b) {
        this.b = b;
    }
}