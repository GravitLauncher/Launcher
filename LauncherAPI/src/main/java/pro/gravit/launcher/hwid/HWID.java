package pro.gravit.launcher.hwid;

public interface HWID {
    String getSerializeString();

    int getLevel(); //Уровень доверия, насколько уникальные значения

    int compare(HWID hwid);

    boolean isNull();
}
