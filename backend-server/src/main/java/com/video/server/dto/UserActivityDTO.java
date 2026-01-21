package com.video.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityDTO implements Comparable<UserActivityDTO> {
    private String timestamp; // 显示时间
    private String content;   // 内容
    private String color;     // 颜色
    private String type;      // 类型
    private Long rawTime;     // 排序用时间戳

    @Override
    public int compareTo(UserActivityDTO o) {
        return Long.compare(o.rawTime, this.rawTime);
    }
}