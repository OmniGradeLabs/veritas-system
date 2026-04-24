package com.veritas.omnigrade.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseEntity {
  @Id
  @GeneratedValue(generator = "uuid-v7")
  @GenericGenerator(name = "uuid-v7", type = UuidV7Generator.class)
  private UUID id;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;

    /**
     * So sánh tính bằng nhau (equality) của hai thực thể dựa trên định danh cơ sở dữ liệu (Database
     * Identity). *
     *
     * <p>Trong môi trường JPA/Hibernate, các thực thể thường được lazy-load dưới dạng các lớp
     * Proxy. Lớp Proxy này là một subclass động của Entity gốc. Nếu sử dụng {@code o.getClass()}
     * hoặc {@code instanceof} thông thường, phép so sánh sẽ thất bại do khác biệt về classloader và
     * hệ thống kiểu (type system). *
     *
     * <p>Phương thức này trích xuất lớp persistent class thực sự ẩn dưới Proxy để đảm bảo một
     * Entity và Proxy của chính nó luôn được đánh giá là bằng nhau nếu có cùng ID.
     */
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();

    if (thisEffectiveClass != oEffectiveClass) return false;

    BaseEntity that = (BaseEntity) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  /**
   * Tính toán mã băm (hash code) của thực thể. *
   *
   * <p>Tuân thủ chặt chẽ nguyên tắc: {@code HashCode} của một đối tượng KHÔNG ĐƯỢC PHÉP thay đổi
   * trong suốt vòng đời của nó nếu đối tượng đó đang nằm trong các cấu trúc dữ liệu băm (như
   * HashSet, HashMap). *
   *
   * <p>Nếu tính HashCode dựa trên trường {@code id}: Khi một Entity mới được tạo (id = null) và đưa
   * vào HashSet, sau đó được {@code save()} (Hibernate gán id mới), HashCode sẽ thay đổi. Hệ quả là
   * Entity đó sẽ bị "thất lạc" trong bucket của HashSet. *
   *
   * <p>Việc trả về một hằng số (Class HashCode) ép các cấu trúc dữ liệu băm phải gộp (collide) các
   * entity vào cùng một bucket và giải quyết việc tìm kiếm thông qua phương thức {@code equals()},
   * đảm bảo an toàn tuyệt đối cho các thao tác chuyển đổi trạng thái (transient -> persistent).
   */
  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
