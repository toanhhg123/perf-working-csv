package com.app.performanceexport;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
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
