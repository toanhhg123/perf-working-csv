package com.app.performanceexport;


import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tbl_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;
    private String name;
    private String avatar;
    private Integer level;
    private String sex;
    private String sign;
    private Integer vipType;
    private Integer vipStatus;
    private Integer vipRole;
    private Integer archive;
    private Integer fans;
    private Integer friend;
    private Integer likeNum;
    private Integer isSenior;
}
