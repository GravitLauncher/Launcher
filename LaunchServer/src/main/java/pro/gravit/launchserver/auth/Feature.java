package pro.gravit.launchserver.auth;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(Features.class)
public @interface Feature {
    String value();
}
