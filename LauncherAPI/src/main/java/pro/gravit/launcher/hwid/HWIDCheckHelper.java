package pro.gravit.launcher.hwid;

public class HWIDCheckHelper {
    public static int checkString(String str)
    {
        int result = 0;
        //Считаем символы
        char lastChar = '\0';
        int combo = 0;
        int maxCombo = 0;
        //Считаем род символов
        int lastCharType = -1;
        int lastCharTypeCombo = 0;
        int wtfCharTypeCombo = 0;
        boolean skipLastCharType = true;
        for(char c : str.toCharArray())
        {
            if(c == lastChar || Math.abs(c - lastChar) == 1 ||
                    ( ( lastChar == '0' || lastChar == '9' ) && ( c == 'A' || c == 'a' ))) //Переход с 0 или 9 на A или a
            {
                lastChar = c;
                combo++;
            }
            else
            {
                if(maxCombo < combo)
                    maxCombo = combo;
                lastChar = c;
                combo = 1;
            }
            int charType = getCharType(c);
            if(lastCharType == charType) {
                lastCharTypeCombo++;
                //Нам подсунули серию из идущих подряд спец символов. Что за?
                if((charType == -1 || charType == 3) && lastCharTypeCombo > 2)
                {
                    wtfCharTypeCombo+=3;
                }
                //Нам подсунули серию из слишком большого числа идущих подряд чисел. Что за?
                if((charType == 0) && lastCharTypeCombo > 4)
                {
                    wtfCharTypeCombo++;
                }
            }
            else
            {
                if(skipLastCharType && ( charType == -1 || charType == 3 ))
                {
                    skipLastCharType = false;
                }
                else
                {
                    skipLastCharType = true;
                    lastCharType = charType;
                }
            }
        }
        //Считаем результат
        if(maxCombo > 3) result+= maxCombo * 3;
        if(wtfCharTypeCombo > 1) result+= wtfCharTypeCombo * 2;
        return result;
    }
    public static int getCharType(char c)
    {
        if(c >= '0' && c <= '9') return 0;
        if(c >= 'A' && c <= 'Z') return 1;
        if(c >= 'a' && c <= 'z') return 2;
        if(c == ' ' || c == '-' || c == '_') return 3;
        return -1;
    }
}
