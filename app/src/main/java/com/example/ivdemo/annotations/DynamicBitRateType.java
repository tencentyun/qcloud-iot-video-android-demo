package com.example.ivdemo.annotations;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@IntDef({DynamicBitRateType.DEFAULT_TYPE, DynamicBitRateType.WATER_LEVEL_TYPE, DynamicBitRateType.INTERNET_SPEED_TYPE})
/*
 * 动态调整码率类型
 */
public @interface DynamicBitRateType {
    /*Defines the type of avt */
    int DEFAULT_TYPE = 0; /*不开启*/
    int  WATER_LEVEL_TYPE= 1; /*水位值*/
    int INTERNET_SPEED_TYPE = 2; /*1秒内的平均网速*/
}
