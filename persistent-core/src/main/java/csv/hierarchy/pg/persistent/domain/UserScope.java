package csv.hierarchy.pg.persistent.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum UserScope {
    ALL((short) 1),
    NONE((short) 0),
    SPECIFIC((short) 2);

    private final short num;

    public static UserScope fromShort(short num) {
        for (UserScope userScope : UserScope.values()) {
            if (userScope.getNum() == num) {
                return userScope;
            }
        }
        return null;
    }
}
