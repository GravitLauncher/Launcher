package pro.gravit.launcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation implies that method/field/class should not be renamed or obfuscated
 * It is used for classes and fields serializable with the help of GSON to save the field name during transmission over the network.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface LauncherNetworkAPI {
}
