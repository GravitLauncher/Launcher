package pro.gravit.launcher.hwid;

@Deprecated
public interface HWID {

    int getLevel(); //Уровень доверия, насколько уникальные значения

    int getAntiLevel(); //Уровень лживости, насколько фальшивые значения

    int compare(HWID hwid);

    boolean isNull();

    void normalize();
}
