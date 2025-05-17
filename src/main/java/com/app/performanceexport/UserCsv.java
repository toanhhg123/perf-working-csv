package com.app.performanceexport;


import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCsv {
    @CsvBindByName(column = "uid")
    private Long uid;

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "avatar")
    private String avatar;

    @CsvBindByName(column = "level")
    private Integer level;

    @CsvBindByName(column = "sex")
    private String sex;

    @CsvBindByName(column = "sign")
    private String sign;

    @CsvBindByName(column = "vip_type")
    private Integer vipType;

    @CsvBindByName(column = "vip_status")
    private Integer vipStatus;

    @CsvBindByName(column = "vip_role")
    private Integer vipRole;

    @CsvBindByName(column = "archive")
    private Integer archive;

    @CsvBindByName(column = "fans")
    private Integer fans;

    @CsvBindByName(column = "friend")
    private Integer friend;

    @CsvBindByName(column = "like_num")
    private Integer likeNum;

    @CsvBindByName(column = "is_senior")
    private Integer isSenior;
}
