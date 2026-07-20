package com.slatto.domain.user.entity;

import com.slatto.domain.user.enums.CategoryName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_category",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_category_user_category_name",
                columnNames = {"user_id", "category_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private CategoryName categoryName;

    private UserCategory(Users user, CategoryName categoryName) {
        this.user = user;
        this.categoryName = categoryName;
    }

    public static UserCategory create(Users user, CategoryName categoryName) {
        return new UserCategory(user, categoryName);
    }
}