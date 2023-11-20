package dk.digitalidentity;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = UserSecurityContextFactory.class)
public @interface TestUser {
    String uuid() default "ff6fc101-aeb2-486e-8d39-5d8e718abdec";
}
