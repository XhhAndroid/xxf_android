package com.xxf.arch.json.typeadapter.format.impl.time;

import androidx.annotation.NonNull;

import com.xxf.arch.json.typeadapter.format.TimeObjectFormatTypeAdapter;

/**
 * @Description: 时间格式化HH:mm:ss 时分秒
 * 分割符 s:代表斜杠 hl:代表中横线  c:代表冒号
 * @Author: XGod
 * @CreateDate: 2020/11/25 9:47
 */
public class Time_HH_c_mm_c_ss_FormatTypeAdapter extends TimeObjectFormatTypeAdapter {

    @Override
    public String format(@NonNull Long origin) throws Exception {
        return android.text.format.DateFormat.format("HH:mm:ss", origin).toString();
    }
}