package com.zs.ytbx.dto;

import lombok.Data;

@Data
public class ProductQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String category;
    private String company;
    private String keyword;
}
