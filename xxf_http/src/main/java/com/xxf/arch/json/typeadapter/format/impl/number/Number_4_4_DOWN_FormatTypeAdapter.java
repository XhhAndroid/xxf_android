package com.xxf.arch.json.typeadapter.format.impl.number;

import androidx.annotation.NonNull;

import com.xxf.arch.json.typeadapter.format.NumberObjectFormatTypeAdapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * @Description: 2-2位小数格式化
 * @Author: XGod  xuanyouwu@163.com  17611639080  https://github.com/NBXXF     https://blog.csdn.net/axuanqq
 * @CreateDate: 2020/11/25 10:23
 */
public class Number_4_4_DOWN_FormatTypeAdapter extends NumberObjectFormatTypeAdapter {
    @Override
    public String format(@NonNull BigDecimal origin) throws Exception {
        NumberFormat numberInstance = NumberFormat.getNumberInstance(Locale.CHINA);
        numberInstance.setGroupingUsed(false);
        numberInstance.setMinimumFractionDigits(4);
        numberInstance.setMaximumFractionDigits(4);
        numberInstance.setRoundingMode(RoundingMode.DOWN);
        String format = numberInstance.format(origin);
        return format;
    }
}
