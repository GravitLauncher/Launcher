package ru.gravit.launcher;

public interface HWID {
    String getSerializeString();

    int getLevel(); //Уровень доверия, насколько уникальные значения

    boolean isNull();
}
