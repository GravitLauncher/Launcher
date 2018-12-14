package ru.gravit.launcher;

import com.google.gson.Gson;

public class OshiHWID implements HWID {
    public static Gson gson = new Gson();
    @LauncherAPI
    public long totalMemory = 0;
    @LauncherAPI
    public String serialNumber;
    @LauncherAPI
    public String HWDiskSerial;
    @LauncherAPI
    public String processorID;

    @Override
    public String getSerializeString() {
        return gson.toJson(this);
    }
    @Override
    public int getLevel() //Уровень доверия, насколько уникальные значения
    {
        int result = 0;
        if(totalMemory != 0) result++;
        if(serialNumber != null && !serialNumber.equals("unknown")) result+=4;
        if(HWDiskSerial != null && !HWDiskSerial.equals("unknown")) result+=15;
        if(processorID != null  && !processorID.equals("unknown")) result+=6;
        return result;
    }
    @Override
    public boolean isNull() {
        return getLevel() < 2;
    }
}
