package cz.johnslovakia.gameapi.modules;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleImplementation {
    Class<? extends Module> value();
}