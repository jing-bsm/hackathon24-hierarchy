package csv.hierarchy.pg.writer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuth {
    short scope;
    Set<String> cascade;
    Set<String> explicit;
}
