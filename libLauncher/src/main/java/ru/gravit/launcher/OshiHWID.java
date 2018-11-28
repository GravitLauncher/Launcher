package ru.gravit.launcher;

import com.google.gson.Gson;

public class OshiHWID implements HWID {
    public static Gson gson = new Gson();
    public long totalMemory = 0;
    public String serialNumber;
    public String HWDiskSerial;

    @Override
    public String getSerializeString() {
        return gson.toJson(this);
    }
    public int getLevel() //Уровень доверия, насколько уникальные значения
    {
        int result = 0;
        if(totalMemory != 0) result++;
        if(serialNumber != null) result+=5;
        if(HWDiskSerial != null) result+=8;
        return result;
    }
    @Override
    public boolean isNull() {
        return getLevel() < 2;
    }
}
