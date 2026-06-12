package dev.vivim.filecloud.dto.annotation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message="Username must not be blank!")
@Size(min = 3, max = 20, message = "Username must be from 3 to 20 characters!")
public @interface ValidUsername {}