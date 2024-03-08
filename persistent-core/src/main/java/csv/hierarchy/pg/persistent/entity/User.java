package csv.hierarchy.pg.persistent.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "user_auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "hierarchy_id", nullable = false)
    private Long hierarchyId;
    @Column(name = "definition_id", nullable = false)
    private Long definitionId;
    @Column(name = "scope", columnDefinition = "SMALLINT")
    private Short scope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cascade", columnDefinition = "jsonb")
    private JsonNode cascade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explicit", columnDefinition = "jsonb")
    private JsonNode explicit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entire", columnDefinition = "jsonb")
    private JsonNode entire;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_ts")
    private Timestamp updateTs;
}
