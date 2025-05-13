package com.app.performanceexport;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMapper {

    public static User convertToEntity(UserCsv csv) {
        return User.builder()
                .name(csv.getName())
                .avatar(csv.getAvatar())
                .level(csv.getLevel())
                .sex(csv.getSex())
                .sign(csv.getSign())
                .vipType(csv.getVipType())
                .vipStatus(csv.getVipStatus())
                .vipRole(csv.getVipRole())
                .archive(csv.getArchive())
                .fans(csv.getFans())
                .friend(csv.getFriend())
                .likeNum(csv.getLikeNum())
                .isSenior(csv.getIsSenior())
                .build();
    }
}
