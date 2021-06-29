package poc;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(PodLogExtension.class)
public @interface PodLog {
    String value() default "[unassigned]";

    String filter() default "[unassigned]";

    String[] namespaces() default "[unassigned]";
}
