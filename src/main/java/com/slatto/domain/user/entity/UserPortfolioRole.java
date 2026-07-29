package com.slatto.domain.user.entity;

import com.slatto.domain.user.enums.RoleName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_portfolio_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolioRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private UserPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private RoleName roleName;

    private UserPortfolioRole(UserPortfolio portfolio, Users user, RoleName roleName) {
        this.portfolio = portfolio;
        this.user = user;
        this.roleName = roleName;
    }

    public static UserPortfolioRole create(UserPortfolio portfolio, Users user, RoleName roleName) {
        return new UserPortfolioRole(portfolio, user, roleName);
    }
}