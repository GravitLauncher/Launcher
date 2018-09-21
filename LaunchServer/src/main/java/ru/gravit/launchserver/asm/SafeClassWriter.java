package ru.gravit.launchserver.asm;

import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;

/**
 * ClassWriter с другой реализацией метода getCommonSuperClass: при его использовании не происходит загрузки классов.
 * Однако, сама по себе загрузка классов редко является проблемой, потому что инициализация класса (вызов статических
 * блоков) происходит не при загрузке класса. Проблемы появляются, когда изменения вставляются в зависимые друг от друга
 * классы, тогда стандартная реализация отваливается с ClassCircularityError.
 */
public class SafeClassWriter extends ClassWriter {
    private final ClassMetadataReader classMetadataReader;

    public SafeClassWriter(ClassMetadataReader classMetadataReader, int flags) {
        super(flags);
        this.classMetadataReader = classMetadataReader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        ArrayList<String> superClasses1 = classMetadataReader.getSuperClasses(type1);
        ArrayList<String> superClasses2 = classMetadataReader.getSuperClasses(type2);
        int size = Math.min(superClasses1.size(), superClasses2.size());
        int i;
        for (i = 0; i < size && superClasses1.get(i).equals(superClasses2.get(i)); i++);
        if (i == 0) {
            return "java/lang/Object";
        } else {
            return superClasses1.get(i-1);
        }
    }
}