package com.bid.auction.entity;

import com.bid.auction.enums.Role;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;

    private String name;

    private String email;

    private Role role;

    @Builder.Default
    private boolean emailVerified = false;
}
