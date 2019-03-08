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
        if (totalMemory != 0) result++;
        if (serialNumber != null && !serialNumber.equals("unknown")) result += 4;
        if (HWDiskSerial != null && !HWDiskSerial.equals("unknown")) result += 15;
        if (processorID != null && !processorID.equals("unknown")) result += 6;
        return result;
    }

    @Override
    public int compare(HWID hwid) {
        if(hwid instanceof OshiHWID)
        {
            int rate = 0;
            OshiHWID oshi = (OshiHWID) hwid;
            if(Math.abs(oshi.totalMemory - totalMemory) < 1024*1024) rate+=10;
            if(oshi.HWDiskSerial.equals(HWDiskSerial)) rate+=50;
            if(oshi.processorID.equals(processorID)) rate+=26;
            if(oshi.serialNumber.equals(serialNumber)) rate+=15;
            return rate;
        }
        return 0;
    }

    @Override
    public boolean isNull() {
        return getLevel() < 2;
    }
}
